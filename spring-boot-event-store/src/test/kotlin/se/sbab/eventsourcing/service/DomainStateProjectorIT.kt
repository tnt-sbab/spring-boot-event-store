package se.sbab.eventsourcing.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import se.sbab.demo.es.AccountId
import se.sbab.event.AccountClosedEvent
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
class DomainStateProjectorIT {

    @Autowired
    lateinit var domainStateProjector: DomainStateProjector

    @Test
    fun testOpenAccount() {
        val accountId = AccountId()
        val events = listOf(AccountOpenedEvent(accountId))
        val accountState = domainStateProjector.currentState(events) as Account
        assertEquals(accountId, accountState.id)
        assertEquals(0, accountState.balance)
        assertEquals(AccountStatus.ACTIVE, accountState.status)
    }

    @Test
    fun testWithdrawFromAccount() {
        val accountId = AccountId()
        val events = listOf(
            AccountOpenedEvent(accountId),
            MoneyDepositedEvent(accountId, 300),
            MoneyWithdrawnEvent(accountId, 100),
        )
        val accountState = domainStateProjector.currentState(events) as Account
        assertEquals(accountId, accountState.id)
        assertEquals(200, accountState.balance)
        assertEquals(AccountStatus.ACTIVE, accountState.status)
    }

    @Test
    fun testWithdrawManyTimesAccount() {
        val accountId = AccountId()
        val events = listOf(
            AccountOpenedEvent(accountId),
            MoneyDepositedEvent(accountId, 300),
            MoneyWithdrawnEvent(accountId, 100),
            MoneyWithdrawnEvent(accountId, 50),
            MoneyWithdrawnEvent(accountId, 25),
        )
        val accountState = domainStateProjector.currentState(events) as Account
        assertEquals(accountId, accountState.id)
        assertEquals(125, accountState.balance)
        assertEquals(AccountStatus.ACTIVE, accountState.status)
    }

    @Test
    fun testDepositToAccount() {
        val accountId = AccountId()
        val events = listOf(
            AccountOpenedEvent(accountId),
            MoneyDepositedEvent(accountId, 300),
            MoneyDepositedEvent(accountId, 100),
        )
        val accountState = domainStateProjector.currentState(events) as Account
        assertEquals(accountId, accountState.id)
        assertEquals(400, accountState.balance)
        assertEquals(AccountStatus.ACTIVE, accountState.status)
    }

    @Test
    fun testDepositToAndWithdrawMoneyFromAccount() {
        val accountId = AccountId()
        val events = listOf(
            AccountOpenedEvent(accountId),
            MoneyDepositedEvent(accountId, 300),
            MoneyDepositedEvent(accountId, 100),
            MoneyWithdrawnEvent(accountId, 50),
        )
        val accountState = domainStateProjector.currentState(events) as Account
        assertEquals(accountId, accountState.id)
        assertEquals(350, accountState.balance)
        assertEquals(AccountStatus.ACTIVE, accountState.status)
    }

    @Test
    fun testCloseAccount() {
        val accountId = AccountId()
        val events = listOf(
            AccountOpenedEvent(accountId),
            MoneyDepositedEvent(accountId, 300),
            MoneyWithdrawnEvent(accountId, 300),
            AccountClosedEvent(accountId),
        )
        val accountState = domainStateProjector.currentState(events) as Account
        assertEquals(accountId, accountState.id)
        assertEquals(0, accountState.balance)
        assertEquals(AccountStatus.CLOSED, accountState.status)
    }
}
