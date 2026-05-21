package se.sbab.eventsourcing.domain

import se.sbab.event.AccountOpenedEvent
import se.sbab.eventsourcing.Event
import se.sbab.eventsourcing.service.RootStateProjector

// @Service // Integration tests are configured to use the reflective RootStateProjector
class RootStateProjectorService : RootStateProjector {
    override fun onEvent(event: Event): Account = when (event) {
        is AccountOpenedEvent -> Account(event)
        else -> throw IllegalArgumentException("No constructor event found for account")
    }
}
