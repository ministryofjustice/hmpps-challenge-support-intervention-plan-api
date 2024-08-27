package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.springframework.data.web.PagedModel.PageMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import java.time.LocalDate
import java.util.UUID

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

data class CsipSummaries(val content: List<CsipSummary>, val page: PageMetadata)
