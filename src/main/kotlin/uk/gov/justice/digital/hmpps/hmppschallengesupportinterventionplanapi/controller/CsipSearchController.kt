package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springdoc.core.annotations.ParameterObject
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipOverview
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
  @GetMapping("/search/csip-records")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun findCsipRecords(@Valid @ParameterObject request: FindCsipRequest): CsipSearchResults = search.findMatchingCsipRecords(request)

  @Operation(summary = "Retrieve an overview of CSIP records for a given prison")
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Successful request returning CSIP overview",
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
  @GetMapping("/prisons/{prisonCode}/csip-records/overview")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun getCsipOverview(@PathVariable prisonCode: String): CsipOverview = search.getOverviewForPrison(prisonCode)
}
