package se.sbab.eventsourcing.commandhandler

import se.sbab.event.MoneyWithdrawnEvent
import se.sbab.eventsourcing.Event
import se.sbab.eventsourcing.command.WithdrawMoneyCommand
import se.sbab.eventsourcing.domain.Account
import se.sbab.eventsourcing.domain.AccountStatus

class WithdrawMoneyCommandHandler(private val accountState: Account?) {
    fun handle(command: WithdrawMoneyCommand): Event {
        checkNotNull(accountState) { "Account ${command.accountId} not found" }
        check(accountState.status != AccountStatus.CLOSED) { "Cannot withdraw money from a closed account" }
        check(accountState.balance >= command.amount) {
            "Cannot withdraw ${command.amount} on account with balance ${accountState.balance}"
        }
        return MoneyWithdrawnEvent(command.accountId, command.amount)
    }
}
