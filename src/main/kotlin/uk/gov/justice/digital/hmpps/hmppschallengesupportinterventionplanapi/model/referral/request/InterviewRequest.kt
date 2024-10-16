package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate

@Schema(description = "The request body to create an interview")
data class CreateInterviewRequest(
  override val interviewee: String,
  override val interviewDate: LocalDate,
  override val intervieweeRoleCode: String,
  override val interviewText: String?,
) : InterviewRequest

@Schema(description = "The request body to update an interview")
data class UpdateInterviewRequest(
  override val interviewee: String,
  override val interviewDate: LocalDate,
  override val intervieweeRoleCode: String,
  override val interviewText: String?,
) : InterviewRequest
