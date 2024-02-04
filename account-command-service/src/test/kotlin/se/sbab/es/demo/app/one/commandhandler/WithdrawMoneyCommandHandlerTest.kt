package se.sbab.es.demo.app.one.commandhandler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.app.one.aggregate.Account
import se.sbab.es.demo.app.one.command.WithdrawMoneyCommand

class WithdrawMoneyCommandHandlerTest {
    @Test
    fun `withdraw money should generate a MoneyWithdrawnEvent`() {
        val accountId = AccountId()
        val account = Account(1000)
        val command = WithdrawMoneyCommand(accountId, 400)
        val event = WithdrawMoneyCommandHandler(account).handle(command)
        assertEquals(accountId, event.accountId)
        assertEquals(400, event.amount)
        assertEquals(600, account.onEvent(event).balance)
    }

    @Test
    fun `withdraw all remaining money should generate an MoneyWithdrawnEvent`() {
        val accountId = AccountId()
        val account = Account(1000)
        val command = WithdrawMoneyCommand(accountId, 1000)
        val event = WithdrawMoneyCommandHandler(account).handle(command)
        assertEquals(accountId, event.accountId)
        assertEquals(1000, event.amount)
        assertEquals(0, account.onEvent(event).balance)
    }

    @Test
    fun `withdraw money from a non existing account should generate an IllegalStateException`() {
        val command = WithdrawMoneyCommand(AccountId(), 500)
        val exception = assertThrows(IllegalStateException::class.java) {
            WithdrawMoneyCommandHandler(null).handle(command)
        }
        assertEquals("Account not found", exception.message)
    }

    @Test
    fun `withdraw amount zero should generate an IllegalArgumentException`() {
        val accountId = AccountId()
        val account = Account(1000)
        val command = WithdrawMoneyCommand(accountId, 0)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            WithdrawMoneyCommandHandler(account).handle(command)
        }
        assertEquals("Amount to withdraw cannot be zero or negative", exception.message)
    }

    @Test
    fun `withdraw a negative amount should generate an IllegalArgumentException`() {
        val accountId = AccountId()
        val account = Account(1000)
        val command = WithdrawMoneyCommand(accountId, -500)
        val exception = assertThrows(IllegalArgumentException::class.java) {
            WithdrawMoneyCommandHandler(account).handle(command)
        }
        assertEquals("Amount to withdraw cannot be zero or negative", exception.message)
    }

    @Test
    fun `withdraw an amount that exceeds the account balance should generate an IllegalStateException`() {
        val accountId = AccountId()
        val account = Account(1000)
        val command = WithdrawMoneyCommand(accountId, 1500)
        val exception = assertThrows(IllegalStateException::class.java) {
            WithdrawMoneyCommandHandler(account).handle(command)
        }
        assertEquals("Insufficient account balance", exception.message)
    }
}