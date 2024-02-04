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
import org.springframework.stereotype.Service
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import se.sbab.eventsourcing.AggregateId
import se.sbab.eventsourcing.DomainState
import se.sbab.eventsourcing.Event
import se.sbab.eventsourcing.repository.EventEntity
import se.sbab.eventsourcing.repository.EventRepository
import se.sbab.eventsourcing.repository.Revision
import java.util.UUID

interface EventStoreService {
    fun save(id: AggregateId, updatedFrom: Revision = Revision(), events: List<Event>)

    fun getState(id: AggregateId): Pair<Revision, DomainState?>

    fun getRevision(id: AggregateId): Revision
}

@Service
class EventStoreServiceImpl(
    private val repository: EventRepository,
    private val projector: DomainStateProjector,
    private val serDeService: SerDeService,
    private val eventRepositoryService: EventRepositoryService,
) : EventStoreService {
    companion object {
        const val REVISION_HEADER_NAME = "revision"
        const val AGGREGATE_ID_HEADER_NAME = "aggregate-id"
    }

    @Timed(value = "event_store_save_aggregate", percentiles = [0.1, 0.5, 0.75, 0.9, 0.95, 0.99])
    override fun save(id: AggregateId, updatedFrom: Revision, events: List<Event>) {
        var revision = updatedFrom
        val entities = events.map { event ->
            val eventEntity = convert(id.get(), revision, event)
            revision = revision.next()
            eventEntity
        }
        eventRepositoryService.saveAll(events, entities)
        addResponseHeaders(id.get(), revision)
    }

    @Timed(value = "event_store_read_aggregate", percentiles = [0.1, 0.5, 0.75, 0.9, 0.95, 0.99])
    override fun getState(id: AggregateId): Pair<Revision, DomainState?> {
        val eventEntityList = repository.findAllByAggregateIdOrderByRevision(id.get())
        if (eventEntityList.isEmpty()) {
            return Pair(Revision(), null)
        }
        val events = serDeService.deserialize(eventEntityList)
        return Pair(getRevision(eventEntityList), projector.currentState(events))
    }

    override fun getRevision(id: AggregateId): Revision = repository.findRevisionForAggregateId(id.get())

    private fun getRevision(events: List<EventEntity>) = events.last().revision

    private fun convert(aggregateId: UUID, updatedFrom: Revision, event: Event): EventEntity {
        return EventEntity(
            aggregateId = aggregateId,
            revision = updatedFrom.next(),
            payload = serDeService.serialize(event),
        )
    }

    private fun addResponseHeaders(aggregateId: UUID, revision: Revision) {
        val requestAttributes = RequestContextHolder.getRequestAttributes()
        if (requestAttributes is ServletRequestAttributes) {
            requestAttributes.response?.let { response ->
                response.setHeader(AGGREGATE_ID_HEADER_NAME, aggregateId.toString())
                response.setIntHeader(REVISION_HEADER_NAME, revision.value)
            }
        }
    }
}
