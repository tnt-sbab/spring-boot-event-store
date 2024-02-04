package se.sbab.es.demo.query.kafka

import org.slf4j.LoggerFactory
import org.springframework.context.ApplicationEventPublisher
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.KafkaHeaders.RECEIVED_KEY
import org.springframework.messaging.handler.annotation.Header
import org.springframework.stereotype.Component
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.Event
import se.sbab.es.demo.query.kafka.Revision.Companion.REVISION_HEADER

@Component
class EventConsumer(private val applicationEventPublisher: ApplicationEventPublisher) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @KafkaListener(topics = ["account-events"])
    fun onEvent(
        event: Event,
        @Header(RECEIVED_KEY) accountId: AccountId,
        @Header(REVISION_HEADER) revision: Revision
    ) {
        logger.info("Received event with key=$accountId and revision=$revision: $event")
        publishEvent(event, accountId, revision)
    }

    fun publishEvent(event: Event, accountId: AccountId, revision: Revision) {
        applicationEventPublisher.publishEvent(event)
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, revision))
    }
}