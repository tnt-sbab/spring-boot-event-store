package se.sbab.eventsourcing.commandhandler

import se.sbab.event.MoneyDepositedEvent
import se.sbab.eventsourcing.Event
import se.sbab.eventsourcing.command.DepositMoneyCommand
import se.sbab.eventsourcing.domain.Account
import se.sbab.eventsourcing.domain.AccountStatus

class DepositMoneyCommandHandler(private val account: Account?) {
    fun handle(command: DepositMoneyCommand): Event {
        checkNotNull(account) { "Account ${command.accountId} not found" }
        check(account.status != AccountStatus.CLOSED) { "Cannot deposit money to a closed account" }

        return MoneyDepositedEvent(command.accountId, command.amount)
    }
}
