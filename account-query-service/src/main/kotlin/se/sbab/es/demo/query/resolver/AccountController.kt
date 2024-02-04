package se.sbab.es.demo.query.resolver

import org.springframework.data.domain.PageRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.repository.Account
import se.sbab.es.demo.query.repository.AccountRepository

@Controller
class AccountController(private val accountRepository: AccountRepository) {
    @QueryMapping
    fun account(@Argument accountId: AccountId): Account? =
        accountRepository.findByAccountId(accountId)

    @QueryMapping
    fun accounts(@Argument page: Int, @Argument size: Int): SearchResult {
        val result = accountRepository.findAll(PageRequest.of(page, size))
        return SearchResult(result.content, result)
    }
}