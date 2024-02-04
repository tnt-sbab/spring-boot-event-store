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

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import se.sbab.eventsourcing.AggregateId
import se.sbab.eventsourcing.repository.EventRepository
import se.sbab.eventsourcing.toJson
import java.time.OffsetDateTime

@Service
class EventsService(
    private val eventRepository: EventRepository,
    private val serDeService: SerDeService,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    fun getEvents(aggregateId: AggregateId): List<EventDto> {
        logger.info("Get all events for aggregateId=$aggregateId")
        return eventRepository.findAllByAggregateIdOrderByRevision(aggregateId.get()).map {
            val event = serDeService.deserialize(it.payload)
            EventDto(event.schema.name, it.occurredAt, it.revision.value, event.toJson())
        }
    }
}

data class EventDto(val name: String, val occurredAt: OffsetDateTime, val revision: Int, val eventPayload: String)
