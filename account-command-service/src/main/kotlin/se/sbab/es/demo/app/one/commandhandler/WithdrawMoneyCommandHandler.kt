package se.sbab.es.demo.app.one.commandhandler

import se.sbab.es.demo.app.one.aggregate.Account
import se.sbab.es.demo.app.one.command.WithdrawMoneyCommand
import se.sbab.event.MoneyWithdrawnEvent

class WithdrawMoneyCommandHandler(private val account: Account?) {
    fun handle(command: WithdrawMoneyCommand): MoneyWithdrawnEvent {
        checkNotNull(account) { "Account not found" }
        require(command.withdrawAmount > 0) { "Amount to withdraw cannot be zero or negative" }
        check(account.balance >= command.withdrawAmount) { "Insufficient account balance" }
        return MoneyWithdrawnEvent(command.accountId, command.withdrawAmount)
    }
}
