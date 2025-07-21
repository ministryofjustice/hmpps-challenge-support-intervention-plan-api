package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import io.swagger.v3.oas.annotations.Operation
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.ReconciliationInitiator

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
class ReconciliationController(private val reconciliationInitiator: ReconciliationInitiator) {
  @Operation(hidden = true)
  @PostMapping("/reconciliation/person")
  fun personReconciliation() {
    reconciliationInitiator.initiatePersonReconciliation()
  }
}
