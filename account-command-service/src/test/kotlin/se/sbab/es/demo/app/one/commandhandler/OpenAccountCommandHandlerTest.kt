package se.sbab.es.demo.app.one.commandhandler

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.app.one.aggregate.Account
import se.sbab.es.demo.app.one.command.OpenAccountCommand

internal class OpenAccountCommandHandlerTest {
    @Test
    fun `open a new account should generate an AccountOpenedEvent with the given account id`() {
        val accountId = AccountId()
        val command = OpenAccountCommand(accountId)
        val event = OpenAccountCommandHandler(null).handle(command)
        assertEquals(accountId, event.accountId)
    }

    @Test
    fun `an already opened account should generate an IllegalStateException `() {
        val account = Account()
        val command = OpenAccountCommand(AccountId())
        val exception = assertThrows(IllegalStateException::class.java) {
            OpenAccountCommandHandler(account).handle(command)
        }
        assertEquals("Account is already opened", exception.message)
    }
}