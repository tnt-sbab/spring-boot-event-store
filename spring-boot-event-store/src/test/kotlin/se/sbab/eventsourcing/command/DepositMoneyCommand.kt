package se.sbab.eventsourcing.command

import se.sbab.demo.es.AccountId

data class DepositMoneyCommand(
    val accountId: AccountId,
    val amount: Int,
)
