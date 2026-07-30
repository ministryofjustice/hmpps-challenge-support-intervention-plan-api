package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_PRISONER_CASE_NOTES_RO
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SuggestedCaseNotesResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.SuggestedCaseNotesRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CaseNotesService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "10. Suggested Case Notes Controller", description = "Endpoints for suggested case notes")
class SuggestedCaseNotesController(
  private val caseNotesService: CaseNotesService,
  @Value($$"${feature.suggested-case-notes}")
  private val suggestedCaseNotesEnabled: Boolean,
) {
  @Operation(
    summary = "Retrieve suggested case notes for a prisoner",
    description = "Returns suggested case notes for the given prisoner and behaviour type.",
  )
  @ApiResponses(
    value = [
      ApiResponse(responseCode = "200", description = "Suggested case notes returned"),
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
  @PostMapping("/v1/suggestedCaseNotes/{prisonerNumber}")
  @PreAuthorize("hasAuthority('$ROLE_PRISONER_CASE_NOTES_RO')")
  fun suggestedCaseNotes(
    @PathVariable prisonerNumber: String,
    @RequestBody request: SuggestedCaseNotesRequest,
  ): SuggestedCaseNotesResponse {
    if (!suggestedCaseNotesEnabled) {
      throw ResponseStatusException(HttpStatus.METHOD_NOT_ALLOWED, "suggestedCaseNotes feature is not enabled")
    }

    return caseNotesService.buildSuggestedCaseNotes(prisonerNumber, request)
  }
}
