package se.sbab.es.demo.app.one.commandhandler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.app.one.aggregate.Account
import se.sbab.es.demo.app.one.command.DepositMoneyCommand

internal class DepositMoneyCommandHandlerTest {
    @Test
    fun `deposit money to an existing account should generate a MoneyDepositedEvent with correct account id and amount`() {
        val accountId = AccountId()
        val account = Account(1000)
        val command = DepositMoneyCommand(accountId, 500)
        val event = DepositMoneyCommandHandler(account).handle(command)
        assertEquals(accountId, event.accountId)
        assertEquals(500, event.amount)
        assertEquals(1500, account.onEvent(event).balance)
    }

    @Test
    fun `deposit money to a non existing account should generate an IllegalStateException`() {
        val command = DepositMoneyCommand(AccountId(), 500)
        val exception = assertThrows(IllegalStateException::class.java) {
            DepositMoneyCommandHandler(null).handle(command)
        }
        assertEquals("Account not found", exception.message)
    }

    @Test
    fun `deposit amount zero to an existing account should generate an IllegalArgumentException`() {
        val accountId = AccountId()
        val account = Account(1000)
        val command = DepositMoneyCommand(accountId, 0)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            DepositMoneyCommandHandler(account).handle(command)
        }
        assertEquals("Amount to deposit cannot be zero or negative", exception.message)
    }

    @Test
    fun `deposit a negative amount to an existing account should generate an IllegalArgumentException`() {
        val accountId = AccountId()
        val account = Account(1000)
        val command = DepositMoneyCommand(accountId, -500)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            DepositMoneyCommandHandler(account).handle(command)
        }
        assertEquals("Amount to deposit cannot be zero or negative", exception.message)
    }
}