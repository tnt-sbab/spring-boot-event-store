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

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import java.nio.ByteBuffer

data class Revision(val value: Int = 0) {
    companion object {
        const val REVISION_HEADER = "revision"
    }

    constructor(value: ByteArray) : this(ByteBuffer.wrap(value).int)

    fun getBytes(): ByteArray = ByteBuffer.allocate(4).putInt(value).array()

    fun next() = copy(value = value + 1)

    override fun toString() = value.toString()
}

@Converter
class RevisionConverter : AttributeConverter<Revision, Int> {
    override fun convertToDatabaseColumn(attribute: Revision): Int = attribute.value

    override fun convertToEntityAttribute(dbData: Int?): Revision = dbData?.let { Revision(dbData) } ?: Revision()
}
