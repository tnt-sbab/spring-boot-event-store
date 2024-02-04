package se.sbab.es.demo.app.one.aggregate

import se.sbab.eventsourcing.DomainState
import se.sbab.eventsourcing.Event
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent

data class Account(val balance: Int = 0) : DomainState {
    constructor(event: AccountOpenedEvent) : this()

    private fun on(event: MoneyWithdrawnEvent) = copy(balance = balance - event.amount)

    private fun on(event: MoneyDepositedEvent) = copy(balance = balance + event.amount)

    override fun onEvent(event: Event): Account =
        when (event) {
            is MoneyWithdrawnEvent -> on(event)
            is MoneyDepositedEvent -> on(event)
            else -> throw IllegalArgumentException("Unsupported event type")
        }
}
