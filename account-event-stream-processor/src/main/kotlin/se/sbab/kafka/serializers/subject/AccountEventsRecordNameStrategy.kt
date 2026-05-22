package se.sbab.kafka.serializers.subject

import io.confluent.kafka.schemaregistry.ParsedSchema
import io.confluent.kafka.serializers.subject.TopicRecordNameStrategy

/**
 * This subject name strategy will avoid having to register all the events in the Schema Registry again on the
 * outgoing topic.
 */
class AccountEventsRecordNameStrategy : TopicRecordNameStrategy() {
    override fun subjectName(topic: String?, isKey: Boolean, schema: ParsedSchema?): String {
        return super.subjectName("account-events", isKey, schema)
    }
}
