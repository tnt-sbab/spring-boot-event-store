package se.sbab.es.demo.query.repository

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.data.domain.PageRequest
import se.sbab.demo.es.AccountId
import java.time.OffsetDateTime

@DataJpaTest
class AccountRepositoryIT {
    @Autowired
    lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun init() {
        accountRepository.deleteAll()
    }

    @Test
    fun `save and find an account should return the account`() {
        val account = createAndSaveAccount(1000)
        val accountId = account.accountId
        val result = accountRepository.findByAccountId(accountId)!!
        assertEquals(accountId, result.accountId)
        assertEquals(1000, result.balance)
    }

    @Test
    fun `find a non existing account should return null`() {
        val accountId = AccountId()
        val result = accountRepository.findByAccountId(accountId)
        assertNull(result)
    }

    @Test
    fun `save and find 10 accounts with a page size of 10 should return one page with 10 items`() {
        createAndSaveAccounts(10, 1000)
        val page = 0
        val size = 10
        val result = accountRepository.findAll(PageRequest.of(page, size))
        assertEquals(10, result.content.size)
        assertEquals(0, result.number)
        assertEquals(1, result.totalPages)
        assertFalse(result.hasNext())
        assertFalse(result.hasPrevious())
    }

    @Test
    fun `find all using an empty repository should return an empty result with zero pages`() {
        val result = accountRepository.findAll(PageRequest.of(0, 10))
        assertEquals(0, result.content.size)
        assertEquals(0, result.number)
        assertEquals(0, result.totalPages)
        assertFalse(result.hasNext())
        assertFalse(result.hasPrevious())
    }

    @Test
    fun `save and find 15 accounts with a page size of 10 should return two pages with 10 and 5 items`() {
        createAndSaveAccounts(15, 100)
        val size = 10
        val result = accountRepository.findAll(PageRequest.of(0, size))
        assertEquals(10, result.content.size)
        assertEquals(0, result.number)
        assertEquals(2, result.totalPages)
        assertTrue(result.hasNext())
        assertFalse(result.hasPrevious())
        val result2 = accountRepository.findAll(PageRequest.of(1, size))
        assertEquals(5, result2.content.size)
        assertEquals(1, result2.number)
        assertEquals(2, result2.totalPages)
        assertFalse(result2.hasNext())
        assertTrue(result2.hasPrevious())
    }

    private fun createAndSaveAccounts(noOfAccounts: Int, balance: Int): List<Account> =
        (1..noOfAccounts).map { createAndSaveAccount(balance) }

    private fun createAndSaveAccount(balance: Int): Account {
        val accountId = AccountId()
        val now = OffsetDateTime.now()
        val account = Account(accountId, now, now, balance)
        accountRepository.save(account)
        return account
    }
}