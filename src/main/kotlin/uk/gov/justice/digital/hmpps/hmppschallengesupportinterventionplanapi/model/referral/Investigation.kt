package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral

import com.fasterxml.jackson.annotation.JsonIgnore
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The investigation on the incident that motivated the CSIP referral")
data class Investigation(
  @Schema(description = "The names of the staff involved in the investigation.")
  val staffInvolved: String?,

  @Schema(description = "Any evidence that was secured as part of the investigation.")
  val evidenceSecured: String?,

  @Schema(description = "The reasons why the incident occurred.")
  val occurrenceReason: String?,

  @Schema(description = "The normal behaviour of the person in prison.")
  val personsUsualBehaviour: String?,

  @Schema(description = "What triggers the person in prison has that could have motivated the incident.")
  val personsTrigger: String?,

  @Schema(description = "Any protective factors to reduce the person's risk factors and prevent triggers for instance of violence")
  val protectiveFactors: String?,

  @Schema(description = "Username of the user that created the investigation")
  val recordedBy: String?,

  @Schema(description = "Display name of the user that created the investigation")
  val recordedByDisplayName: String?,

  @Schema(description = "The interviews in relation to the Investigation")
  val interviews: Collection<Interview>,
) {
  @JsonIgnore
  var new: Boolean = false
}
