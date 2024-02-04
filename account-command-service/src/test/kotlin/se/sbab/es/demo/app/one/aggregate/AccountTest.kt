package se.sbab.es.demo.app.one.aggregate

import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import se.sbab.demo.es.AccountId
import se.sbab.event.AccountClosedEvent
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent

internal class AccountTest {

    @Test
    fun `initial balance for an account should be zero`() {
        val event = AccountOpenedEvent(AccountId())
        val account = Account(event)
        assertEquals(0, account.balance)
    }

    @Test
    fun `deposit money to an account should increase the balance with that amount`() {
        val account = Account(1000)
        val event = MoneyDepositedEvent(AccountId(), 100)
        assertEquals(1100, account.onEvent(event).balance)
    }

    @Test
    fun `withdraw money from an account should decrease the balance with that amount`() {
        val account = Account(1000)
        val event = MoneyWithdrawnEvent(AccountId(), 100)
        assertEquals(900, account.onEvent(event).balance)
    }

    @Test
    fun `an already opened account can not be opened again`() {
        val account = Account(1000)
        val event = AccountOpenedEvent(AccountId())
        val exception = assertThrows(IllegalArgumentException::class.java) {
            account.onEvent(event)
        }
        assertEquals("Unsupported event type", exception.message)
    }

    @Test
    fun `unsupported events should throw an IllegalArgumentException`() {
        val account = Account(1000)
        val event = AccountClosedEvent(AccountId())
        val exception = assertThrows(IllegalArgumentException::class.java) {
            account.onEvent(event)
        }
        assertEquals("Unsupported event type", exception.message)
    }
}