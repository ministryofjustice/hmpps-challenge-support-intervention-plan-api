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
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReferralRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "2. Referral Controller", description = "Endpoints for Referral and Contributory Factor operations")
class ReferralsController {
  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/csip-records/{recordUuid}/referral")
  @Operation(
    summary = "Update the CSIP referral",
    description = "Update the CSIP referral only. Cannot update contributory factors with this endpoint. Publishes prisoner-csip.csip-record-created event with referralAffected = true",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "CSIP referral updated",
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
        description = "The CSIP referral associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updateReferral(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody updateReferralRequest: UpdateReferralRequest,
  ): Referral = throw NotImplementedError()

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/csip-records/{recordUuid}/referral/contributory-factors")
  @Operation(
    summary = "Add a contributory factor to the referral.",
    description = "Add a contributory factor to the referral. Publishes prisoner-csip.contributory-factor-created event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "New contributory factor added to CSIP referral",
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
        description = "The CSIP referral associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun createContributoryFactor(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody createContributoryFactorRequest: CreateContributoryFactorRequest,
  ): ContributoryFactor = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/csip-records/referral/contributory-factors/{contributoryFactorUuid}")
  @Operation(
    summary = "Update a contributory factor on the referral.",
    description = "Update a contributory factor on the referral. Publishes prisoner-csip.contributory-factor-updated event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Contributory factor updated",
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
        description = "The contributory factor associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updateContributoryFactor(
    @PathVariable @Parameter(
      description = "Contributory Factor unique identifier",
      required = true,
    ) contributoryFactorUuid: UUID,
    @Valid @RequestBody updateContributoryFactorRequest: UpdateContributoryFactorRequest,
  ): ContributoryFactor = throw NotImplementedError()

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/csip-records/referral/contributory-factors/{contributoryFactorUuid}")
  @Operation(
    summary = "Remove a contributory factor on the referral.",
    description = "Remove a contributory factor from the referral. Publishes prisoner-csip.contributory-factor-deleted event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Contributory factor deleted",
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
        description = "The contributory factor associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun deleteContributoryFactor(
    @PathVariable @Parameter(
      description = "Contributory Factor unique identifier",
      required = true,
    ) contributoryFactorUuid: UUID,
  ): Nothing = throw NotImplementedError()
}
