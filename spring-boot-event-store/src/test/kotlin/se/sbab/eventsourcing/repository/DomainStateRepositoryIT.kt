package se.sbab.eventsourcing.repository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import se.sbab.demo.es.AccountId
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent
import se.sbab.eventsourcing.domain.Account
import se.sbab.eventsourcing.service.EventStoreService
import java.util.UUID

@SpringBootTest(
    properties = [
        "spring.kafka.properties.schema.registry.url=mock://localhost:8081", // mock:// prefix creates a mocked schema registry
        "spring.kafka.properties.auto.register.schemas=true",
        "events-payload-topic=account-events",
    ],
)
class DomainStateRepositoryIT {
    @Autowired
    lateinit var eventStoreService: EventStoreService

    @Autowired
    lateinit var domainStateRepository: DomainStateRepository

    @Test
    fun `AggregateRepository get by id for one event should return latest projected state`() {
        val accountId = AccountId()
        eventStoreService.save(id = accountId, events = listOf(AccountOpenedEvent(accountId)))
        val result = eventStoreService.getState(accountId).second

        val aggregateRoot = domainStateRepository.getById(accountId) as Account
        Assertions.assertEquals(result, aggregateRoot)
        Assertions.assertEquals(0, aggregateRoot.balance)
    }

    @Test
    fun `AggregateRepository get by id for two events should return latest projected state`() {
        val accountId = AccountId()
        eventStoreService.save(
            id = accountId,
            events = listOf(
                AccountOpenedEvent(accountId),
                MoneyDepositedEvent(accountId, 1000),
                MoneyWithdrawnEvent(accountId, 100),
            ),
        )
        val result = eventStoreService.getState(accountId).second

        val aggregateRoot = domainStateRepository.getById(accountId) as Account
        Assertions.assertEquals(result, aggregateRoot)
        Assertions.assertEquals(900, aggregateRoot.balance)
    }

    @Test
    fun `AggregateRepository find by id for one event should return latest projected state`() {
        val accountId = AccountId()
        eventStoreService.save(
            id = accountId,
            events = listOf(
                AccountOpenedEvent(accountId),
                MoneyDepositedEvent(accountId, 1000),
            ),
        )
        val result = eventStoreService.getState(accountId).second

        val aggregateRoot = domainStateRepository.findById(accountId) as Account?
        Assertions.assertEquals(result, aggregateRoot)
        Assertions.assertEquals(1000, aggregateRoot?.balance)
    }

    @Test
    fun `AggregateRepository find by id should return null for a non existing id`() {
        val aggregateRoot = domainStateRepository.findById(UUID.randomUUID())
        Assertions.assertNull(aggregateRoot)
    }

    @Test
    fun `AggregateRepository get by id should throw an IllegalStateException for a non existing id`() {
        val id = UUID.fromString("8dae3548-7900-4781-8524-e19e7d80dc22")
        val exception: IllegalStateException = Assertions.assertThrows(IllegalStateException::class.java) {
            domainStateRepository.getById(id)
        }
        Assertions.assertEquals("Aggregate id '$id' not found.", exception.message)
    }
}
