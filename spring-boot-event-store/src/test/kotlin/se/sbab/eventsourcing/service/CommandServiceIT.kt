package se.sbab.eventsourcing.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.Commit
import org.springframework.transaction.annotation.Transactional
import se.sbab.demo.es.AccountId
import se.sbab.eventsourcing.command.DepositMoneyCommand
import se.sbab.eventsourcing.command.OpenAccountCommand
import se.sbab.eventsourcing.command.WithdrawMoneyCommand
import se.sbab.eventsourcing.commandhandler.DepositMoneyCommandHandler
import se.sbab.eventsourcing.commandhandler.OpenAccountCommandHandler
import se.sbab.eventsourcing.commandhandler.WithdrawMoneyCommandHandler
import se.sbab.eventsourcing.domain.Account
import se.sbab.eventsourcing.domain.AccountStatus
import se.sbab.eventsourcing.repository.DomainStateRepository
import se.sbab.eventsourcing.repository.EventRepository
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest(
    properties = [
        "spring.kafka.properties.schema.registry.url=mock://localhost:8081", // mock:// prefix creates a mocked schema registry
        "spring.kafka.properties.auto.register.schemas=true",
        "spring.jpa.properties.hibernate.jdbc.batch_size=20",
        "events-payload-topic=account-events",
        "logging.level.org.hibernate.engine.jdbc.spi.SqlExceptionHelper=OFF",
        "logging.level.org.hibernate.engine.jdbc.batch.internal.AbstractBatchImpl=WARN",
    ],
)
class CommandServiceIT {
    @Autowired
    lateinit var eventRepository: EventRepository

    @Autowired
    lateinit var domainStateRepository: DomainStateRepository

    @Autowired
    lateinit var accountCommandService: CommandService<Account>

    @BeforeEach
    fun init() {
        eventRepository.deleteAll()
    }

    @Test
    fun `Open account command should result in correct account state`() {
        val accountId = AccountId()
        val command = OpenAccountCommand(accountId)
        accountCommandService.apply(accountId) { state -> OpenAccountCommandHandler(state).handle(command) }
        val accountState = domainStateRepository.getById(accountId) as Account
        assertEquals(Account(accountId, 0, AccountStatus.ACTIVE), accountState)
    }

    @Test
    fun `Open account followed by a deposit and a withdraw should result in correct account state`() {
        val accountId = AccountId()
        accountCommandService.apply(accountId) { state ->
            OpenAccountCommandHandler(state).handle(OpenAccountCommand(accountId))
        }
        accountCommandService.apply(accountId) { state ->
            DepositMoneyCommandHandler(state).handle(DepositMoneyCommand(accountId, 1000))
        }
        accountCommandService.apply(accountId) { state ->
            WithdrawMoneyCommandHandler(state).handle(WithdrawMoneyCommand(accountId, 100))
        }
        val accountState = domainStateRepository.getById(accountId) as Account
        assertEquals(Account(accountId, 1000 - 100, AccountStatus.ACTIVE), accountState)
    }

    @Test
    @Transactional
    @Commit
    fun `Concurrent commands should result in retries and correct account state`() {
        val numberOfConcurrentDeposits = 20
        val depositAmount = 37
        val accountId = AccountId()
        accountCommandService.apply(accountId) { state ->
            OpenAccountCommandHandler(state).handle(OpenAccountCommand(accountId))
        }
        val executorService = Executors.newFixedThreadPool(10)
        repeat(numberOfConcurrentDeposits) {
            executorService.execute(AsyncDepositMoneyCommandHandler(accountCommandService, accountId, depositAmount))
        }
        executorService.shutdown()
        executorService.awaitTermination(5, TimeUnit.SECONDS)
        val accountState = domainStateRepository.getById(accountId) as Account
        assertEquals(Account(accountId, numberOfConcurrentDeposits * depositAmount, AccountStatus.ACTIVE), accountState)
    }

    class AsyncDepositMoneyCommandHandler(
        private val accountCommandService: CommandService<Account>,
        private val accountId: AccountId,
        private val amount: Int,
    ) : Runnable {
        override fun run() {
            accountCommandService.apply(accountId) { state ->
                DepositMoneyCommandHandler(state).handle(DepositMoneyCommand(accountId, amount))
            }
        }
    }
}
