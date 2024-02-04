package se.sbab.es.demo.bff.filter.factory

import org.springframework.cloud.gateway.filter.GatewayFilter
import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory
import org.springframework.cloud.gateway.support.GatewayToStringStyler.filterToStringCreator
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import se.sbab.es.demo.bff.filter.factory.AddPollUrlResponseHeaderGatewayFilterFactory.Config

@Component
class AddPollUrlResponseHeaderGatewayFilterFactory : AbstractGatewayFilterFactory<Config>(Config::class.java) {
    override fun apply(config: Config): GatewayFilter {
        return object : GatewayFilter {
            override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
                return chain.filter(exchange).then(Mono.fromRunnable {
                    this@AddPollUrlResponseHeaderGatewayFilterFactory.addPollUrl(exchange, config)
                })
            }

            override fun toString(): String = filterToStringCreator(this@AddPollUrlResponseHeaderGatewayFilterFactory)
                .append("url", config.url)
                .toString()
        }
    }

    private fun addPollUrl(exchange: ServerWebExchange, config: Config) {
        val aggregateId = exchange.response.headers.getFirst("aggregate-id")
        val revision = exchange.response.headers.getFirst("revision")
        if (aggregateId != null && revision != null) {
            val url = config.url.replace("\$aggregateId", aggregateId).replace("\$revision", revision)
            exchange.response.headers.add("poll-url", url)
        }
    }
    data class Config(
        val url: String
    )

    override fun shortcutFieldOrder(): List<String> = listOf("url")
}