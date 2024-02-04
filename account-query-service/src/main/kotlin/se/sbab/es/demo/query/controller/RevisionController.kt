package se.sbab.es.demo.query.controller

import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import se.sbab.demo.es.AccountId
import se.sbab.es.demo.query.kafka.Revision
import se.sbab.es.demo.query.repository.AccountRevisionRepository

@RestController
@RequestMapping("/accounts")
class RevisionController(
    private val accountRevisionRepository: AccountRevisionRepository
) {
    @GetMapping("/{accountId}/revision/{revision}")
    fun revisionStatus(
        @PathVariable("accountId") accountId: AccountId,
        @PathVariable("revision") revision: Revision
    ): ResponseEntity<Unit> {
        val currentRevision = accountRevisionRepository.findByIdOrNull(accountId)?.revision ?: Revision(0)
        return if (currentRevision >= revision) {
            ResponseEntity.ok().build()
        } else {
            ResponseEntity.noContent().build()
        }
    }
}