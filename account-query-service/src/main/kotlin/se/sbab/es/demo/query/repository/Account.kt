package se.sbab.es.demo.query.repository

import jakarta.persistence.AttributeOverride
import jakarta.persistence.Column
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import se.sbab.demo.es.AccountId
import java.time.OffsetDateTime

@Entity(name = "ACCOUNT")
data class Account(
    @EmbeddedId
    @AttributeOverride( name = "id", column = Column(name = "ACCOUNT_ID"))
    val accountId: AccountId,

    @Column(nullable = false)
    val createdAt: OffsetDateTime,

    @Column(nullable = false)
    val updatedAt: OffsetDateTime,

    @Column(nullable = false)
    val balance: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Account) return false
        return accountId == other.accountId
    }

    override fun hashCode(): Int = accountId.hashCode()
}