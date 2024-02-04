package se.sbab.es.demo.query.projection

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.repository.AccountRepository
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class AccountProjectionServiceIT {
    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Autowired
    lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun init() {
        accountRepository.deleteAll()
    }

    @Test
    fun `AccountOpenedEvent should add the account to accountRepository with balance zero`() {
        val accountId = AccountId()
        applicationEventPublisher.publishEvent(AccountOpenedEvent(accountId))
        val account = accountRepository.findByAccountId(accountId)!!
        assertEquals(accountId, account.accountId)
        assertEquals(0, account.balance)
    }

    @Test
    fun `MoneyDepositedEvent should add the amount to the balance`() {
        val accountId = AccountId()
        val amount = 1234
        applicationEventPublisher.publishEvent(AccountOpenedEvent(accountId))
        applicationEventPublisher.publishEvent(MoneyDepositedEvent(accountId, amount))
        val account = accountRepository.findByAccountId(accountId)!!
        assertEquals(accountId, account.accountId)
        assertEquals(amount, account.balance)
    }

    @Test
    fun `MoneyWithdrawnEvent should reduce the amount to the balance`() {
        val accountId = AccountId()
        val depositAmount = 5000
        val withdrawAmount = 1000
        applicationEventPublisher.publishEvent(AccountOpenedEvent(accountId))
        applicationEventPublisher.publishEvent(MoneyDepositedEvent(accountId, depositAmount))
        applicationEventPublisher.publishEvent(MoneyWithdrawnEvent(accountId, withdrawAmount))
        val account = accountRepository.findByAccountId(accountId)!!
        assertEquals(accountId, account.accountId)
        assertEquals(4000, account.balance)
    }

    @Test
    fun `MoneyDepositedEvent on a non existing account should throw an exception`() {
        val accountId = AccountId()
        val exception = assertThrows(IllegalStateException::class.java) {
            applicationEventPublisher.publishEvent(MoneyDepositedEvent(accountId, 5000))
        }
        assertEquals("Account with id=$accountId not found", exception.message)
    }

    @Test
    fun `MoneyWithdrawnEvent on a non existing account should throw an exception`() {
        val accountId = AccountId()
        val exception = assertThrows(IllegalStateException::class.java) {
            applicationEventPublisher.publishEvent(MoneyWithdrawnEvent(accountId, 5000))
        }
        assertEquals("Account with id=$accountId not found", exception.message)
    }
}