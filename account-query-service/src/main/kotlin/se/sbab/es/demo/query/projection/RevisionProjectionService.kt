package se.sbab.es.demo.query.projection

import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import se.sbab.es.demo.query.kafka.AccountRevisionEvent
import se.sbab.es.demo.query.repository.AccountRevisionRepository

@Service
class RevisionProjectionService(
    private val accountRevisionRepository: AccountRevisionRepository
) {
    @EventListener
    fun on(accountRevisionEvent: AccountRevisionEvent) {
        accountRevisionRepository.findByAccountId(accountRevisionEvent.accountId)?.run {
            accountRevisionRepository.save(accountRevisionEvent.toEntity(this))
        } ?: accountRevisionRepository.save(accountRevisionEvent.toEntity())
    }
 }