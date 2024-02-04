package se.sbab.es.demo.query.repository

import org.springframework.data.repository.ListCrudRepository
import org.springframework.data.repository.PagingAndSortingRepository
import se.sbab.demo.es.AccountId

interface AccountRepository : PagingAndSortingRepository<Account, AccountId>, ListCrudRepository<Account, AccountId> {
    fun findByAccountId(accountId: AccountId): Account?
}