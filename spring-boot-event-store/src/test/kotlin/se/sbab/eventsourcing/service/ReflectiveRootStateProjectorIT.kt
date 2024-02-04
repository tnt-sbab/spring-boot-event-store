package se.sbab.eventsourcing.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import se.sbab.demo.es.AccountId
import se.sbab.event.AccountOpenedEvent
import se.sbab.eventsourcing.domain.Account

@SpringBootTest(
    properties = [
        "spring.kafka.properties.schema.registry.url=mock://localhost:8081", // mock:// prefix creates a mocked schema registry
        "spring.kafka.properties.auto.register.schemas=true",
        "events-payload-topic=account-events",
    ],
)
class ReflectiveRootStateProjectorIT {
    @Autowired
    lateinit var rootStateProjector: RootStateProjector

    @Test
    fun `root state projector should find the constructor with AccountOpenedEvent as argument`() {
        val accountId = AccountId()
        val event = AccountOpenedEvent(accountId)
        val state = rootStateProjector.onEvent(event) as Account
        assertEquals(0, state.balance)
    }
}
