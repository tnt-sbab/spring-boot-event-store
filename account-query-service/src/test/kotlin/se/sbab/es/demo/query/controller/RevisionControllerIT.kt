package se.sbab.es.demo.query.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.kafka.EventConsumer
import se.sbab.es.demo.query.kafka.Revision
import se.sbab.event.AccountOpenedEvent
import se.sbab.event.MoneyDepositedEvent
import se.sbab.event.MoneyWithdrawnEvent

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class RevisionControllerIT {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var eventConsumer: EventConsumer

    @Test
    fun `get revision for non existing account should return status 204 NO CONTENT`() {
        val accountId = AccountId()
        val revision = 1
        mockMvc.perform(get("/accounts/$accountId/revision/$revision"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `get revision for an existing account and revision number should return status 200 OK`() {
        val accountId = AccountId()
        val revision = Revision(1)
        eventConsumer.publishEvent(AccountOpenedEvent(accountId), accountId, revision)
        mockMvc.perform(get("/accounts/$accountId/revision/$revision"))
            .andExpect(status().isOk)
    }

    @Test
    fun `get revision with a higher revision number for an existing account should return status 204 NO CONTENT`() {
        val accountId = AccountId()
        val revision = Revision(2)
        eventConsumer.publishEvent(AccountOpenedEvent(accountId), accountId, Revision(1))
        mockMvc.perform(get("/accounts/$accountId/revision/$revision"))
            .andExpect(status().isNoContent)
    }

    @Test
    fun `poll for revision with should return status 200 OK once the revision is greater than or equal to the requested revision number`() {
        val accountId = AccountId()
        val revision = Revision(3)
        eventConsumer.publishEvent(AccountOpenedEvent(accountId), accountId, Revision(1))
        mockMvc.perform(get("/accounts/$accountId/revision/$revision"))
            .andExpect(status().isNoContent)
        eventConsumer.publishEvent(MoneyDepositedEvent(accountId, 1000), accountId, Revision(2))
        mockMvc.perform(get("/accounts/$accountId/revision/$revision"))
            .andExpect(status().isNoContent)
        eventConsumer.publishEvent(MoneyWithdrawnEvent(accountId, 500), accountId, Revision(3))
        mockMvc.perform(get("/accounts/$accountId/revision/$revision"))
            .andExpect(status().isOk)
    }
}
