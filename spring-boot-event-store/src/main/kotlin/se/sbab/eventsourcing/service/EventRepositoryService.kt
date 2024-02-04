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
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import se.sbab.eventsourcing.Event
import se.sbab.eventsourcing.repository.EventEntity
import se.sbab.eventsourcing.repository.EventRepository

interface EventRepositoryService {
    fun saveAll(events: List<Event>, entities: List<EventEntity>)
}

@Service
class EventRepositoryServiceImpl(
    private val repository: EventRepository,
    private val sequenceRepositoryService: SequenceRepositoryService,
    private val applicationEventPublisher: ApplicationEventPublisher,
) : EventRepositoryService {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Timed(value = "event_repository_save_all", percentiles = [0.1, 0.5, 0.75, 0.9, 0.95, 0.99])
    override fun saveAll(events: List<Event>, entities: List<EventEntity>) {
        val entitiesWithIds = assignSequenceNumbers(entities)
        events.zip(entitiesWithIds).forEach { (event, entity) ->
            applicationEventPublisher.publishEvent(event)
            applicationEventPublisher.publishEvent(entity)
        }
        repository.saveAll(entitiesWithIds)
    }

    private fun assignSequenceNumbers(entities: List<EventEntity>): List<EventEntity> {
        val sequences = sequenceRepositoryService.fetchSequenceValues(entities.size)
        return sequences.zip(entities).map { (sequence, entity) ->
            entity.copy(id = sequence)
        }
    }
}
