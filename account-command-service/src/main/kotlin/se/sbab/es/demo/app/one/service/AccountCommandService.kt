package se.sbab.es.demo.app.one.service

import org.springframework.stereotype.Service
import se.sbab.eventsourcing.Event
import se.sbab.eventsourcing.service.CommandService
import se.sbab.es.demo.app.one.aggregate.Account
import se.sbab.es.demo.app.one.command.DepositMoneyCommand
import se.sbab.es.demo.app.one.command.OpenAccountCommand
import se.sbab.es.demo.app.one.command.WithdrawMoneyCommand
import se.sbab.es.demo.app.one.commandhandler.DepositMoneyCommandHandler
import se.sbab.es.demo.app.one.commandhandler.OpenAccountCommandHandler
import se.sbab.es.demo.app.one.commandhandler.WithdrawMoneyCommandHandler

@Service
class AccountCommandService(private val commandService: CommandService<Account>) {
    fun apply(command: OpenAccountCommand): Event =
        commandService.apply(command.accountId) { account -> OpenAccountCommandHandler(account).handle(command) }

    fun apply(command: DepositMoneyCommand): Event =
        commandService.apply(command.accountId) { account -> DepositMoneyCommandHandler(account).handle(command) }

    fun apply(command: WithdrawMoneyCommand): Event =
        commandService.apply(command.accountId) { account -> WithdrawMoneyCommandHandler(account).handle(command) }
}
