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
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreatePlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertPlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.PlanService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(path = ["/csip-records"], produces = [MediaType.APPLICATION_JSON_VALUE])
@Tag(name = "6. Plans Controller", description = "Endpoints for Plans And Identified Needs operations")
class PlansController(private val planService: PlanService) {
  @Operation(
    summary = "Create the CSIP plan",
    description = "Create the CSIP plan. Publishes person.csip.record.updated event with affected component of Plan and IdentifiedNeed",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "CSIP plan created",
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
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{recordUuid}/plan")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun createPlan(
    @PathVariable @Parameter(description = "CSIP record unique identifier", required = true) recordUuid: UUID,
    @Valid @RequestBody request: CreatePlanRequest,
  ): Plan = planService.createPlanWithIdentifiedNeeds(recordUuid, request)

  @Operation(
    summary = "Create or update the CSIP plan",
    description = "Create or update the CSIP plan. Publishes person.csip.record.updated event with affected component of Plan",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "CSIP plan created",
      ),
      ApiResponse(
        responseCode = "200",
        description = "CSIP plan updated",
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
  @PatchMapping("/{recordUuid}/plan")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updatePlan(
    @PathVariable @Parameter(description = "CSIP record unique identifier", required = true) recordUuid: UUID,
    @Valid @RequestBody request: UpsertPlanRequest,
  ): Plan = planService.updatePlan(recordUuid, request)

  @Operation(
    summary = "Add an identified need to the plan.",
    description = "Add an identified need to the plan. Publishes prisoner-csip.identified-need.created event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Identified need added to CSIP plan",
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
        description = "The CSIP plan associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{recordUuid}/plan/identified-needs")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun createIdentifiedNeed(
    @PathVariable @Parameter(description = "CSIP record unique identifier", required = true) recordUuid: UUID,
    @Valid @RequestBody request: CreateIdentifiedNeedRequest,
  ): IdentifiedNeed = planService.addIdentifiedNeed(recordUuid, request)

  @Operation(
    summary = "Update an identified need on the plan.",
    description = "Update an identified need on the plan. Publishes prisoner-csip.identified-need-updated event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Identified Need updated",
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
        description = "The identified need associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/plan/identified-needs/{identifiedNeedUuid}")
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updateIdentifiedNeed(
    @PathVariable @Parameter(
      description = "Identified Need unique identifier",
      required = true,
    ) identifiedNeedUuid: UUID,
    @Valid @RequestBody updateIdentifiedNeed: UpdateIdentifiedNeedRequest,
  ): IdentifiedNeed = planService.updateIdentifiedNeed(identifiedNeedUuid, updateIdentifiedNeed)
}
