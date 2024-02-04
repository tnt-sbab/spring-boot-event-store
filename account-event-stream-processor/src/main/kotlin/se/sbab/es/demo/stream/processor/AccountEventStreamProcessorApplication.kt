package se.sbab.es.demo.stream.processor

import org.apache.kafka.streams.kstream.KStream
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import java.util.function.Function

@SpringBootApplication
class AccountEventStreamProcessorApplication {
	@Bean
	fun accountQueryEventStream(): Function<KStream<String, Event>, KStream<String, Event>> =
		Function { accountEvents -> accountEvents }
}

fun main(args: Array<String>) {
	runApplication<AccountEventStreamProcessorApplication>(*args)
}
