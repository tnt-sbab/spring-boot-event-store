package se.sbab.eventsourcing

import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.specific.SpecificRecordBase
import org.apache.avro.util.TimePeriod
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import se.sbab.eventsourcing.test.avro.EventUtilTestEvent
import se.sbab.eventsourcing.test.avro.LogicalTypesTestEvent
import se.sbab.eventsourcing.test.avro.NestedRecord
import se.sbab.eventsourcing.test.avro.Status
import se.sbab.eventsourcing.test.avro.TestDuration
import se.sbab.eventsourcing.test.avro.Token
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.temporal.UnsupportedTemporalTypeException
import java.util.UUID

internal class EventUtilTest {
    private val jsonMapper = jacksonObjectMapper()

    @Test
    fun `toJson should render all supported Avro types`() {
        val event = EventUtilTestEvent(
            BigDecimal("42.50"),
            NestedRecord("nested", 7),
            listOf(NestedRecord("first", 1), NestedRecord("second", 2)),
            mutableMapOf<CharSequence, CharSequence>("alpha" to "bravo"),
            ByteBuffer.wrap(byteArrayOf(1, 2)),
            Token(byteArrayOf(3, 4)),
            "hello",
            Status.ACTIVE,
            true,
            123,
            9876543210L,
            1.25f,
            99.5,
            null,
        )

        val root = jsonMapper.readTree(event.toJson())

        assertTrue(root["amount"].has("bytes"))
        assertEquals(0, root["amount"]["bytes"].decimalValue().compareTo(BigDecimal("42.50")))
        assertEquals("nested", root["nestedRecord"]["label"].asString())
        assertEquals(7, root["nestedRecord"]["quantity"].asInt())
        assertEquals("first", root["nestedRecords"][0]["label"].asString())
        assertEquals("second", root["nestedRecords"][1]["label"].asString())
        assertEquals("bravo", root["attributes"]["alpha"].asString())
        assertEquals("AQI=", root["binaryValue"].asString())
        assertEquals("AwQ=", root["fixedValue"].asString())
        assertEquals("hello", root["text"].asString())
        assertEquals("ACTIVE", root["status"].asString())
        assertTrue(root["active"].asBoolean())
        assertEquals(123, root["count"].asInt())
        assertEquals(9876543210L, root["total"].asLong())
        assertEquals(0, root["ratio"].decimalValue().compareTo(BigDecimal("1.25")))
        assertEquals(0, root["precise"].decimalValue().compareTo(BigDecimal("99.5")))
        assertTrue(root["optionalText"].isNull)
        assertFalse(root["optionalText"].isObject)
    }

    @Test
    fun `toJson should render generated logical types as ISO strings and duration components`() {
        val event = logicalTypesEvent()

        assertEquals(
            jsonMapper.readTree(
                """
                {
                  "id": "123e4567-e89b-12d3-a456-426614174000",
                  "date": "2024-02-29",
                  "timeMillis": "12:34:56.123",
                  "timeMicros": "12:34:56.123456",
                  "timestampMillis": "2024-02-29T12:34:56.123Z",
                  "timestampMicros": "2024-02-29T12:34:56.123456Z",
                  "localTimestampMillis": "2024-02-29T12:34:56.123",
                  "localTimestampMicros": "2024-02-29T12:34:56.123456",
                  "duration": {"months": 1, "days": 2, "milliseconds": 3}
                }
                """.trimIndent(),
            ),
            jsonMapper.readTree(event.toJson()),
        )
    }

    @ParameterizedTest
    @MethodSource("rawLogicalValues")
    fun `toJson should convert underlying Avro logical values`(fieldName: String, value: Any, expected: String) {
        val fieldSchema = LogicalTypesTestEvent.getClassSchema().getField(fieldName).schema()
        val event = record(fieldSchema, value)

        assertEquals(jsonMapper.readTree(expected), jsonMapper.readTree(event.toJson())["value"])
    }

    @Test
    fun `toJson should apply custom formats throughout records unions arrays and maps`() {
        val event = logicalTypesEvent()
        val format = EventJsonFormat(
            date = DateTimeFormatter.ofPattern("dd/MM/uuuu"),
            time = DateTimeFormatter.ofPattern("HH:mm:ss.SSSSSS"),
            timestamp = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss.SSSSSS XXX"),
            localTimestamp = DateTimeFormatter.ofPattern("uuuu/MM/dd HH:mm:ss.SSSSSS"),
        )
        val root = jsonMapper.readTree(event.toJson(format))

        assertEquals("29/02/2024", root["date"].asString())
        assertEquals("12:34:56.123000", root["timeMillis"].asString())
        assertEquals("12:34:56.123456", root["timeMicros"].asString())
        assertEquals("2024/02/29 12:34:56.123000 Z", root["timestampMillis"].asString())
        assertEquals("2024/02/29 12:34:56.123456 Z", root["timestampMicros"].asString())
        assertEquals("2024/02/29 12:34:56.123000", root["localTimestampMillis"].asString())
        assertEquals("2024/02/29 12:34:56.123456", root["localTimestampMicros"].asString())

        val dateSchema = event.schema.getField("date").schema()
        val union = Schema.createUnion(Schema.create(Schema.Type.NULL), dateSchema)
        assertEquals(
            """{"value":{"int":"29/02/2024"}}""",
            record(union, event.date).toJson(format),
        )
        assertEquals("""{"value":null}""", record(union, null).toJson(format))
        assertEquals(
            """{"value":[{"int":"29/02/2024"},null]}""",
            record(Schema.createArray(union), listOf(event.date, null)).toJson(format),
        )
        assertEquals(
            """{"value":{"today":"29/02/2024"}}""",
            record(Schema.createMap(dateSchema), mapOf("today" to event.date)).toJson(format),
        )
        assertEquals(
            root,
            jsonMapper.readTree(record(event.schema, event).toJson(format))["value"],
        )
        assertEquals("2024-02-29", jsonMapper.readTree(event.toJson())["date"].asString())
    }

    @Test
    fun `toJson should resolve unions for all converted logical types`() {
        val event = logicalTypesEvent()
        val expected = jsonMapper.readTree(event.toJson())
        event.schema.fields.forEach { field ->
            val schema = field.schema()
            val union = Schema.createUnion(Schema.create(Schema.Type.NULL), schema)
            val key = if (schema.type == Schema.Type.FIXED) schema.fullName else schema.type.name.lowercase()
            assertEquals(
                expected[field.name()],
                jsonMapper.readTree(record(union, event.get(field.pos())).toJson())["value"][key],
                field.name(),
            )
        }
    }

    @Test
    fun `toJson should propagate incompatible formatter errors`() {
        assertThrows(UnsupportedTemporalTypeException::class.java) {
            logicalTypesEvent().toJson(EventJsonFormat(date = DateTimeFormatter.ISO_INSTANT))
        }
    }

    private fun logicalTypesEvent(): LogicalTypesTestEvent = LogicalTypesTestEvent(
        UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
        LocalDate.parse("2024-02-29"),
        LocalTime.parse("12:34:56.123"),
        LocalTime.parse("12:34:56.123456"),
        Instant.parse("2024-02-29T12:34:56.123Z"),
        Instant.parse("2024-02-29T12:34:56.123456Z"),
        LocalDateTime.parse("2024-02-29T12:34:56.123"),
        LocalDateTime.parse("2024-02-29T12:34:56.123456"),
        TestDuration(ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN).putInt(1).putInt(2).putInt(3).array()),
    )

    private fun record(valueSchema: Schema, value: Any?): Event {
        val recordSchema = Schema.createRecord("TestRecord", null, "se.sbab.eventsourcing.test", false)
        recordSchema.fields = listOf(Schema.Field("value", valueSchema))
        return object : SpecificRecordBase() {
            override fun getSchema(): Schema = recordSchema
            override fun get(field: Int): Any? = value
            override fun put(field: Int, value: Any?) = throw UnsupportedOperationException()
        }
    }

    companion object {
        @JvmStatic
        fun rawLogicalValues(): List<Array<Any>> {
            val durationSchema = LogicalTypesTestEvent.getClassSchema().getField("duration").schema()
            val durationBytes = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN)
                .putInt(-1).putInt(Int.MIN_VALUE).putInt(3).array()
            return listOf(
                arrayOf("id", "123e4567-e89b-12d3-a456-426614174000", "\"123e4567-e89b-12d3-a456-426614174000\""),
                arrayOf("date", 0, "\"1970-01-01\""),
                arrayOf("date", -1, "\"1969-12-31\""),
                arrayOf("timeMillis", 0, "\"00:00:00\""),
                arrayOf("timeMillis", 86399999, "\"23:59:59.999\""),
                arrayOf("timeMicros", 86399999999L, "\"23:59:59.999999\""),
                arrayOf("timestampMillis", 0L, "\"1970-01-01T00:00:00Z\""),
                arrayOf("timestampMillis", -1L, "\"1969-12-31T23:59:59.999Z\""),
                arrayOf("timestampMicros", -1L, "\"1969-12-31T23:59:59.999999Z\""),
                arrayOf("localTimestampMillis", 0L, "\"1970-01-01T00:00:00\""),
                arrayOf("localTimestampMillis", -1L, "\"1969-12-31T23:59:59.999\""),
                arrayOf("localTimestampMicros", -1L, "\"1969-12-31T23:59:59.999999\""),
                arrayOf("duration", GenericData.Fixed(durationSchema, durationBytes), """{"months":4294967295,"days":2147483648,"milliseconds":3}"""),
                arrayOf("duration", durationBytes, """{"months":4294967295,"days":2147483648,"milliseconds":3}"""),
                arrayOf("duration", TimePeriod.of(1, 2, 3), """{"months":1,"days":2,"milliseconds":3}"""),
            )
        }
    }
}
