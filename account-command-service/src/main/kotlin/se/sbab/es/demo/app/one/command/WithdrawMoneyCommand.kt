package se.sbab.es.demo.app.one.command

import se.sbab.demo.es.AccountId

data class WithdrawMoneyCommand(val accountId: AccountId, val withdrawAmount: Int)
