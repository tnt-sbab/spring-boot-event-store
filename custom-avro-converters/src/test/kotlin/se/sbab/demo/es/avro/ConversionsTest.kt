package se.sbab.demo.es.avro

import org.apache.avro.LogicalTypes
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import se.sbab.demo.es.AccountId
import se.sbab.demo.es.avro.Conversions.AccountIdConversion
import se.sbab.demo.es.avro.DemoLogicalTypes.Companion.ACCOUNT_ID
import java.util.UUID

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ConversionsTest {
    @BeforeAll
    fun init() {
        DemoLogicalTypes.registerTypeFactories()
    }

    @Test
    fun `AccountId should be converted to char sequence identical to original`() {
        val accountId = AccountId()
        val value = AccountIdConversion().toCharSequence(accountId, null, null)
        assertEquals(accountId.toString(), value)
    }

    @Test
    fun `AccountId can be converted to and from String`() {
        val accountId = AccountId()
        val charSequence = AccountIdConversion().toCharSequence(accountId, null, null)
        assertEquals(accountId, AccountIdConversion().fromCharSequence(charSequence, null, null))
    }

    @Test
    fun `wire format for AccountId should be String identical to a uuid string`() {
        val uuid = UUID.randomUUID()
        val accountId = AccountId(uuid)
        val charSequence = AccountIdConversion().toCharSequence(accountId, null, null)
        assertEquals(uuid.toString(), charSequence)
    }

    @Test
    fun `Schema name for AccountId logical type should be registered`() {
        val logicalType = LogicalTypes.fromSchema(AccountIdConversion().recommendedSchema)
        assertEquals(ACCOUNT_ID, logicalType.name)
    }
}