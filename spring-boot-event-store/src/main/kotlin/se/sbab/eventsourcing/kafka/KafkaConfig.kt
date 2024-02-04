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

import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.context.properties.PropertyMapper
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.kafka.support.KafkaHeaderMapper
import org.springframework.kafka.support.ProducerListener
import org.springframework.kafka.support.SimpleKafkaHeaderMapper
import org.springframework.kafka.support.converter.RecordMessageConverter

@Configuration
@ConditionalOnClass(KafkaTemplate::class)
@EnableConfigurationProperties(KafkaProperties::class)
class KafkaConfig(@Autowired private val kafkaProperties: KafkaProperties) {
    @Bean
    fun simpleKafkaHeaderMapper(): KafkaHeaderMapper = SimpleKafkaHeaderMapper()

    @Bean
    @ConditionalOnProperty(name = ["publish-events"], havingValue = "true", matchIfMissing = false)
    fun byteArrayKafkaTemplate(): KafkaTemplate<String, ByteArray> {
        val configProperties = kafkaProperties.buildProducerProperties()
        configProperties[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.qualifiedName
        configProperties[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ByteArraySerializer::class.qualifiedName
        val eventProducerFactory = DefaultKafkaProducerFactory<String, ByteArray>(configProperties)
        return KafkaTemplate(eventProducerFactory)
    }

    /*
     * Implementation from org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration.kafkaTemplate
     * https://github.com/spring-projects/spring-boot/blob/main/spring-boot-project/spring-boot-autoconfigure/src/main/java/org/springframework/boot/autoconfigure/kafka/KafkaAutoConfiguration.java#L92
     */
    @Bean
    fun kafkaTemplate(
        kafkaProducerFactory: ProducerFactory<Any, Any>,
        kafkaProducerListener: ProducerListener<Any, Any>,
        messageConverter: ObjectProvider<RecordMessageConverter>,
    ): KafkaTemplate<*, *> {
        val map = PropertyMapper.get().alwaysApplyingWhenNonNull()
        val kafkaTemplate = KafkaTemplate(kafkaProducerFactory)
        messageConverter.ifUnique { kafkaTemplate.messageConverter = it }
        map.from(kafkaProducerListener).to(kafkaTemplate::setProducerListener)
        map.from(kafkaProperties.template.defaultTopic).to(kafkaTemplate::setDefaultTopic)
        map.from(kafkaProperties.template.transactionIdPrefix).to(kafkaTemplate::setTransactionIdPrefix)
        kafkaTemplate.setObservationEnabled(true)
        return kafkaTemplate
    }
}
