package se.sbab.eventsourcing.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.junit.jupiter.SpringExtension
import se.sbab.eventsourcing.repository.SequenceEntity
import se.sbab.eventsourcing.repository.SequenceRepository

@ExtendWith(SpringExtension::class)
@Import(SequenceRepositoryServiceImpl::class)
@DataJpaTest
class SequenceRepositoryServiceTest {
    @Autowired
    lateinit var sequenceRepositoryService: SequenceRepositoryServiceImpl

    @Autowired
    lateinit var sequenceRepository: SequenceRepository

    private final val LAST_USED_SEQ = 5000L

    @BeforeEach
    fun init() {
        sequenceRepository.save(SequenceEntity("EVENTID", LAST_USED_SEQ))
    }

    @Test
    fun `get an increasing number of sequences from the sequence service`() {
        var lastUsedSeq = LAST_USED_SEQ
        for (numOfSequenceValuesToFetch in 1..10) {
            val range = sequenceRepositoryService.fetchSequenceValues(numOfSequenceValuesToFetch)
            val expectedRange = (lastUsedSeq + 1)..(lastUsedSeq + numOfSequenceValuesToFetch)
            assertEquals(expectedRange.toList(), range)
            lastUsedSeq += numOfSequenceValuesToFetch
        }
    }
}
