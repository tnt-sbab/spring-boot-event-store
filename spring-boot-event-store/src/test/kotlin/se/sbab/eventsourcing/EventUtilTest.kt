package se.sbab.eventsourcing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import se.sbab.eventsourcing.test.avro.EventUtilTestEvent
import se.sbab.eventsourcing.test.avro.NestedRecord
import se.sbab.eventsourcing.test.avro.Status
import se.sbab.eventsourcing.test.avro.Token
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.nio.ByteBuffer

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
}
