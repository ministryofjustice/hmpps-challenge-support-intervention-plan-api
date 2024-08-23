package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.internal.SyncCsipRecord

@RestController
@RequestMapping(path = ["/sync/csip-records"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Sync CSIP Record Controller", description = "Endpoint for sync operations")
class SyncController(private val csip: SyncCsipRecord) {
  @PutMapping
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('$ROLE_NOMIS')")
  fun syncCsipRecord(@Valid @RequestBody request: SyncCsipRequest): SyncResponse {
    val contextName = CsipRequestContext::class.simpleName!!
    verifyDoesNotExist(RequestContextHolder.getRequestAttributes()?.getAttribute(contextName, 0)) {
      IllegalStateException("Context should not be set")
    }
    RequestContextHolder.getRequestAttributes()!!
      .setAttribute(
        contextName,
        csipRequestContext().copy(source = Source.NOMIS, activeCaseLoadId = request.activeCaseloadId),
        0,
      )
    return csip.sync(request)
  }
}
