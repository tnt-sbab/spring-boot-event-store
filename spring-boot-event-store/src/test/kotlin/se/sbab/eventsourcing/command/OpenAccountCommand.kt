package se.sbab.eventsourcing.command

import se.sbab.demo.es.AccountId

data class OpenAccountCommand(
    val accountId: AccountId,
)
