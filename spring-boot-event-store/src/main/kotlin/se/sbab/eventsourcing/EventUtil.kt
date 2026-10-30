/**
 * Copyright 2024 SBAB Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.sbab.eventsourcing

import org.apache.avro.Conversions
import org.apache.avro.Conversions.DecimalConversion
import org.apache.avro.Schema
import org.apache.avro.data.TimeConversions
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericFixed
import org.apache.avro.specific.SpecificData
import org.apache.avro.specific.SpecificRecord
import org.apache.avro.util.TimePeriod
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.BinaryNode
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.NullNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAccessor

private val JSON_MAPPER = jacksonObjectMapper()
private val DECIMAL_CONVERSION = DecimalConversion()
private val LOGICAL_CONVERSIONS = listOf(
    Conversions.UUIDConversion(),
    TimeConversions.DateConversion(),
    TimeConversions.TimeMillisConversion(),
    TimeConversions.TimeMicrosConversion(),
    TimeConversions.TimestampMillisConversion(),
    TimeConversions.TimestampMicrosConversion(),
    TimeConversions.LocalTimestampMillisConversion(),
    TimeConversions.LocalTimestampMicrosConversion(),
    Conversions.DurationConversion(),
).associateBy { it.logicalTypeName }

/**
 * Formats for the JSON event view, not Avro's wire encoding. Timestamps are rendered in UTC;
 * local timestamps have no timezone. Formatters must support the corresponding java.time type.
 */
data class EventJsonFormat(
    val date: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE,
    val time: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_TIME,
    val timestamp: DateTimeFormatter = DateTimeFormatter.ISO_INSTANT,
    val localTimestamp: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME,
)

fun Event.toJson(): String = toJson(EventJsonFormat())

fun Event.toJson(format: EventJsonFormat): String {
    return JSON_MAPPER.writeValueAsString(renderJsonNode(schema, this, format))
}

private fun renderJsonNode(schema: Schema, value: Any?, format: EventJsonFormat): JsonNode = when {
    value == null -> NullNode.instance
    schema.logicalType?.name == "decimal" -> JSON_MAPPER.nodeFactory.numberNode(value.toBigDecimal(schema))
    schema.logicalType?.name in LOGICAL_CONVERSIONS -> renderLogicalNode(schema, value, format)
    schema.type == Schema.Type.RECORD -> renderRecordNode(schema, value as SpecificRecord, format)
    schema.type == Schema.Type.UNION -> renderUnionNode(schema, value, format)
    schema.type == Schema.Type.ARRAY -> renderArrayNode(schema, value as Collection<*>, format)
    schema.type == Schema.Type.MAP -> renderMapNode(schema, value as Map<*, *>, format)
    schema.type == Schema.Type.BYTES -> renderBinaryNode(value)
    schema.type == Schema.Type.FIXED -> renderFixedNode(value)
    schema.type == Schema.Type.STRING -> StringNode.valueOf(value.toString())
    schema.type == Schema.Type.ENUM -> StringNode.valueOf(value.toString())
    schema.type == Schema.Type.BOOLEAN -> BooleanNode.valueOf(value as Boolean)
    schema.type == Schema.Type.INT -> JSON_MAPPER.nodeFactory.numberNode((value as Number).toInt())
    schema.type == Schema.Type.LONG -> JSON_MAPPER.nodeFactory.numberNode((value as Number).toLong())
    schema.type == Schema.Type.FLOAT -> JSON_MAPPER.nodeFactory.numberNode((value as Number).toFloat())
    schema.type == Schema.Type.DOUBLE -> JSON_MAPPER.nodeFactory.numberNode((value as Number).toDouble())
    else -> StringNode.valueOf(value.toString())
}

private fun renderLogicalNode(schema: Schema, value: Any, format: EventJsonFormat): JsonNode {
    val conversion = LOGICAL_CONVERSIONS.getValue(schema.logicalType.name)
    val logicalValue = if (conversion.convertedType.isInstance(value)) {
        value
    } else {
        val rawValue = if (schema.type == Schema.Type.FIXED && value is ByteArray) GenericData.Fixed(schema, value) else value
        Conversions.convertToLogicalType(rawValue, schema, schema.logicalType, conversion)
    }

    return when (logicalValue) {
        is TimePeriod -> JSON_MAPPER.nodeFactory.objectNode().apply {
            put("months", logicalValue.months)
            put("days", logicalValue.days)
            put("milliseconds", logicalValue.millis)
        }
        is TemporalAccessor -> {
            val formatter = when (schema.logicalType.name) {
                "date" -> format.date
                "time-millis", "time-micros" -> format.time
                "timestamp-millis", "timestamp-micros" -> format.timestamp.withZone(ZoneOffset.UTC)
                else -> format.localTimestamp
            }
            StringNode.valueOf(formatter.format(logicalValue))
        }
        else -> StringNode.valueOf(logicalValue.toString())
    }
}

private fun renderRecordNode(schema: Schema, record: SpecificRecord, format: EventJsonFormat): ObjectNode =
    JSON_MAPPER.nodeFactory.objectNode().apply {
        schema.fields.forEach { field ->
            set(field.name(), renderJsonNode(field.schema(), record.get(field.pos()), format))
        }
    }

private fun renderUnionNode(schema: Schema, value: Any, format: EventJsonFormat): JsonNode {
    val branchSchema = resolveUnionBranch(schema, value)
    if (branchSchema.type == Schema.Type.NULL) {
        return NullNode.instance
    }

    return JSON_MAPPER.nodeFactory.objectNode().apply {
        set(jsonTypeName(branchSchema), renderJsonNode(branchSchema, value, format))
    }
}

private fun renderArrayNode(schema: Schema, values: Collection<*>, format: EventJsonFormat): ArrayNode =
    JSON_MAPPER.nodeFactory.arrayNode().apply {
        values.forEach { add(renderJsonNode(schema.elementType, it, format)) }
    }

private fun renderMapNode(schema: Schema, values: Map<*, *>, format: EventJsonFormat): ObjectNode =
    JSON_MAPPER.nodeFactory.objectNode().apply {
        values.forEach { (key, item) -> set(key.toString(), renderJsonNode(schema.valueType, item, format)) }
    }

private fun renderBinaryNode(value: Any): JsonNode = when (value) {
    is ByteBuffer -> BinaryNode.valueOf(value.duplicate().toByteArray())
    is ByteArray -> BinaryNode.valueOf(value)
    else -> StringNode.valueOf(value.toString())
}

private fun renderFixedNode(value: Any): JsonNode = when (value) {
    is GenericFixed -> BinaryNode.valueOf(value.bytes())
    is ByteArray -> BinaryNode.valueOf(value)
    else -> StringNode.valueOf(value.toString())
}

private fun resolveUnionBranch(schema: Schema, value: Any?): Schema {
    if (value == null) {
        return schema.types.first { it.type == Schema.Type.NULL }
    }

    schema.types.firstOrNull { branch ->
        LOGICAL_CONVERSIONS[branch.logicalType?.name]?.convertedType?.isInstance(value) == true
    }?.let { return it }

    schema.types.firstOrNull { branch -> branch.matches(value) }
        ?.let { return it }

    return schema.types[SpecificData.get().resolveUnion(schema, value)]
}

private fun Schema.matches(value: Any): Boolean = when (type) {
    Schema.Type.NULL -> false
    Schema.Type.BOOLEAN -> value is Boolean
    Schema.Type.INT -> value is Int
    Schema.Type.LONG -> value is Long
    Schema.Type.FLOAT -> value is Float
    Schema.Type.DOUBLE -> value is Double
    Schema.Type.STRING -> value is CharSequence
    Schema.Type.ENUM -> true
    Schema.Type.BYTES -> logicalType?.name == "decimal" && value is BigDecimal || value is ByteBuffer || value is ByteArray
    Schema.Type.FIXED -> value is GenericFixed || value is ByteArray
    Schema.Type.RECORD -> value is SpecificRecord && value.schema.fullName == fullName
    Schema.Type.ARRAY -> value is Collection<*>
    Schema.Type.MAP -> value is Map<*, *>
    Schema.Type.UNION -> false
}

private fun jsonTypeName(schema: Schema): String = when (schema.type) {
    Schema.Type.RECORD, Schema.Type.ENUM, Schema.Type.FIXED -> schema.fullName
    else -> schema.type.name.lowercase()
}

private fun Any.toBigDecimal(schema: Schema): BigDecimal = when (this) {
    is BigDecimal -> this
    is ByteBuffer -> DECIMAL_CONVERSION.fromBytes(duplicate(), schema, schema.logicalType as org.apache.avro.LogicalTypes.Decimal)
    is ByteArray -> DECIMAL_CONVERSION.fromBytes(ByteBuffer.wrap(this), schema, schema.logicalType as org.apache.avro.LogicalTypes.Decimal)
    else -> BigDecimal(this.toString())
}

private fun ByteBuffer.toByteArray(): ByteArray {
    val duplicate = duplicate()
    val bytes = ByteArray(duplicate.remaining())
    duplicate.get(bytes)
    return bytes
}
