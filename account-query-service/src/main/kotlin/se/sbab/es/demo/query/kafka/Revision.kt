package se.sbab.es.demo.query.kafka

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonCreator.Mode.DELEGATING
import com.fasterxml.jackson.annotation.JsonValue
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.nio.ByteBuffer

data class Revision @JsonCreator(mode = DELEGATING) constructor(@JsonValue val value: Int) {
    companion object {
        const val REVISION_HEADER = "revision"
    }

    // This constructor is used when consuming from the Kafka revision header
    constructor(value: ByteArray) : this(ByteBuffer.wrap(value).int)

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(id: String) : this(Integer.valueOf(id))

    fun next() = Revision(value + 1)

    override fun toString() = value.toString()

    operator fun compareTo(revision: Revision): Int = value.compareTo(revision.value)
}

@Converter
class RevisionConverter : AttributeConverter<Revision, Int> {
    override fun convertToDatabaseColumn(attribute: Revision): Int = attribute.value

    override fun convertToEntityAttribute(dbData: Int?): Revision? = dbData?.let { Revision(dbData) }
}
