package se.sbab.demo.es

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import java.io.Serializable
import java.util.UUID
import java.util.function.Supplier

data class AccountId(
    @JsonValue val id: UUID = UUID.randomUUID()
): Supplier<UUID>, Serializable {
    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    constructor(id: String) : this(UUID.fromString(id))

    override fun get(): UUID = id

    override fun toString() = id.toString()
}