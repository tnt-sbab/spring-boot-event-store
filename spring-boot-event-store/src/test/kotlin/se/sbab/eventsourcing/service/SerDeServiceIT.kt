package se.sbab.eventsourcing.service

import org.apache.commons.codec.binary.Hex
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import se.sbab.demo.es.AccountId
import se.sbab.event.AccountClosedEvent
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyWithdrawnEvent
import java.nio.ByteBuffer

@SpringBootTest(
    properties = [
        "spring.kafka.properties.schema.registry.url=mock://localhost:8081", // mock:// prefix creates a mocked schema registry
        "spring.kafka.properties.auto.register.schemas=true",
        "events-payload-topic=account-events",
    ],
)
@ContextConfiguration(classes = [KafkaAvroSerDeFactory::class, SchemaRegistrySerDeService::class])
class SerDeServiceIT {
    @Autowired
    lateinit var serDeService: SchemaRegistrySerDeService

    @Test
    fun `serialize and deserialize a AccountOpenedEvent`() {
        val event = AccountOpenedEvent(AccountId())
        val bytes = serDeService.serialize(event)
        val deserializedEvent = serDeService.deserialize(bytes)
        assertEquals(event, deserializedEvent)
        assertEquals(0.toByte(), bytes[0]) // Verify magic byte
        println("Schema ID: " + ByteBuffer.wrap(bytes, 1, 4).int)
        println(Hex.encodeHexString(bytes))
    }

    @Test
    fun `serialize and deserialize a AccountClosedEvent`() {
        val event = AccountClosedEvent(AccountId())
        val bytes = serDeService.serialize(event)
        val deserializedEvent = serDeService.deserialize(bytes)
        assertEquals(event, deserializedEvent)
        println("Schema ID: " + ByteBuffer.wrap(bytes, 1, 4).int)
        println(Hex.encodeHexString(bytes))
    }

    @Test
    fun `serialize and deserialize a MoneyWithdrawnEvent`() {
        val event = MoneyWithdrawnEvent(AccountId(), 100)
        val bytes = serDeService.serialize(event)
        val deserializedEvent = serDeService.deserialize(bytes)
        assertEquals(event, deserializedEvent)
        println("Schema ID: " + ByteBuffer.wrap(bytes, 1, 4).int)
        println(Hex.encodeHexString(bytes))
    }
}
