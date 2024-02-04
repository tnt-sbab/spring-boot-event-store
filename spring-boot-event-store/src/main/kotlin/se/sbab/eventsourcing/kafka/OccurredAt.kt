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

import java.nio.ByteBuffer

/**
 * Used to set a kafka header for resend events that can be used to sort events in original
 * send order.
 */
data class OccurredAt(val value: Long) {
    companion object {
        const val OCCURRED_AT_HEADER = "occurred_at"
    }

    constructor(value: ByteArray) : this(ByteBuffer.wrap(value).long)

    fun getBytes(): ByteArray = ByteBuffer.allocate(8).putLong(value).array()

    override fun toString() = value.toString()
}
