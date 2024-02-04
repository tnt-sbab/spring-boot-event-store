package se.sbab.es.demo.app.one

import org.slf4j.LoggerFactory
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import se.sbab.common.logging.EnableCustomRequestLogFilter
import se.sbab.eventsourcing.EventsourcingConfiguration

@SpringBootApplication
@Import(EventsourcingConfiguration::class)
@EnableCustomRequestLogFilter
class AccountCommandServiceApplication

fun main(args: Array<String>) {
    runApplication<AccountCommandServiceApplication>(*args)
}

@Component
@Profile("dev")
class UrlApplicationRunner(
    private val environment: Environment
) : ApplicationRunner {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun run(args: ApplicationArguments?) {
        val port = environment.getProperty("server.port")
        logger.info("Swagger URL: http://localhost:$port/account-command-service/swagger-ui.html")
        logger.info("H2 URL: http://localhost:$port/account-command-service/h2-console")
    }
}
