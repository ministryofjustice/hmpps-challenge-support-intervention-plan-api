package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipSearchResults
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.FindCsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.CsipSearchService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping
@Tag(name = "8. CSIP Admin Controller", description = "Endpoints for admin features such as search and reporting")
class CsipSearchController(private val search: CsipSearchService) {
  @Operation(
    summary = "Search and filter all CSIP records.",
    description = "Returns the CSIP records matching search query and filters",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP records found",
      ),
      ApiResponse(
        responseCode = "400",
        description = "Bad Request",
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
  @ResponseStatus(HttpStatus.OK)
  @GetMapping("/search/csip-records")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun findCsipRecords(@Valid @ParameterObject request: FindCsipRequest): CsipSearchResults =
    search.findMatchingCsipRecords(request)
}
