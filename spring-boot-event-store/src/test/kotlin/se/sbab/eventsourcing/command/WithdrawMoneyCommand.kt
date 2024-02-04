package se.sbab.eventsourcing.command

import se.sbab.demo.es.AccountId

data class WithdrawMoneyCommand(
    val accountId: AccountId,
    val amount: Int,
)
