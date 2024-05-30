package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.request.CreateReferenceDataRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.request.UpdateReferenceDataRequest
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

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Add a reference data code.",
    description = "Add a reference data code. Publishes prisoner-csip.reference-data-code-created event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Reference data code added",
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
      ApiResponse(
        responseCode = "404",
        description = "The reference data associated with this domain was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun addReferenceDataCode(
    @PathVariable @Parameter(
      description = "Reference data domain.",
      required = true,
    ) domain: ReferenceDataType,
    @Valid @RequestBody createReferenceDataRequest: CreateReferenceDataRequest,
  ): ReferenceData = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/{code}")
  @Operation(
    summary = "Update a reference data code’s properties.",
    description = "Update a reference data code’s properties. Publishes prisoner-csip.reference-data-code-updated event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Reference data code updated",
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
      ApiResponse(
        responseCode = "404",
        description = "The reference data associated with this domain and code was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updateReferenceDataCode(
    @PathVariable @Parameter(
      description = "Reference data domain.",
      required = true,
    ) domain: ReferenceDataType,
    @PathVariable @Parameter(
      description = "Short code of the reference data to be updated",
      required = true,
    ) code: String,
    @Valid @RequestBody updateReferenceDataRequest: UpdateReferenceDataRequest,
  ): ReferenceData = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/{code}/deactivate")
  @Operation(
    summary = "Deactivate a reference data code.",
    description = "Deactivate a reference data code. Publishes prisoner-csip.reference-data-code-deactivated event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Reference data code deactivated",
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
        description = "The reference data associated with this domain and code was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun deactivateReferenceDataCode(
    @PathVariable @Parameter(
      description = "Reference data domain.",
      required = true,
    ) domain: ReferenceDataType,
    @PathVariable @Parameter(
      description = "Short code of the reference data to be deactivated",
      required = true,
    ) code: String,
  ): ReferenceData = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/{code}/reactivate")
  @Operation(
    summary = "Reactivate a reference data code.",
    description = "Reactivate a reference data code. Publishes prisoner-csip.reference-data-code-reactivated event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Reference data code reactivated",
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
        description = "The reference data associated with this domain and code was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun reactivateReferenceDataCode(
    @PathVariable @Parameter(
      description = "Reference data domain.",
      required = true,
    ) domain: ReferenceDataType,
    @PathVariable @Parameter(
      description = "Short code of the reference data to be reactivated",
      required = true,
    ) code: String,
  ): ReferenceData = throw NotImplementedError()
}
