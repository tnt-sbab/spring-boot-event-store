package se.sbab.es.demo.query.repository

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.kafka.Revision
import se.sbab.es.demo.query.kafka.RevisionConverter

@Entity(name = "ACCOUNT_REVISION")
data class AccountRevision(
    @EmbeddedId
    @AttributeOverride( name = "id", column = Column(name = "ACCOUNT_ID"))
    val accountId: AccountId,

    @Column(nullable = false)
    @Convert(converter = RevisionConverter::class)
    val revision: Revision,

    @Column(nullable = false)
    val outOfOrder: Boolean
)
