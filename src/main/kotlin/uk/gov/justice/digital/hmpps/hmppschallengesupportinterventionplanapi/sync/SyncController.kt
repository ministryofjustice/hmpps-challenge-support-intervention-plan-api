package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.RequestContextHolder
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.internal.SyncCsipRecord
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(path = ["/sync/csip-records"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "Sync CSIP Record Controller", description = "Endpoint for sync operations")
class SyncController(private val csip: SyncCsipRecord) {
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP record merged",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad request",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PutMapping
  @ResponseStatus(HttpStatus.OK)
  @PreAuthorize("hasAnyRole('$ROLE_NOMIS')")
  fun syncCsipRecord(@Valid @RequestBody request: SyncCsipRequest): SyncResponse {
    setSyncContext(request)
    return csip.sync(request)
  }

  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP record merged",
      ),
      ApiResponse(
        responseCode = "401",
        description = "Unauthorised, requires a valid Oauth2 token",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
      ApiResponse(
        responseCode = "403",
        description = "Forbidden, requires an appropriate role",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PreAuthorize("hasAnyRole('$ROLE_NOMIS')")
  fun deleteCsipRecord(@PathVariable id: UUID, @RequestBody request: DefaultLegacyActioned) {
    setSyncContext(request)
    csip.deleteCsipRecord(id)
  }

  private fun setSyncContext(actioned: LegacyActioned) {
    val contextName = CsipRequestContext::class.simpleName!!
    verifyDoesNotExist(RequestContextHolder.getRequestAttributes()?.getAttribute(contextName, 0)) {
      IllegalStateException("Context should not be set")
    }
    RequestContextHolder.getRequestAttributes()!!
      .setAttribute(
        contextName,
        CsipRequestContext(
          source = Source.NOMIS,
          requestAt = actioned.actionedAt,
          username = actioned.actionedBy,
          userDisplayName = actioned.actionedByDisplayName,
          activeCaseLoadId = actioned.activeCaseloadId,
        ),
        0,
      )
  }
}
