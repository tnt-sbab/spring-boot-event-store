/**
 * Copyright 2024 SBAB Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.sbab.eventsourcing.service

import io.micrometer.core.annotation.Timed
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import se.sbab.eventsourcing.AggregateId
import se.sbab.eventsourcing.DomainState
import se.sbab.eventsourcing.Event

interface CommandService<T : DomainState> {
    fun apply(aggregateId: AggregateId, updateFunction: (T?) -> Event): Event

    fun applyList(aggregateId: AggregateId, updateFunction: (T?) -> List<Event>): List<Event>

    fun applyListWithoutState(aggregateId: AggregateId, updateFunction: () -> List<Event>): List<Event>
}

@Service
class CommandServiceImpl<T : DomainState>(private val eventStoreService: EventStoreService) : CommandService<T> {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Timed(value = "event_store_persist_aggregate", percentiles = [0.1, 0.5, 0.75, 0.9, 0.95, 0.99])
    override fun apply(aggregateId: AggregateId, updateFunction: (T?) -> Event): Event {
        return retryOnDataIntegrityViolationException {
            val (revision, state) = eventStoreService.getState(aggregateId)
            val event = updateFunction(state as T?)
            eventStoreService.save(aggregateId, revision, listOf(event))
            event
        }
    }

    @Timed(value = "event_store_persist_aggregate", percentiles = [0.1, 0.5, 0.75, 0.9, 0.95, 0.99])
    override fun applyList(aggregateId: AggregateId, updateFunction: (T?) -> List<Event>): List<Event> {
        return retryOnDataIntegrityViolationException {
            val (revision, state) = eventStoreService.getState(aggregateId)
            val events = updateFunction(state as T?)
            eventStoreService.save(aggregateId, revision, events)
            events
        }
    }

    @Timed(value = "event_store_persist_aggregate", percentiles = [0.1, 0.5, 0.75, 0.9, 0.95, 0.99])
    override fun applyListWithoutState(aggregateId: AggregateId, updateFunction: () -> List<Event>): List<Event> {
        return retryOnDataIntegrityViolationException {
            val revision = eventStoreService.getRevision(aggregateId)
            val events = updateFunction()
            eventStoreService.save(aggregateId, revision, events)
            events
        }
    }

    private fun <T> retryOnDataIntegrityViolationException(
        block: () -> T,
    ): T {
        while (true) {
            try {
                return block()
            } catch (e: DataIntegrityViolationException) {
                logger.debug("Concurrent save operations detected. Will perform a retry.")
            }
        }
    }
}
