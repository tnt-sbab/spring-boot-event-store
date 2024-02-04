package se.sbab.es.demo.app.one.command

import se.sbab.demo.es.AccountId

data class DepositMoneyCommand(val accountId: AccountId, val depositAmount: Int)
