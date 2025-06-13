package se.sbab.es.demo.query

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@SpringBootApplication
class AccountQueryServiceApplication

fun main(args: Array<String>) {
	runApplication<AccountQueryServiceApplication>(*args)
}

@Component
@Profile("dev")
class UrlApplicationRunner(
	@Value("\${server.port:8080}") private val port: String
) : ApplicationRunner {
	private val logger = LoggerFactory.getLogger(javaClass)

	override fun run(args: ApplicationArguments?) {
		logger.info("H2 URL: http://localhost:$port/account-query-service/h2-console")
		logger.info("Playground: http://localhost:$port/account-query-service/playground")
		logger.info("Voyager: http://localhost:$port/account-query-service/voyager")
		logger.info("GraphiQL: http://localhost:$port/account-query-service/graphiql")
	}
}
