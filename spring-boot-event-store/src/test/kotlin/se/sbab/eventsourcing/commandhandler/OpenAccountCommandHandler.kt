package se.sbab.eventsourcing.commandhandler

import se.sbab.event.AccountOpenedEvent
import se.sbab.eventsourcing.Event
import se.sbab.eventsourcing.command.OpenAccountCommand
import se.sbab.eventsourcing.domain.Account

class OpenAccountCommandHandler(private val accountState: Account?) {
    fun handle(command: OpenAccountCommand): Event {
        check(accountState == null) { "Account ${command.accountId} already opened" }
        return AccountOpenedEvent(command.accountId)
    }
}
