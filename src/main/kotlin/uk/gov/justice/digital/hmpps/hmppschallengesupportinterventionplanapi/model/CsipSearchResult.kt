package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import java.time.LocalDate
import java.util.UUID

interface PagedResponse {
  val totalElements: Long
}

data class PageMeta(override val totalElements: Long) : PagedResponse

data class CsipSearchResult(
  val id: UUID,
  val logCode: String?,
  val prisoner: Prisoner,
  val referralDate: LocalDate,
  val incidentType: String,
  val nextReviewDate: LocalDate?,
  val caseManager: String?,
  val status: ReferenceData,
)

data class Prisoner(
  val prisonNumber: String,
  val firstName: String,
  val lastName: String,
  val location: String?,
)

data class CsipSearchResults(val content: List<CsipSearchResult>, val metadata: PageMeta)
