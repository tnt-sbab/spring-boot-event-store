package se.sbab.es.demo.app.one

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import se.sbab.eventsourcing.EventsourcingConfiguration

@SpringBootApplication
@Import(EventsourcingConfiguration::class)
class AccountCommandServiceApplication

fun main(args: Array<String>) {
    runApplication<AccountCommandServiceApplication>(*args)
}
