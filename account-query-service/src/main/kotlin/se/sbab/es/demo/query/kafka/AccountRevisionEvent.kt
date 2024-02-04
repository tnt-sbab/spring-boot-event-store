package se.sbab.es.demo.query.kafka

import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.repository.AccountRevision

data class AccountRevisionEvent(
    val accountId: AccountId,
    val revision: Revision
) {
    fun toEntity(): AccountRevision = AccountRevision(
        accountId = accountId,
        revision = revision,
        outOfOrder = revision.value != 1
    )

    fun toEntity(entity: AccountRevision) = entity.copy(
        revision = revision,
        outOfOrder = entity.outOfOrder || revision != entity.revision.next()
    )
}
