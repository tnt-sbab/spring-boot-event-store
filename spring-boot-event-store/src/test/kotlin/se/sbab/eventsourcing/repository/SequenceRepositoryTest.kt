package se.sbab.eventsourcing.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension

@ExtendWith(SpringExtension::class)
@DataJpaTest
class SequenceRepositoryTest {
    @Autowired
    lateinit var sequenceRepository: SequenceRepository

    private final val LAST_USED_SEQ = 1000L

    @BeforeEach
    fun init() {
        sequenceRepository.save(SequenceEntity("EVENTID", LAST_USED_SEQ))
    }

    @Test
    fun `get an increasing number of sequences from the sequence repository`() {
        var lastUsedSeq = LAST_USED_SEQ
        for (numOfSequenceValuesToFetch in 1..10) {
            sequenceRepository.incrementBy(numOfSequenceValuesToFetch.toLong())
            val myLastId = sequenceRepository.getLastUsedSeq()
            assertEquals(lastUsedSeq + numOfSequenceValuesToFetch, myLastId)
            lastUsedSeq += numOfSequenceValuesToFetch
        }
    }
}
