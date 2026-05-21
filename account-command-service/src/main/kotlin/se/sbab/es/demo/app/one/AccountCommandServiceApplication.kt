package se.sbab.es.demo.app.one

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class AccountCommandServiceApplication

fun main(args: Array<String>) {
    runApplication<AccountCommandServiceApplication>(*args)
}
