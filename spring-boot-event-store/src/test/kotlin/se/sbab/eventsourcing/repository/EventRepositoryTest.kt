package se.sbab.eventsourcing.repository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.OffsetDateTime
import java.util.UUID

@ExtendWith(SpringExtension::class)
@DataJpaTest
internal class EventRepositoryTest {
    @Autowired
    lateinit var eventRepository: EventRepository

    @BeforeEach
    fun init() {
        eventRepository.deleteAll()
    }

    @Test
    fun `save and find by AggregateId should result in identical entities`() {
        val aggregateId = UUID.randomUUID()
        val entity = createEventEntity(aggregateId = aggregateId)
        eventRepository.save(entity)
        val events = eventRepository.findAllByAggregateIdOrderByRevision(aggregateId)
        assertEquals(1, events.size)
        assertEquals(entity, events[0])
    }

    @Test
    fun `save two EventEntity objects with same aggregate id and aggregate revision should result in a DataIntegrityViolationException`() {
        val aggregateId = UUID.randomUUID()
        eventRepository.save(createEventEntity(aggregateId = aggregateId))
        Assertions.assertThrows(DataIntegrityViolationException::class.java) {
            eventRepository.save(createEventEntity(aggregateId = aggregateId))
            eventRepository.count() // This will cause a flush to the database
        }
    }

    private fun createEventEntity(
        aggregateId: UUID = UUID.randomUUID(),
        revision: Revision = Revision(1),
    ) = EventEntity(
        aggregateId = aggregateId,
        occurredAt = OffsetDateTime.now(),
        revision = revision,
        payload = ByteArray(0),
    )
}
