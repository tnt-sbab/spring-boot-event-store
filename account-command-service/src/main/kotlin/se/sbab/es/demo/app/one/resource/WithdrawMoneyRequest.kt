package se.sbab.es.demo.app.one.resource

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Min
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.app.one.command.WithdrawMoneyCommand

data class WithdrawMoneyRequest(
    @field:Min(value = 1, message = "Amount to withdraw must be greater than zero")
    @Schema(description = "Amount to withdraw", example = "100", required = true) val amount: Int
) {
    fun toCommand(accountId: AccountId) = WithdrawMoneyCommand(accountId, amount)
}
