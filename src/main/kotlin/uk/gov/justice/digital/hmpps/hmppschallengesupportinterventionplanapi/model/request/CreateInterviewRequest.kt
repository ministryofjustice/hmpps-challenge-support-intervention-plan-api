package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalDate

@Schema(description = "The request body to create an interview")
data class CreateInterviewRequest(
  @Schema(
    description = "Name of the person being interviewed.",
  )
  @field:Size(min = 0, max = 100, message = "Interviewee name must be <= 100 characters")
  val interviewee: String,

  @Schema(
    description = "The date the interview took place.",
    example = "2021-09-27",
  )
  @JsonFormat(pattern = "yyyy-MM-dd")
  val interviewDate: LocalDate,

  @Schema(
    description = "What role the interviewee played in the incident or referral.",
  )
  @field:Size(min = 1, max = 12, message = "Interviewee Role Code must be <= 12 characters")
  val intervieweeRoleCode: String,

  @Schema(
    description = "Information provided in interview.",
  )
  @field:Size(min = 0, max = 4000, message = "Interview Text must be <= 4000 characters")
  val interviewText: String?,
)
