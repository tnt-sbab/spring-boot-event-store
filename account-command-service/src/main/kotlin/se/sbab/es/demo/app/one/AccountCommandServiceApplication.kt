package se.sbab.es.demo.app.one

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import se.sbab.eventsourcing.EventsourcingConfiguration

@SpringBootApplication
@Import(EventsourcingConfiguration::class)
class AccountCommandServiceApplication

fun main(args: Array<String>) {
    runApplication<AccountCommandServiceApplication>(*args)
}

@Component
@Profile("dev")
class UrlApplicationRunner(
    @Value("\${server.port:8080}") private val port: String
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments) {
        logger.info("Swagger URL: http://localhost:$port/account-command-service/swagger-ui.html")
        logger.info("H2 URL: http://localhost:$port/account-command-service/h2-console")
    }
}
