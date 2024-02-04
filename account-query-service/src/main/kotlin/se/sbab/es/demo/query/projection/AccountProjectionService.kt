package se.sbab.es.demo.query.projection

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.repository.Account
import se.sbab.es.demo.query.repository.AccountRepository
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent
import java.time.OffsetDateTime

@Service
class AccountProjectionService(private val accountRepository: AccountRepository) {
    @EventListener
    fun on(event: AccountOpenedEvent) {
        val now = OffsetDateTime.now()
        val account = Account(event.accountId, now, now, 0)
        accountRepository.save(account)
    }

    @EventListener
    fun on(event: MoneyDepositedEvent) = project(event.accountId) { account ->
        account.copy(updatedAt = OffsetDateTime.now(), balance = account.balance + event.amount)
    }

    @EventListener
    fun on(event: MoneyWithdrawnEvent) = project(event.accountId) { account ->
        account.copy(updatedAt = OffsetDateTime.now(), balance = account.balance - event.amount)
    }

    fun project(accountId: AccountId, projectFunction: (Account) -> Account) =
        accountRepository.findByAccountId(accountId)?.run {
            accountRepository.save(projectFunction(this))
        } ?: throw IllegalStateException("Account with id=$accountId not found")
}