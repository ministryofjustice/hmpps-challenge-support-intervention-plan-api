package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate

interface PlanRequest {
  val caseManager: String?
  val reasonForPlan: String?
  val nextCaseReviewDate: LocalDate?
}

interface FirstReviewRequest : PlanRequest {
  val firstCaseReviewDate: LocalDate?
}

@Schema(description = "The request for creating a CSIP Plan for a CSIP record")
data class CreatePlanRequest(
  @Schema(description = "The case manager assigned to the CSIP plan.")
  @field:Size(min = 0, max = 100, message = "Case Manager name must be <= 100 characters")
  override val caseManager: String,

  @Schema(description = "The reasons motivating the creation of a CSIP plan.")
  @field:Size(min = 0, max = 240, message = "Reason for Plan must be <= 240 characters")
  override val reasonForPlan: String,

  @Schema(description = "The first date the CSIP plan should be reviewed.", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val nextCaseReviewDate: LocalDate,

  @Schema(description = "The needs identified in the CSIP plan.")
  @field:Valid
  override val identifiedNeeds: List<CreateIdentifiedNeedRequest> = listOf(),
) : PlanRequest, IdentifiedNeedsRequest

@Schema(description = "The request for creating a CSIP Plan for a CSIP record")
data class UpdatePlanRequest(
  @Schema(description = "The case manager assigned to the CSIP plan.")
  @field:Size(min = 0, max = 100, message = "Case Manager name must be <= 100 characters")
  override val caseManager: String?,

  @Schema(description = "The reasons motivating the creation of a CSIP plan.")
  @field:Size(min = 0, max = 240, message = "Reason for Plan must be <= 240 characters")
  override val reasonForPlan: String?,

  @Schema(description = "The first date the CSIP plan should be reviewed.", example = "2021-09-27")
  @JsonFormat(pattern = "yyyy-MM-dd")
  override val nextCaseReviewDate: LocalDate?,
) : PlanRequest
