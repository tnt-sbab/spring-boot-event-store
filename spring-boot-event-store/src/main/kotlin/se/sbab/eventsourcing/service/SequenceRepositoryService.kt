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

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import se.sbab.eventsourcing.repository.SequenceRepository

interface SequenceRepositoryService {
    fun fetchSequenceValues(numberOfSequenceValues: Int): List<Long>
}

@Service
class SequenceRepositoryServiceImpl(
    private val sequenceRepository: SequenceRepository,
) : SequenceRepositoryService {
    // New sequence values should only be fetched inside already running transactions
    @Transactional(propagation = Propagation.MANDATORY)
    override fun fetchSequenceValues(numberOfSequenceValues: Int): List<Long> {
        sequenceRepository.incrementBy(numberOfSequenceValues.toLong())
        val lastSequenceNumber = sequenceRepository.getLastUsedSeq()
        val firstSequenceNumber = lastSequenceNumber - numberOfSequenceValues + 1
        return (firstSequenceNumber..lastSequenceNumber).toList()
    }
}
