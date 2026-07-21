package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesFilterParams
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNotesService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@Tag(name = "Case Notes Controller", description = "Endpoints for case notes operations")
class CaseNotesController(
  private val caseNotesService: CaseNotesService,
) {
  @Operation(summary = "Initiate case notes retrieval for an offender")
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "202", description = "Case notes retrieval initiated"),
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
  @PostMapping("/initiate-case-notes-retrieval")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun getCaseNotes(
    params: CaseNotesFilterParams,
    @Valid @RequestBody requestBody: CaseNotesLookupRequest,
  ): ResponseEntity<Unit> {
    caseNotesService.getCaseNotes(requestBody, params)
    return ResponseEntity.accepted().build()
  }
}
