package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

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
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpsertSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.SaferCustodyScreeningOutcomeService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(
  path = ["/csip-records/{recordUuid}/referral/safer-custody-screening"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@Tag(
  name = "3. Safer Custody Screening Outcome Controller",
  description = "Endpoints for Safer Custody Screening Outcome operations",
)
class SaferCustodyScreeningOutcomesController(
  private val screeningOutcomeService: SaferCustodyScreeningOutcomeService,
) {

  @Operation(
    summary = "Upsert a safer custody screening outcome to the referral.",
    description = "Create or update the safer custody screening outcome. Publishes person.csip.record.updated event with saferCustodyScreeningOutcomeAffected = true",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Safer Custody Screening Outcome created on the CSIP referral",
      ),
      ApiResponse(
        responseCode = "200",
        description = "Safer Custody Screening Outcome updated on the CSIP referral",
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
  @PutMapping
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun upsertScreeningOutcome(
    @PathVariable @Parameter(description = "CSIP record unique identifier", required = true) recordUuid: UUID,
    @Valid @RequestBody request: UpsertSaferCustodyScreeningOutcomeRequest,
  ): ResponseEntity<SaferCustodyScreeningOutcome> = screeningOutcomeService.upsertScreeningOutcome(recordUuid, request).let {
    if (it.new) {
      ResponseEntity.status(HttpStatus.CREATED).body(it)
    } else {
      ResponseEntity.ok(it)
    }
  }
}
