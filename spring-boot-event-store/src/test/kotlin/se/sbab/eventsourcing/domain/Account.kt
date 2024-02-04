package se.sbab.eventsourcing.domain

import se.sbab.demo.es.AccountId
import se.sbab.event.AccountClosedEvent
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent
import se.sbab.eventsourcing.DomainState
import se.sbab.eventsourcing.Event

data class Account(val id: AccountId, val balance: Int = 0, val status: se.sbab.eventsourcing.domain.AccountStatus = se.sbab.eventsourcing.domain.AccountStatus.ACTIVE) :
    DomainState {
    constructor(event: AccountOpenedEvent) : this(
        id = event.accountId,
    )

    private fun on(event: MoneyWithdrawnEvent) = copy(balance = balance - event.amount)

    private fun on(event: MoneyDepositedEvent) = copy(balance = balance + event.amount)

    private fun onAccountClosedEvent() = copy(status = se.sbab.eventsourcing.domain.AccountStatus.CLOSED)

    override fun onEvent(event: Event): DomainState =
        when (event) {
            is AccountOpenedEvent -> throw IllegalArgumentException("Account is already open")
            is AccountClosedEvent -> onAccountClosedEvent()
            is MoneyWithdrawnEvent -> on(event)
            is MoneyDepositedEvent -> on(event)
            else -> throw IllegalArgumentException("Unknown event type for account")
        }
}

enum class AccountStatus {
    ACTIVE,
    CLOSED,
}
