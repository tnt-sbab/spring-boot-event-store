package se.sbab.es.demo.bff

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AccountBffApplicationIT(@LocalServerPort port: Int) {
    val client = WebTestClient.bindToServer().baseUrl("http://localhost:$port").build()

    @Test
    fun `verify cortex-bff healthcheck response`() {
        client.get().uri("/account-bff/admin/healthcheck").exchange()
            .expectStatus().isOk
            .expectBody<String>()
            .consumeWith { result -> Assertions.assertThat(result.responseBody).contains("""status":"UP""") }
    }
}
