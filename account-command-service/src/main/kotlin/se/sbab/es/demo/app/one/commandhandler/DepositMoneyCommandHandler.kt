package se.sbab.es.demo.app.one.commandhandler

import se.sbab.es.demo.app.one.aggregate.Account
import se.sbab.es.demo.app.one.command.DepositMoneyCommand
import se.sbab.event.MoneyDepositedEvent

class DepositMoneyCommandHandler(private val account: Account?) {
    fun handle(command: DepositMoneyCommand): MoneyDepositedEvent {
        checkNotNull(account) { "Account not found" }
        require(command.depositAmount > 0) { "Amount to deposit cannot be zero or negative" }
        return MoneyDepositedEvent(command.accountId, command.depositAmount)
    }
}
