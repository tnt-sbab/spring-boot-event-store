package se.sbab.es.demo.bff

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AccountBffApplication

fun main(args: Array<String>) {
	runApplication<AccountBffApplication>(*args)
}
