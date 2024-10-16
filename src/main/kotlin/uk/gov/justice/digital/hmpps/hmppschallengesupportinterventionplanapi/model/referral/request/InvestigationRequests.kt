package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import java.time.LocalDate

interface InvestigationRequest {
  @get:Schema(description = "The names of the staff involved in the investigation.")
  @get:Size(min = 0, max = 4000, message = "Staff involved must be <= 4000 characters")
  val staffInvolved: String?

  @get:Schema(description = "Any evidence that was secured as part of the investigation.")
  @get:Size(min = 0, max = 4000, message = "Evidence secured must be <= 4000 characters")
  val evidenceSecured: String?

  @get:Schema(description = "The reasons why the incident occurred.")
  @get:Size(min = 0, max = 4000, message = "Occurrence reason must be <= 4000 characters")
  val occurrenceReason: String?

  @get:Schema(description = "The normal behaviour of the person in prison.")
  @get:Size(min = 0, max = 4000, message = "Person's usual behaviour must be <= 4000 characters")
  val personsUsualBehaviour: String?

  @get:Schema(description = "What triggers the person in prison has that could have motivated the incident.")
  @get:Size(min = 0, max = 4000, message = "Person's trigger must be <= 4000 characters")
  val personsTrigger: String?

  @get:Schema(description = "Any protective factors to reduce the person's risk factors and prevent triggers for instance of violence")
  @get:Size(min = 0, max = 4000, message = "Protective factors must be <= 4000 characters")
  val protectiveFactors: String?
}

interface InterviewRequest {
  @get:Schema(description = "Name of the person being interviewed.")
  @get:Size(min = 0, max = 100, message = "Interviewee name must be <= 100 characters")
  val interviewee: String

  @get:Schema(description = "The date the interview took place.", example = "2021-09-27")
  val interviewDate: LocalDate

  @get:Schema(description = "What role the interviewee played in the incident or referral.")
  @get:Size(min = 1, max = 12, message = "Interviewee role code must be <= 12 characters")
  val intervieweeRoleCode: String

  @get:Schema(description = "Information provided in interview.")
  @get:Size(min = 0, max = 4000, message = "Interview text must be <= 4000 characters")
  val interviewText: String?
}

interface InterviewsRequest {
  @get:Schema(description = "The interviews in relation to the investigation")
  @get:Valid
  val interviews: List<InterviewRequest>
}
