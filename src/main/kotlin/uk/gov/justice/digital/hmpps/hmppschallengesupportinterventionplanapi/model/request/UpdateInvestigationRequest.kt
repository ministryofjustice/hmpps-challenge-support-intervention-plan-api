package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(
  description = "The request body to update an investigation on the incident that motivated the CSIP referral.",
)
data class UpdateInvestigationRequest(
  @Schema(
    description = "The names of the staff involved in the investigation.",
  )
  @field:Size(min = 0, max = 4000, message = "Staff involved must be <= 4000 characters")
  val staffInvolved: String?,

  @Schema(
    description = "Any evidence that was secured as part of the investigation.",
  )
  @field:Size(min = 0, max = 4000, message = "Evidence Secured must be <= 4000 characters")
  val evidenceSecured: String?,

  @Schema(
    description = "The reasons why the incident occurred.",
  )
  @field:Size(min = 0, max = 4000, message = "Occurrence reason must be <= 4000 characters")
  val occurrenceReason: String?,

  @Schema(
    description = "The normal behaviour of the person in prison.",
  )
  @field:Size(min = 0, max = 4000, message = "Person's Usual Behaviour must be <= 4000 characters")
  val personsUsualBehaviour: String?,

  @Schema(
    description = "What triggers the person in prison has that could have motivated the incident.",
  )
  @field:Size(min = 0, max = 4000, message = "Person's Trigger must be <= 4000 characters")
  val personsTrigger: String?,

  @Schema(
    description = "Any protective factors to reduce the person's risk factors and " +
      "prevent triggers for instance of violence",
  )
  @field:Size(min = 0, max = 4000, message = "Protective Factors must be <= 4000 characters")
  val protectiveFactors: String?,
)
