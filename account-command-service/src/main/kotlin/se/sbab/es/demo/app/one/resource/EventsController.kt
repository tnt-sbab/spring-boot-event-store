package se.sbab.es.demo.app.one.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import se.sbab.eventsourcing.service.EventDto
import se.sbab.eventsourcing.service.EventsService
import se.sbab.demo.es.AccountId

@RestController
@RequestMapping("events")
class EventsController(private val eventsService: EventsService) {
    @Operation(summary = "Get all account events")
    @GetMapping("/{accountId}")
    fun events(
        @Schema(example = "b4e37836-049b-40d3-b872-330d863fc2b9", required = true)
        @PathVariable("accountId") accountId: AccountId
    ): List<EventDto> = eventsService.getEvents(accountId)
}
