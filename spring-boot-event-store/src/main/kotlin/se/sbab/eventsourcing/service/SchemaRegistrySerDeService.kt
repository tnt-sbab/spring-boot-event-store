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

import io.confluent.kafka.schemaregistry.avro.AvroSchema
import io.confluent.kafka.serializers.KafkaAvroDeserializer
import io.confluent.kafka.serializers.KafkaAvroSerializer
import org.apache.avro.Schema
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import se.sbab.eventsourcing.Event

@Service
class SchemaRegistrySerDeService(
    private val serializer: KafkaAvroSerializer,
    private val deserializer: KafkaAvroDeserializer,
    @Value("\${events-payload-topic}") private val topic: String,
) : SerDeService {
    override fun serialize(record: Event): ByteArray = serializer.serialize(topic, record)

    override fun deserialize(bytes: ByteArray): Event = deserializer.deserialize(topic, bytes) as Event

    fun register(className: String, schema: Schema) = serializer.register(getSubject(className), AvroSchema(schema))

    private fun getSubject(fullyQualifiedClassName: String) = "$topic-$fullyQualifiedClassName"
}
