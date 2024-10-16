package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
import java.time.LocalDate
import java.util.UUID

interface PagedResponse {
  val totalElements: Long
}

data class PageMeta(override val totalElements: Long) : PagedResponse

data class CsipSummary(
  val id: UUID,
  val prisonNumber: String,
  val logCode: String?,
  val referralDate: LocalDate,
  val nextReviewDate: LocalDate?,
  val incidentType: ReferenceData,
  val caseManager: String?,
  val status: CsipStatus,
)

data class CsipSummaries(val content: List<CsipSummary>, val metadata: PageMeta)
