package se.sbab.demo.es

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.util.UUID

internal class AccountIdTest {
    @Test
    fun `a valid uuid string should construct a valid AccountId object`() {
        val uuid = UUID.randomUUID()
        assertEquals(uuid, AccountId(uuid.toString()).id)
    }

    @Test
    fun `AccountId toString should produce an identical string`() {
        val uuid = UUID.randomUUID().toString()
        assertEquals(uuid, AccountId(uuid).toString())
    }

    @Test
    fun `two references containing the same value should be equal`() {
        val uuid = UUID.randomUUID()
        val accountId1 = AccountId(uuid)
        val accountId2 = AccountId(uuid)
        assertEquals(accountId1, accountId2)
    }

    @Test
    fun `upper case uuid string should create a correct AccountId`() {
        val uuid = UUID.randomUUID()
        val accountId = AccountId(uuid.toString().uppercase())
        assertEquals(uuid.toString(), accountId.toString())
    }

    @Test
    fun `invalid format should throw an IllegalArgumentException`() {
        val uuidString = "32748477-5074-4139-97ce-d9af142b7771x"
        val exception = assertThrows(IllegalArgumentException::class.java) {
            AccountId(uuidString)
        }
        assertEquals("UUID string too large", exception.message)
    }

    @Test
    fun `AccountId can be serialized identical to raw string without wrapper class`() {
        val uuid = UUID.randomUUID()
        val accountId = AccountId(uuid)
        val result = jacksonObjectMapper().writeValueAsString(accountId)
        val expected = jacksonObjectMapper().writeValueAsString(uuid.toString())
        assertEquals(expected, result)
    }

    @Test
    fun `AccountId can be serialized and deserialized to identical objects`() {
        val accountId = AccountId()
        val serialized = jacksonObjectMapper().writeValueAsString(accountId)
        val deserialized = jacksonObjectMapper().readValue(serialized, AccountId::class.java)
        assertEquals(accountId, deserialized)
    }
}