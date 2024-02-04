package se.sbab.es.demo.query.projection

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.kafka.AccountRevisionEvent
import se.sbab.es.demo.query.kafka.Revision
import se.sbab.es.demo.query.repository.AccountRevisionRepository

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RevisionProjectionServiceIT {
    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Autowired
    lateinit var accountRevisionRepository: AccountRevisionRepository

    @BeforeEach
    fun init() {
        accountRevisionRepository.deleteAll()
    }

    @Test
    fun `initial AccountRevisionEvent with correct revision should be saved to the repository in order`() {
        val accountId = AccountId()
        val revision = Revision(1)
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, revision))
        val accountRevision = accountRevisionRepository.findByAccountId(accountId)!!
        assertEquals(accountId, accountRevision.accountId)
        assertEquals(revision, accountRevision.revision)
        assertFalse(accountRevision.outOfOrder)
    }

    @Test
    fun `initial AccountRevisionEvent with incorrect revision should be saved to the repository out of order`() {
        val accountId = AccountId()
        val revision = Revision(2)
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, revision))
        val accountRevision = accountRevisionRepository.findByAccountId(accountId)!!
        assertEquals(accountId, accountRevision.accountId)
        assertEquals(revision, accountRevision.revision)
        assertTrue(accountRevision.outOfOrder)
    }

    @Test
    fun `second AccountRevisionEvent with correct revision should be saved to the repository in order`() {
        val accountId = AccountId()
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, Revision(1)))
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, Revision(2)))
        val accountRevision = accountRevisionRepository.findByAccountId(accountId)!!
        assertEquals(accountId, accountRevision.accountId)
        assertEquals(Revision(2), accountRevision.revision)
        assertFalse(accountRevision.outOfOrder)
    }

    @Test
    fun `second AccountRevisionEvent with incorrect revision should be saved to the repository out order`() {
        val accountId = AccountId()
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, Revision(1)))
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, Revision(1)))
        val accountRevision = accountRevisionRepository.findByAccountId(accountId)!!
        assertEquals(accountId, accountRevision.accountId)
        assertEquals(Revision(1), accountRevision.revision)
        assertTrue(accountRevision.outOfOrder)
    }

    @Test
    fun `AccountRevisionEvent with correct revision after invalid revision should be saved to the repository out order`() {
        val accountId = AccountId()
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, Revision(1)))
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, Revision(1)))
        applicationEventPublisher.publishEvent(AccountRevisionEvent(accountId, Revision(2)))
        val accountRevision = accountRevisionRepository.findByAccountId(accountId)!!
        assertEquals(accountId, accountRevision.accountId)
        assertEquals(Revision(2), accountRevision.revision)
        assertTrue(accountRevision.outOfOrder)
    }
}