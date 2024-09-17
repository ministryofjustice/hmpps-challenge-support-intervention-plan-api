package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ValidInvestigationDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ValidInvestigationDetail.Companion.WITH_INTERVIEW_MESSAGE

interface InvestigationRequest {
  val staffInvolved: String?
  val evidenceSecured: String?
  val occurrenceReason: String?
  val personsUsualBehaviour: String?
  val personsTrigger: String?
  val protectiveFactors: String?
}

@Schema(description = "The request body to create an investigation on the incident that motivated the CSIP referral.")
@ValidInvestigationDetail(message = WITH_INTERVIEW_MESSAGE)
data class CreateInvestigationRequest(
  @Schema(description = "The names of the staff involved in the investigation.")
  @field:Size(min = 0, max = 4000, message = "Staff involved must be <= 4000 characters")
  override val staffInvolved: String?,

  @Schema(description = "Any evidence that was secured as part of the investigation.")
  @field:Size(min = 0, max = 4000, message = "Evidence Secured must be <= 4000 characters")
  override val evidenceSecured: String?,

  @Schema(description = "The reasons why the incident occurred.")
  @field:Size(min = 0, max = 4000, message = "Occurrence reason must be <= 4000 characters")
  override val occurrenceReason: String?,

  @Schema(description = "The normal behaviour of the person in prison.")
  @field:Size(min = 0, max = 4000, message = "Person's Usual Behaviour must be <= 4000 characters")
  override val personsUsualBehaviour: String?,

  @Schema(description = "What triggers the person in prison has that could have motivated the incident.")
  @field:Size(min = 0, max = 4000, message = "Person's Trigger must be <= 4000 characters")
  override val personsTrigger: String?,

  @Schema(
    description = "Any protective factors to reduce the person's risk factors and prevent triggers for instance of violence",
  )
  @field:Size(min = 0, max = 4000, message = "Protective Factors must be <= 4000 characters")
  override val protectiveFactors: String?,

  @Schema(description = "The interviews in relation to the investigation")
  @field:Valid
  override val interviews: List<CreateInterviewRequest> = listOf(),
) : InvestigationRequest, InterviewsRequest

@Schema(description = "The request body to update an investigation on the incident that motivated the CSIP referral.")
@ValidInvestigationDetail
data class UpdateInvestigationRequest(
  @Schema(description = "The names of the staff involved in the investigation.")
  @field:Size(min = 0, max = 4000, message = "Staff involved must be <= 4000 characters")
  override val staffInvolved: String?,

  @Schema(description = "Any evidence that was secured as part of the investigation.")
  @field:Size(min = 0, max = 4000, message = "Evidence Secured must be <= 4000 characters")
  override val evidenceSecured: String?,

  @Schema(description = "The reasons why the incident occurred.")
  @field:Size(min = 0, max = 4000, message = "Occurrence reason must be <= 4000 characters")
  override val occurrenceReason: String?,

  @Schema(description = "The normal behaviour of the person in prison.")
  @field:Size(min = 0, max = 4000, message = "Person's Usual Behaviour must be <= 4000 characters")
  override val personsUsualBehaviour: String?,

  @Schema(description = "What triggers the person in prison has that could have motivated the incident.")
  @field:Size(min = 0, max = 4000, message = "Person's Trigger must be <= 4000 characters")
  override val personsTrigger: String?,

  @Schema(
    description = "Any protective factors to reduce the person's risk factors and prevent triggers for instance of violence",
  )
  @field:Size(min = 0, max = 4000, message = "Protective Factors must be <= 4000 characters")
  override val protectiveFactors: String?,
) : InvestigationRequest
