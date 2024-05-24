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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReviewRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.util.UUID

@RestController
@RequestMapping(
  path = ["/csip-records"],
  produces = [MediaType.APPLICATION_JSON_VALUE],
)
@Tag(
  name = "6. Reviews Controller",
  description = "Endpoints for Reviews And Attendees operations",
)
class ReviewsController {
  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/{recordUuid}/plan/reviews")
  @Operation(
    summary = "Add a review of the plan and any attendees.",
    description = "Create a review of the plan and any attendees. Publishes prisoner-csip.review-created and prisoner-csip.attendee-created events",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Reviews and attendees added to CSIP plan",
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
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun createReview(
    @PathVariable @Parameter(
      description = "CSIP record unique identifier",
      required = true,
    ) recordUuid: UUID,
    @Valid @RequestBody createReviewRequest: CreateReviewRequest,
  ): Review = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/plan/reviews/{reviewUuid}")
  @Operation(
    summary = "Update a review of the plan.",
    description = "Update a review of the plan only. Cannot update attendees with this endpoint. Publishes prisoner-csip.review-updated event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Review updated",
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
        description = "The review associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updateReview(
    @PathVariable @Parameter(
      description = "Review unique identifier",
      required = true,
    ) reviewUuid: UUID,
    @Valid @RequestBody updateReviewRequest: UpdateReviewRequest,
  ): Review = throw NotImplementedError()

  @ResponseStatus(HttpStatus.CREATED)
  @PostMapping("/plan/reviews/{reviewUuid}/attendees")
  @Operation(
    summary = "Add an attendee to a review of the plan.",
    description = "Add an attendee to a review of the plan. Publishes prisoner-csip.attendee-created event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "201",
        description = "Attendee added to review",
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
        description = "The review associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun createAttendee(
    @PathVariable @Parameter(
      description = "Review unique identifier",
      required = true,
    ) reviewUuid: UUID,
    @Valid @RequestBody createAttendeeRequest: CreateAttendeeRequest,
  ): Attendee = throw NotImplementedError()

  @ResponseStatus(HttpStatus.OK)
  @PatchMapping("/plan/reviews/attendees/{attendeeUuid}")
  @Operation(
    summary = "Update an attendee on a review of the plan.",
    description = "Update an attendee on a review of the plan. Publishes prisoner-csip.attendee-updated event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "200",
        description = "Attendee updated",
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
        description = "The attendee associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun updateAttendee(
    @PathVariable @Parameter(
      description = "Attendee unique identifier",
      required = true,
    ) attendeeUuid: UUID,
    @Valid @RequestBody updateAttendeeRequest: UpdateAttendeeRequest,
  ): Attendee = throw NotImplementedError()

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @DeleteMapping("/plan/reviews/attendees/{attendeeUuid}")
  @Operation(
    summary = "Remove an attendee from a review of the plan.",
    description = "Remove an attendee from a review of the plan. Publishes prisoner-csip.attendee-deleted event",
  )
  @ApiResponses(
    value = [
      ApiResponse(
        responseCode = "204",
        description = "Attendee deleted",
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
        description = "The attendee associated with this identifier was not found.",
        content = [Content(schema = Schema(implementation = ErrorResponse::class))],
      ),
    ],
  )
  @PreAuthorize("hasAnyRole('$ROLE_CSIP_UI')")
  fun deleteAttendee(
    @PathVariable @Parameter(
      description = "Attendee unique identifier",
      required = true,
    ) attendeeUuid: UUID,
  ): Nothing = throw NotImplementedError()
}
