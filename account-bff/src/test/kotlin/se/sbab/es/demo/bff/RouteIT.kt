package se.sbab.es.demo.bff

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.post
import com.github.tomakehurst.wiremock.client.WireMock.put
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock
import org.springframework.http.HttpStatus
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
@TestPropertySource(
    properties = [
        "integration.account-command-service.url=http://localhost:\${wiremock.server.port}",
        "integration.account-query-service.url=http://localhost:\${wiremock.server.port}"
    ],
)

class RouteIT(@LocalServerPort port: Int) {
    val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @BeforeEach
    fun before() {
        WireMock.resetToDefault()
    }

    @Test
    fun `verify put commands are routed to account-command-service`() {
        val accountId = UUID.randomUUID().toString()
        val revision = "15"
        stubFor(
            put("/account-command-service/accounts/$accountId/deposit")
                .willReturn(
                    aResponse().withStatus(HttpStatus.NO_CONTENT.value())
                        .withHeader("aggregate-id", accountId)
                        .withHeader("revision", revision)
                )
        )
        client.put().uri("/account-bff/api/v1/command/accounts/$accountId/deposit").exchange()
            .expectStatus().isNoContent
            .expectHeader().valueEquals("aggregate-id", accountId)
            .expectHeader().valueEquals("revision", revision)
            .expectHeader().valueEquals("poll-url", "/api/v1/accounts/$accountId/revision/$revision")
    }

    @Test
    fun `verify post commands are routed to account-command-service`() {
        val accountId = UUID.randomUUID().toString()
        val revision = "1"
        stubFor(
            post("/account-command-service/accounts")
                .willReturn(
                    aResponse().withStatus(HttpStatus.NO_CONTENT.value())
                        .withHeader("aggregate-id", accountId)
                        .withHeader("revision", revision)
                )
        )
        client.post().uri("/account-bff/api/v1/command/accounts").exchange()
            .expectStatus().isNoContent
            .expectHeader().valueEquals("aggregate-id", accountId)
            .expectHeader().valueEquals("revision", revision)
            .expectHeader().valueEquals("poll-url", "/api/v1/accounts/$accountId/revision/$revision")
    }

    @Test
    fun `verify get events are routed to account-command-service`() {
        val responseBody = javaClass.getResource("/responses/events-response.json")!!.readText()
        val accountId = UUID.randomUUID().toString()
        stubFor(
            get("/account-command-service/events/$accountId")
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(responseBody)
                )
        )
        client.get().uri("/account-bff/api/v1/account-events/$accountId").exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo(responseBody)

    }

    @Test
    fun `verify GraphQL requests are routed to account-query-service`() {
        val responseBody = "GraphQL Response Body"
        stubFor(
            post("/account-query-service/graphql")
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatus.OK.value())
                        .withBody(responseBody),
                ),
        )
        client.post().uri("/account-bff/api/v1/graphql").exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .isEqualTo(responseBody)

    }

    @Test
    fun `verify revision requests for accounts are routed to account-query-service`() {
        val accountId = UUID.randomUUID().toString()
        val revision = "19"
        stubFor(
            get("/account-query-service/accounts/$accountId/revision/$revision")
                .willReturn(
                    aResponse().withStatus(HttpStatus.OK.value())
                )
        )
        client.get().uri("/account-bff/api/v1/accounts/$accountId/revision/$revision").exchange()
            .expectStatus().isOk

    }
}