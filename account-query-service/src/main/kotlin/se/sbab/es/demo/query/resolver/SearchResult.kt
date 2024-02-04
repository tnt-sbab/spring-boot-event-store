package se.sbab.es.demo.query.resolver

import org.springframework.data.domain.Page
import se.sbab.es.demo.query.repository.Account

data class SearchResult(
    val accounts: List<Account>,
    val page: Page<Account>
)