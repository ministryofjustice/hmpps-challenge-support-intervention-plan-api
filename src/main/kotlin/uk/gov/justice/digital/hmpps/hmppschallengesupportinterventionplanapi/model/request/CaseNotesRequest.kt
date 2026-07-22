package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "The request body for getting a case notes")
data class CaseNotesLookupRequest(
  val offenderIdentifier: String,
  val includeSensitive: Boolean = false,
  val outcomeTypeCode: String,
  val caseload: String,
)

@Schema(description = "The query parameters for filtering case notes")
data class CaseNotesFilterParams(
  val period: Long = 90,
  val pageSize: Int = 100,
)
