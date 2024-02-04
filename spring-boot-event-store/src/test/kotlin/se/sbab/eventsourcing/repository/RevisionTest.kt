package se.sbab.eventsourcing.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.random.Random

internal class RevisionTest {
    @Test
    fun `ByteArray constructor and getBytes should produce identical Revision objects`() {
        val revision = Revision(Random.nextInt())
        assertEquals(revision, Revision(revision.getBytes()))
    }

    @Test
    fun `getBytes for Int 1 should result in ByteArray 0 0 0 1`() {
        assertTrue(byteArrayOf(0, 0, 0, 1).contentEquals(Revision(1).getBytes()))
    }

    @Test
    fun `Verify toString format`() {
        assertEquals("1234", Revision(1234).toString())
    }

    @Test
    fun `Verify revision number defaults to zero`() {
        assertEquals(0, Revision().value)
    }
}
