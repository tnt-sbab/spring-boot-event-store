package se.sbab.eventsourcing.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import se.sbab.demo.es.AccountId
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent
import se.sbab.eventsourcing.domain.Account
import se.sbab.eventsourcing.domain.AccountStatus

@SpringBootTest(
    properties = [
        "spring.kafka.properties.schema.registry.url=mock://localhost:8081", // mock:// prefix creates a mocked schema registry
        "spring.kafka.properties.auto.register.schemas=true",
        "events-payload-topic=account-events",
    ],
)
class EventStoreServiceIT {
    @Autowired
    lateinit var eventStoreService: EventStoreService

    @Test
    fun `Verify that the projected state for an AccountOpenedEvent is correct`() {
        val accountId = AccountId()
        eventStoreService.save(id = accountId, events = listOf(AccountOpenedEvent(accountId)))
        val result = eventStoreService.getState(accountId)
        val revision = result.first
        val accountState = result.second as Account
        assertEquals(accountId, accountState.id)
        assertEquals(0, accountState.balance)
        assertEquals(AccountStatus.ACTIVE, accountState.status)
        assertEquals(1, revision.value)
        assertEquals(1, getRevision(accountId).value)
    }

    @Test
    fun `Verify that the projected state for an AccountOpenedEvent followed by an MoneyDepositedEvent and MoneyWithdrawnEvent is correct`() {
        val accountId = AccountId()
        eventStoreService.save(id = accountId, events = listOf(AccountOpenedEvent(accountId)))
        eventStoreService.save(id = accountId, updatedFrom = getRevision(accountId), events = listOf(MoneyDepositedEvent(accountId, 1000)))
        eventStoreService.save(id = accountId, updatedFrom = getRevision(accountId), events = listOf(MoneyWithdrawnEvent(accountId, 100)))
        val result = eventStoreService.getState(accountId)
        val revision = result.first
        val accountState = result.second as Account
        assertEquals(accountId, accountState.id)
        assertEquals(900, accountState.balance)
        assertEquals(AccountStatus.ACTIVE, accountState.status)
        assertEquals(3, revision.value)
        assertEquals(3, getRevision(accountId).value)
    }

    @Test
    fun `Verify that the projected state for an AccountOpenedEvent followed by and MoneyDepositedEvent, an MoneyWithdrawnEvent and an MoneyDepositedEvent is correct`() {
        val accountId = AccountId()
        eventStoreService.save(id = accountId, events = listOf(AccountOpenedEvent(accountId)))
        eventStoreService.save(id = accountId, updatedFrom = getRevision(accountId), events = listOf(MoneyDepositedEvent(accountId, 500)))
        eventStoreService.save(id = accountId, updatedFrom = getRevision(accountId), events = listOf(MoneyWithdrawnEvent(accountId, 100)))
        eventStoreService.save(id = accountId, updatedFrom = getRevision(accountId), events = listOf(MoneyDepositedEvent(accountId, 400)))
        val result = eventStoreService.getState(accountId)
        val revision = result.first
        val accountState = result.second as Account
        assertEquals(accountId, accountState.id)
        assertEquals(800, accountState.balance)
        assertEquals(AccountStatus.ACTIVE, accountState.status)
        assertEquals(4, revision.value)
        assertEquals(4, getRevision(accountId).value)
    }

    @Test
    fun `Verify that the initial revision number for a non existing random id is zero`() {
        val revision = getRevision(AccountId())
        assertEquals(0, revision.value)
    }

    @Test
    fun `Verify that the revision number for an aggregate with one event is one`() {
        val accountId = AccountId()
        eventStoreService.save(id = accountId, events = listOf(AccountOpenedEvent(accountId)))
        val revision = getRevision(accountId)
        assertEquals(1, revision.value)
    }

    @Test
    fun `Verify all revision numbers from zero up to 100`() {
        val accountId = AccountId()
        assertEquals(0, getRevision(accountId).value)
        eventStoreService.save(id = accountId, events = listOf(AccountOpenedEvent(accountId)))
        assertEquals(1, getRevision(accountId).value)
        for (revisionNumber in 2..100) {
            eventStoreService.save(id = accountId, updatedFrom = getRevision(accountId), events = listOf(MoneyDepositedEvent(accountId, 10)))
            assertEquals(revisionNumber, getRevision(accountId).value)
        }
    }

    private fun getRevision(id: AccountId) = eventStoreService.getRevision(id)
}
