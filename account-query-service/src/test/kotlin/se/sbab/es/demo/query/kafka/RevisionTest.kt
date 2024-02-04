package se.sbab.es.demo.query.kafka

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class RevisionTest {
    @Test
    fun `Revision 1 can be constructed using a four bytes ByteArray`() {
        val revision = Revision(byteArrayOf(0, 0, 0, 1))
        assertEquals(1, revision.value)
    }

    @Test
    fun `Revision 255 can be constructed using a four bytes ByteArray`() {
        val revision = Revision(byteArrayOf(0, 0, 0, 255.toByte()))
        assertEquals(255, revision.value)
    }

    @Test
    fun `Revision 256 can be constructed using a four bytes ByteArray`() {
        val revision = Revision(byteArrayOf(0, 0, 1, 0))
        assertEquals(256, revision.value)
    }


    @Test
    fun `Revision for max int can be constructed using a four bytes ByteArray`() {
        val revision = Revision(byteArrayOf(127, 255.toByte(), 255.toByte(), 255.toByte()))
        assertEquals(Integer.MAX_VALUE, revision.value)
    }

    @Test
    fun `Revision can be serialized identical to a raw int without wrapper class`() {
        val revision = Revision(1)
        val result = jacksonObjectMapper().writeValueAsString(revision)
        val expected = jacksonObjectMapper().writeValueAsString(1)
        assertEquals(expected, result)
    }

    @Test
    fun `Revision can be deserialized identical to a raw int without a wrapper class`() {
        val result = jacksonObjectMapper().readValue("1", Revision::class.java)
        assertEquals(Revision(1), result)
    }

    @Test
    fun `Revision can be deserialized from a string type`() {
        val result = jacksonObjectMapper().readValue("\"1\"", Revision::class.java)
        assertEquals(Revision(1), result)
    }

    @Test
    fun `Revision can be serialized and deserialized to identical objects`() {
        val revision = Revision(12311)
        val serialized = jacksonObjectMapper().writeValueAsString(revision)
        val deserialized = jacksonObjectMapper().readValue(serialized, Revision::class.java)
        assertEquals(revision, deserialized)
    }
}