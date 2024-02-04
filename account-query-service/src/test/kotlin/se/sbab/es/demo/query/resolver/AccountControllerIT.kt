package se.sbab.es.demo.query.resolver

import com.fasterxml.jackson.databind.JsonNode
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.graphql.tester.AutoConfigureGraphQlTester
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationEventPublisher
import org.springframework.graphql.test.tester.GraphQlTester
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.repository.Account
import se.sbab.es.demo.query.repository.AccountRepository
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@AutoConfigureGraphQlTester
class AccountControllerIT {
    @Autowired
    lateinit var graphQlTester: GraphQlTester

    @Autowired
    lateinit var applicationEventPublisher: ApplicationEventPublisher

    @Autowired
    lateinit var accountRepository: AccountRepository

    @BeforeEach
    fun init() {
        accountRepository.deleteAll()
    }

    @Test
    fun `resolve a non existing account should return value null for data account and no errors`() {
        val accountId = AccountId()
        val response = findAccount(accountId)
        response.errors().verify().path("\$.data.account").valueIsNull()
    }

    @Test
    fun `resolve a new empty account should return correct account with zero balance`() {
        val accountId = AccountId()
        applicationEventPublisher.publishEvent(AccountOpenedEvent(accountId))
        val account = findAccount(accountId).getAccount()
        assertEquals(accountId, account.accountId)
        assertEquals(0, account.balance)
    }

    @Test
    fun `resolve an account with money deposited should return correct account with correct balance`() {
        val accountId = AccountId()
        val amount = 10500
        applicationEventPublisher.publishEvent(AccountOpenedEvent(accountId))
        applicationEventPublisher.publishEvent(MoneyDepositedEvent(accountId, amount))
        val account = findAccount(accountId).getAccount()
        assertEquals(accountId, account.accountId)
        assertEquals(amount, account.balance)
    }

    @Test
    fun `resolve all account in an empty repository should return an empty result`() {
        findAccounts(0, 10).getAccounts().apply {
            get("accounts").apply {
                assertTrue(isArray)
                assertEquals(0, size())
            }
            get("page").apply {
                assertEquals(0, get("number").intValue())
                assertEquals(0, get("totalPages").intValue())
                assertFalse(get("hasNext").booleanValue())
                assertFalse(get("hasPrevious").booleanValue())
            }
        }
    }

    @Test
    fun `resolve 10 accounts with a page size of 10 should return one page with 10 items`() {
        val balance = 1000
        createAccounts(10, balance)
        findAccounts(0, 10).getAccounts().apply() {
            get("accounts").apply {
                assertTrue(isArray)
                assertEquals(10, size())
                forEach { account -> assertEquals(balance, account.get("balance").intValue()) }
            }
            get("page").apply {
                assertEquals(0, get("number").intValue())
                assertEquals(1, get("totalPages").intValue())
                assertFalse(get("hasNext").booleanValue())
                assertFalse(get("hasPrevious").booleanValue())
            }
        }
    }

    @Test
    fun `resolve 15 accounts with a page size of 9 should return two pages with 9 and 6 items`() {
        val balance = 100
        createAccounts(15, balance)

        findAccounts(0, 9).getAccounts().apply() {
            get("accounts").apply {
                assertTrue(isArray)
                assertEquals(9, size())
                forEach { account -> assertEquals(balance, account.get("balance").intValue()) }
            }
            get("page").apply {
                assertEquals(0, get("number").intValue())
                assertEquals(2, get("totalPages").intValue())
                assertTrue(get("hasNext").booleanValue())
                assertFalse(get("hasPrevious").booleanValue())
            }
        }

        findAccounts(1, 9).getAccounts().apply() {
            get("accounts").apply {
                assertTrue(isArray)
                assertEquals(6, size())
                forEach { account -> assertEquals(balance, account.get("balance").intValue()) }
            }
            get("page").apply {
                assertEquals(1, get("number").intValue())
                assertEquals(2, get("totalPages").intValue())
                assertFalse(get("hasNext").booleanValue())
                assertTrue(get("hasPrevious").booleanValue())
            }
        }
    }

    private fun findAccount(accountId: AccountId): GraphQlTester.Response =
        graphQlTester.documentName("account").variable("accountId", accountId).execute()

    private fun findAccounts(page: Int, size: Int): GraphQlTester.Response =
        graphQlTester.documentName("accounts").variable("page", page).variable("size", size).execute()

    private fun GraphQlTester.Response.getAccount(): Account =
        errors().verify().path("\$.data.account").entity(Account::class.java).get()

    private fun GraphQlTester.Response.getAccounts(): JsonNode =
        errors().verify().path("\$.data.accounts").entity(JsonNode::class.java).get()

    private fun createAccounts(noOfAccounts: Int, balance: Int): List<AccountId> =
        (1..noOfAccounts).map { createAccount(balance) }

    private fun createAccount(balance: Int): AccountId {
        val accountId = AccountId()
        applicationEventPublisher.publishEvent(AccountOpenedEvent(accountId))
        applicationEventPublisher.publishEvent(MoneyDepositedEvent(accountId, balance))
        return accountId
    }
}