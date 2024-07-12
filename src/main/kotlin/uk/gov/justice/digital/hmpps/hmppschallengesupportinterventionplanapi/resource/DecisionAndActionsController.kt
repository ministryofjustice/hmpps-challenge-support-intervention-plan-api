package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.DecisionActionsService
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(
  path = ["/csip-records/{recordUuid}/referral/decision-and-actions"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@Tag(
  name = "4. Decision And Actions Controller",
  description = "Endpoints for Decision And Actions operations",
)
class DecisionAndActionsController(
  private val decisionActionsService: DecisionActionsService,
) {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping
  @Operation(
    summary = "Add decision and actions to the referral.",
    description = "Create the decision and actions. Publishes person.csip.record.updated event with decisionAndActionsAffected = true",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Decision and actions added to CSIP referral",
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
      ApiResponse(
        responseCode = "409",
        description = "The CSIP referral associated with this identifier already has a decision and actions",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI', '$ROLE_NOMIS')")
  fun createDecision(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody createDecisionAndActionsRequest: CreateDecisionAndActionsRequest,
    httpRequest: HttpServletRequest,
  ): DecisionAndActions = decisionActionsService.createDecisionAndActionsRequest(
    recordUuid,
    createDecisionAndActionsRequest,
    httpRequest.csipRequestContext(),
  )

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping
  @Operation(
    summary = "Update the decision and actions.",
    description = "Update the decision and actions. Publishes person.csip.record.updated event with decisionAndActionsAffected = true",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Decision and Actions updated",
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
  fun updateDecision(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody updateDecisionAndActionsRequest: UpdateDecisionAndActionsRequest,
  ): DecisionAndActions = throw NotImplementedError()
}
