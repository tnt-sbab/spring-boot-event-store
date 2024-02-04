package se.sbab.es.demo.app.one.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.app.one.command.OpenAccountCommand
import se.sbab.es.demo.app.one.service.AccountCommandService

@RestController
@RequestMapping("accounts")
class AccountController(private val accountCommandService: AccountCommandService) {

    @Operation(summary = "Open an account")
    @PostMapping
    fun openAccount(): AccountId {
        val command = OpenAccountCommand(AccountId())
        accountCommandService.apply(command)
        return command.accountId
    }

    @Operation(summary = "Deposit money to an account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{accountId}/deposit")
    fun depositMoneyToAccount(
        @Schema(example = "b4e37836-049b-40d3-b872-330d863fc2b9", required = true)
        @PathVariable("accountId") accountId: AccountId,
        @RequestBody request: DepositMoneyRequest
    ) {
        val command = request.toCommand(accountId)
        accountCommandService.apply(command)
    }

    @Operation(summary = "Withdraw money from an account")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PutMapping("/{accountId}/withdraw")
    fun withdrawMoneyFromAccount(
        @Schema(example = "b4e37836-049b-40d3-b872-330d863fc2b9", required = true)
        @PathVariable("accountId") accountId: AccountId,
        @RequestBody request: WithdrawMoneyRequest
    ) {
        val command = request.toCommand(accountId)
        accountCommandService.apply(command)
    }
}
