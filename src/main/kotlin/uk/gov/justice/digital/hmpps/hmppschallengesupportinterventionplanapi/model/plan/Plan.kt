package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "The CSIP Plan of a CSIP record")
data class Plan(
  @Schema(description = "The case manager assigned to the CSIP plan.")
  val caseManager: String?,

  @Schema(description = "The reasons motivating the creation of a CSIP plan.")
  val reasonForPlan: String?,

  @Schema(description = "The first date the CSIP plan should be reviewed.", example = "2021-09-27")
  val firstCaseReviewDate: LocalDate?,

  @Schema(description = "The next date the CSIP plan should be reviewed.", example = "2021-09-27")
  val nextCaseReviewDate: LocalDate?,

  @Schema(description = "The needs identified in the CSIP plan.")
  val identifiedNeeds: Collection<IdentifiedNeed>,

  @Schema(description = "Regular reviews of the CSIP Plan")
  val reviews: Collection<Review>,
)
