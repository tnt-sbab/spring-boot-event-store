package se.sbab.es.demo.query.repository

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.kafka.Revision

@DataJpaTest
class AccountRevisionRepositoryIT {
    @Autowired
    lateinit var accountRevisionRepository: AccountRevisionRepository

    @BeforeEach
    fun init() {
        accountRevisionRepository.deleteAll()
    }

    @Test
    fun `save and find an account revision should return the account revision`() {
        val accountId = AccountId()
        val revision = Revision(1)
        val accountRevision = AccountRevision(accountId, revision, false)
        accountRevisionRepository.save(accountRevision)
        val result = accountRevisionRepository.findByAccountId(accountId)
        assertEquals(accountId, result!!.accountId)
        assertEquals(revision, result.revision)
        Assertions.assertFalse(result.outOfOrder)
    }

    @Test
    fun `find a non existing account revision should return null`() {
        val accountId = AccountId()
        val result = accountRevisionRepository.findByAccountId(accountId)
        Assertions.assertNull(result)
    }
}
