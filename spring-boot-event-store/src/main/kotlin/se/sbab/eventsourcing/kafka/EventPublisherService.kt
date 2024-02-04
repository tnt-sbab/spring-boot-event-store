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

package se.sbab.eventsourcing.kafka

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.KafkaHeaders
import org.springframework.messaging.support.MessageBuilder
import org.springframework.stereotype.Service
import org.springframework.transaction.event.TransactionalEventListener
import se.sbab.eventsourcing.repository.EventEntity
import se.sbab.eventsourcing.repository.Revision

@Service
@ConditionalOnProperty(name = ["publish-events"], havingValue = "true", matchIfMissing = false)
@ConditionalOnClass(KafkaTemplate::class)
class EventPublisherService(
    private val byteArrayKafkaTemplate: KafkaTemplate<String, ByteArray>,
    @Value("\${events-payload-topic}") private val eventsTopic: String,
) {
    private val logger = LoggerFactory.getLogger(javaClass)

    @TransactionalEventListener
    @Order(Ordered.HIGHEST_PRECEDENCE)
    fun on(event: EventEntity) {
        logger.info("EventEntity received $event")
        val message = MessageBuilder.withPayload(event.payload)
            .setHeader(KafkaHeaders.KEY, event.aggregateId.toString())
            .setHeader(KafkaHeaders.TOPIC, eventsTopic)
            .setHeader(Revision.REVISION_HEADER, event.revision.getBytes())
            .build()
        byteArrayKafkaTemplate.send(message)
    }
}
