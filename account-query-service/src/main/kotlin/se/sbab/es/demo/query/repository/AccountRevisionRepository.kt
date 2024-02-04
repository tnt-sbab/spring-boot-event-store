package se.sbab.es.demo.query.repository

import org.springframework.data.repository.CrudRepository
import se.sbab.demo.es.AccountId

interface AccountRevisionRepository : CrudRepository<AccountRevision, AccountId> {
    fun findByAccountId(accountId: AccountId): AccountRevision?
}