package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(
  description = "The request for updating a CSIP Plan for a CSIP record",
)
data class UpdatePlanRequest(
  @Schema(
    description = "The case manager assigned to the CSIP plan.",
  )
  @field:Size(min = 0, max = 100, message = "Case Manager name must be <= 100 characters")
  val caseManager: String,

  @Schema(
    description = "The reasons motivating the creation of a CSIP plan.",
  )
  @field:Size(min = 0, max = 240, message = "Reason for Plan must be <= 240 characters")
  val reasonForPlan: String,

  @Schema(
    description = "The first date the CSIP plan should be reviewed.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val firstCaseReviewDate: LocalDate,
)
