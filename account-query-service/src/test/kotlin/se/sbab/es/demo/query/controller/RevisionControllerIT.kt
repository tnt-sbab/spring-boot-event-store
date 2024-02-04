package se.sbab.es.demo.query.controller

import io.restassured.RestAssured
import io.restassured.response.ValidatableResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.http.HttpStatus
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.kafka.EventConsumer
import se.sbab.es.demo.query.kafka.Revision
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RevisionControllerIT {
    @LocalServerPort
    private val serverPort = 0

    @Autowired
    lateinit var eventConsumer: EventConsumer

    @Value("\${server.servlet.context-path}")
    lateinit var serverContextPath: String

    @BeforeEach
    fun before() {
        RestAssured.port = serverPort
        RestAssured.baseURI = "http://localhost$serverContextPath"
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails()
    }

    @Test
    fun `get revision for non existing account should return status 204 NO CONTENT`() {
        val accountId = AccountId()
        val revision = 1
        executeRequest("/accounts/$accountId/revision/$revision", HttpStatus.NO_CONTENT)
    }

    @Test
    fun `get revision for an existing account and revision number should return status 200 OK`() {
        val accountId = AccountId()
        val revision = Revision(1)
        eventConsumer.publishEvent(AccountOpenedEvent(accountId), accountId, revision)
        executeRequest("/accounts/$accountId/revision/$revision", HttpStatus.OK)
    }

    @Test
    fun `get revision with a higher revision number for an existing account should return status 204 NO CONTENT`() {
        val accountId = AccountId()
        val revision = Revision(2)
        eventConsumer.publishEvent(AccountOpenedEvent(accountId), accountId, Revision(1))
        executeRequest("/accounts/$accountId/revision/$revision", HttpStatus.NO_CONTENT)
    }

    @Test
    fun `poll for revision with should return status 200 OK once the revision is greater than or equal to the requested revision number`() {
        val accountId = AccountId()
        val revision = Revision(3)
        eventConsumer.publishEvent(AccountOpenedEvent(accountId), accountId, Revision(1))
        executeRequest("/accounts/$accountId/revision/$revision", HttpStatus.NO_CONTENT)
        eventConsumer.publishEvent(MoneyDepositedEvent(accountId, 1000), accountId, Revision(2))
        executeRequest("/accounts/$accountId/revision/$revision", HttpStatus.NO_CONTENT)
        eventConsumer.publishEvent(MoneyWithdrawnEvent(accountId, 500), accountId, Revision(3))
        executeRequest("/accounts/$accountId/revision/$revision", HttpStatus.OK)
    }

    fun executeRequest(
        url: String,
        expectedStatus: HttpStatus,
    ): ValidatableResponse =
        RestAssured.given()
            .`when`()
            .get(url)
            .then()
            .assertThat()
            .statusCode(expectedStatus.value())
}