package se.sbab.eventsourcing.kafka

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class OccurredAtTest {
    @Test
    fun `ByteArray constructor and getBytes should produce identical OccurredAt objects`() {
        val occurredAt = OccurredAt(System.currentTimeMillis())
        assertEquals(occurredAt, OccurredAt(occurredAt.getBytes()))
    }

    @Test
    fun `getBytes for Int 1 should result in ByteArray 0 0 0 0 0 0 0 1`() {
        assertTrue(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1).contentEquals(OccurredAt(1L).getBytes()))
    }

    @Test
    fun `Verify toString format`() {
        assertEquals("1234", OccurredAt(1234L).toString())
    }
}
