package se.sbab.es.demo.app.one.commandhandler

import se.sbab.es.demo.app.one.aggregate.Account
import se.sbab.es.demo.app.one.command.OpenAccountCommand
import se.sbab.event.AccountOpenedEvent

class OpenAccountCommandHandler(private val account: Account?) {
    fun handle(command: OpenAccountCommand): AccountOpenedEvent {
        check(account == null) { "Account is already opened" }
        return AccountOpenedEvent(command.accountId)
    }
}
