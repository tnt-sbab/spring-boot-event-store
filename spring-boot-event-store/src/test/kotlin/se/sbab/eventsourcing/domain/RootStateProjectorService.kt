package se.sbab.eventsourcing.domain

import se.sbab.event.AccountOpenedEvent
import se.sbab.eventsourcing.Event
import se.sbab.eventsourcing.service.RootStateProjector

// @Service
class RootStateProjectorService : RootStateProjector {
    override fun onEvent(event: Event): se.sbab.eventsourcing.domain.Account = when (event) {
        is AccountOpenedEvent -> se.sbab.eventsourcing.domain.Account(event)
        else -> throw IllegalArgumentException("No constructor event found for account")
    }
}
