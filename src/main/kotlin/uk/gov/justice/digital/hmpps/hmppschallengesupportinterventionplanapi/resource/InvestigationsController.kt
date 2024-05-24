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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateInvestigationRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(
  path = ["/csip-records"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@Tag(
  name = "3. Investigations Controller",
  description = "Endpoints for Investigations And Interviews operations",
)
class InvestigationsController {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{recordUuid}/referral/investigation")
  @Operation(
    summary = "Add investigation and any interviews to the referral.",
    description = "Create the investigation and any interviews. Publishes prisoner-csip.csip-record-updated event with investigationAffected = true and prisoner-csip.interview-created event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Investigation and interviews added to CSIP referral",
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
  fun createInvestigation(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody createInvestigationRequest: CreateInvestigationRequest,
  ): Investigation = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/{recordUuid}/referral/investigation")
  @Operation(
    summary = "Update the investigation.",
    description = "Update the investigation only. Cannot update interviews with this endpoint. Publishes prisoner-csip.csip-record-updated event with investigationAffected = true",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Investigation updated",
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
  fun updateInvestigation(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody updateInvestigationRequest: UpdateInvestigationRequest,
  ): Investigation = throw NotImplementedError()

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{recordUuid}/referral/investigation/interviews")
  @Operation(
    summary = "Add an interview to the investigation.",
    description = "Add an interview to the investigation. Publishes prisoner-csip.interview-created event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Interview added to CSIP referral",
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
  fun createInterview(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody createInterviewRequest: CreateInterviewRequest,
  ): Interview = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/referral/investigation/interviews/{interviewUuid}")
  @Operation(
    summary = "Update an interview on the investigation.",
    description = "Update an interview on the investigation. Publishes prisoner-csip.interview-updated event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Interview updated",
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
        description = "The interview associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updateInterview(
    @PathVariable @Parameter(
      description = "Interview unique identifier",
      required = true,
    ) interviewUuid: UUID,
    @Valid @RequestBody updateInterviewRequest: UpdateInterviewRequest,
  ): Interview = throw NotImplementedError()

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/referral/investigation/interviews/{interviewUuid}")
  @Operation(
    summary = "Remove an interview from the investigation.",
    description = "Remove an interview from the investigation. Publishes prisoner-csip.interview-deleted event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Interview deleted",
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
        description = "The interview associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun deleteInterview(
    @PathVariable @Parameter(
      description = "Interview unique identifier",
      required = true,
    ) interviewUuid: UUID,
  ): Nothing = throw NotImplementedError()
}
