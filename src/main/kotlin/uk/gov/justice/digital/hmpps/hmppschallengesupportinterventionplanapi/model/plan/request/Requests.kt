package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import java.time.LocalDate

interface PlanRequest {
  @get:Schema(description = "The case manager assigned to the CSIP plan.")
  @get:Size(min = 0, max = 100, message = "Case manager name must be <= 100 characters")
  val caseManager: String?

  @get:Schema(description = "The reasons motivating the creation of a CSIP plan.")
  @get:Size(min = 0, max = 240, message = "Reason for plan must be <= 240 characters")
  val reasonForPlan: String?

  @get:Schema(description = "The next date the CSIP plan should be reviewed.", example = "2021-09-27")
  val nextCaseReviewDate: LocalDate?
}

interface FirstReviewRequest : PlanRequest {
  val firstCaseReviewDate: LocalDate?
}

interface IdentifiedNeedRequest {
  @get:Schema(description = "Details of the need identified in the CSIP plan.")
  @get:Size(min = 0, max = 1000, message = "Identified need must be <= 1000 characters")
  val identifiedNeed: String

  @get:Schema(description = "The name of the person who is responsible for taking action on the intervention.")
  @get:Size(min = 0, max = 100, message = "Responsible person name must be <= 100 characters")
  val responsiblePerson: String

  @get:Schema(description = "The date the need was identified.", example = "2021-09-27")
  val createdDate: LocalDate

  @get:Schema(description = "The target date the need should be progressed or resolved.", example = "2021-09-27")
  val targetDate: LocalDate

  @get:Schema(description = "The date the identified need was resolved or closed.", example = "2021-09-27")
  val closedDate: LocalDate?

  @get:Schema(description = "The planned intervention for the identified need.")
  @get:Size(min = 0, max = 4000, message = "Intervention must be <= 4000 characters")
  val intervention: String

  @get:Schema(description = "How the plan to address the identified need is progressing.")
  @get:Size(min = 0, max = 4000, message = "Progression must be <= 4000 characters")
  val progression: String?
}

interface IdentifiedNeedsRequest {
  @get:Schema(description = "The needs identified in the CSIP plan.")
  @get:Valid
  val identifiedNeeds: List<IdentifiedNeedRequest>
}

interface ReviewRequest {
  @get:Schema(description = "The date of the review.", example = "2021-09-27")
  val reviewDate: LocalDate?

  @get:Schema(description = "The username of the person who recorded the review.")
  @get:Size(min = 0, max = 64, message = "Recorded by username must be <= 64 characters")
  val recordedBy: String

  @get:Schema(description = "The displayable name of the person who recorded the review.")
  @get:Size(min = 0, max = 255, message = "Recorded by display name must be <= 255 characters")
  val recordedByDisplayName: String

  @get:Schema(description = "The date of the next review.", example = "2021-09-27")
  val nextReviewDate: LocalDate?

  @get:Schema(description = "The date the CSIP plan was closed following a review outcome decision to close it.")
  val csipClosedDate: LocalDate?

  @get:Schema(description = "Additional information about the review.")
  @get:Size(min = 0, max = 4000, message = "Summary must be <= 4000 characters")
  val summary: String?

  @get:Schema(description = "A list of actions following the review.")
  val actions: Set<ReviewAction>
}

interface ReviewsRequest {
  @get:Schema(description = "The reviews of the CSIP plan.")
  @get:Valid
  val reviews: List<ReviewRequest>
}

interface AttendeeRequest {
  @get:Schema(description = "Name of review attendee/contributor.")
  @get:Size(min = 0, max = 100, message = "Attendee name must be <= 100 characters")
  val name: String?

  @get:Schema(description = "Role of review attendee/contributor.")
  @get:Size(min = 0, max = 50, message = "Attendee role must be <= 50 characters")
  val role: String?

  @get:Schema(description = "If the person attended the review.")
  val isAttended: Boolean?

  @get:Schema(description = "Description of attendee contribution.")
  @get:Size(min = 0, max = 4000, message = "Contribution must be <= 4000 characters")
  val contribution: String?
}

interface AttendeesRequest {
  @get:Schema(description = "The attendees/contributors to the review.")
  @get:Valid
  val attendees: List<AttendeeRequest>
}
