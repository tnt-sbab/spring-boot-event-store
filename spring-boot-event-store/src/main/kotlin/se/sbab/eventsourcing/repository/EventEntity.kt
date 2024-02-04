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

package se.sbab.eventsourcing.repository

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Lob
import org.springframework.data.annotation.Immutable
import java.time.OffsetDateTime
import java.util.UUID

@Entity(name = "EVENTS")
@Immutable
data class EventEntity(
    @Id
    val id: Long = 0,

    @Column(nullable = false)
    val aggregateId: UUID,

    @Column(nullable = false)
    @Convert(converter = RevisionConverter::class)
    val revision: Revision,

    @Column(nullable = false, insertable = false)
    val occurredAt: OffsetDateTime = OffsetDateTime.MIN, // Set by a default value in the database

    @Lob
    @Column(nullable = false)
    val payload: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EventEntity) return false
        if (aggregateId != other.aggregateId) return false
        if (revision != other.revision) return false
        return true
    }

    override fun hashCode(): Int {
        var result = aggregateId.hashCode()
        result = 31 * result + revision.value
        return result
    }
}
