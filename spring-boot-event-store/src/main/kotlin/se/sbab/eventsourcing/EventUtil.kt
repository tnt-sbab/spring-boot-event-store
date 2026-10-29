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

import org.apache.avro.Conversions.DecimalConversion
import org.apache.avro.Schema
import org.apache.avro.generic.GenericFixed
import org.apache.avro.specific.SpecificData
import org.apache.avro.specific.SpecificRecord
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

private val JSON_MAPPER = jacksonObjectMapper()
private val DECIMAL_CONVERSION = DecimalConversion()

fun Event.toJson(): String {
    return JSON_MAPPER.writeValueAsString(renderJsonNode(schema, this))
}

private fun renderJsonNode(schema: Schema, value: Any?): JsonNode = when {
    value == null -> NullNode.instance
    schema.logicalType?.name == "decimal" -> JSON_MAPPER.nodeFactory.numberNode(value.toBigDecimal(schema))
    schema.type == Schema.Type.RECORD -> renderRecordNode(schema, value as SpecificRecord)
    schema.type == Schema.Type.UNION -> renderUnionNode(schema, value)
    schema.type == Schema.Type.ARRAY -> renderArrayNode(schema, value as Collection<*>)
    schema.type == Schema.Type.MAP -> renderMapNode(schema, value as Map<*, *>)
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

private fun renderRecordNode(schema: Schema, record: SpecificRecord): ObjectNode =
    JSON_MAPPER.nodeFactory.objectNode().apply {
        schema.fields.forEach { field ->
            set(field.name(), renderJsonNode(field.schema(), record.get(field.pos())))
        }
    }

private fun renderUnionNode(schema: Schema, value: Any): JsonNode {
    val branchSchema = resolveUnionBranch(schema, value)
    if (branchSchema.type == Schema.Type.NULL) {
        return NullNode.instance
    }

    return JSON_MAPPER.nodeFactory.objectNode().apply {
        set(jsonTypeName(branchSchema), renderJsonNode(branchSchema, value))
    }
}

private fun renderArrayNode(schema: Schema, values: Collection<*>): ArrayNode =
    JSON_MAPPER.nodeFactory.arrayNode().apply {
        values.forEach { add(renderJsonNode(schema.elementType, it)) }
    }

private fun renderMapNode(schema: Schema, values: Map<*, *>): ObjectNode =
    JSON_MAPPER.nodeFactory.objectNode().apply {
        values.forEach { (key, item) -> set(key.toString(), renderJsonNode(schema.valueType, item)) }
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
