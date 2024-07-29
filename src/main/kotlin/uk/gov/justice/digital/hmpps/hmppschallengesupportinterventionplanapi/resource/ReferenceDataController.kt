package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.ReferenceDataService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse

@RestController
@RequestMapping(
  path = ["/reference-data/{domain}"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@Tag(
  name = "7. Reference Data Controller",
  description = "Endpoints for Reference Data operations",
)
class ReferenceDataController(
  private val referenceDataService: ReferenceDataService,
) {
  @ResponseStatus(HttpStatus.OK)
  @GetMapping
  @Operation(
    summary = "Retrieve all reference data fo a domain.",
    description = "Get all reference data for a domain e.g. area-of-work",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Reference data found",
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
      ApiResponse(
        responseCode = "404",
        description = "The reference data associated with this domain was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun getReferenceData(
    @PathVariable @Parameter(
      description = "Reference data domain.",
      required = true,
    ) domain: ReferenceDataType,
    @Parameter(
      description = "Include inactive reference data. Defaults to false",
    ) includeInactive: Boolean = false,
  ): Collection<ReferenceData> = referenceDataService.getReferenceDataForDomain(domain, includeInactive)
}
