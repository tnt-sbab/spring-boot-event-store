package se.sbab.es.demo.app.one.resource

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.app.one.command.DepositMoneyCommand

data class DepositMoneyRequest(
    @field:Min(value = 1, message = "Amount to deposit must be greater than zero")
    @Schema(description = "Amount to deposit", example = "100", required = true) val amount: Int
) {
    fun toCommand(accountId: AccountId) = DepositMoneyCommand(accountId, amount)
}
