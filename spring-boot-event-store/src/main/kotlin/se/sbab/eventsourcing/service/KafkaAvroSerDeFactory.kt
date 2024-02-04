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

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig
import io.confluent.kafka.serializers.KafkaAvroSerializer
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class KafkaAvroSerDeFactory(
    @Value("\${spring.kafka.properties.schema.registry.url}") private val schemaRegistryUrl: String,
    @Value("\${spring.kafka.properties.auto.register.schemas:false}") private val autoRegisterSchemas: Boolean,
) {
    @Bean
    fun getKafkaAvroSerializer(): KafkaAvroSerializer {
        val serializer = KafkaAvroSerializer()
        serializer.configure(getCommonProperties(), false)
        return serializer
    }

    @Bean
    fun getKafkaAvroDeserializer(): KafkaAvroDeserializer {
        val deserializer = KafkaAvroDeserializer()
        deserializer.configure(getDeserializerProperties(), false)
        return deserializer
    }

    private fun getDeserializerProperties(): MutableMap<String, Any?> {
        val properties = getCommonProperties()
        properties[KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG] = true
        return properties
    }

    private fun getCommonProperties(): MutableMap<String, Any?> {
        val properties: MutableMap<String, Any?> = HashMap()
        properties[AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG] = schemaRegistryUrl
        properties[AbstractKafkaSchemaSerDeConfig.AUTO_REGISTER_SCHEMAS] = autoRegisterSchemas
        properties[AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY] = TopicRecordNameStrategy::class.qualifiedName
        return properties
    }
}
