package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDateTime
import java.util.UUID

data class SyncCsipRequest(
  @field:Size(max = 10, message = "Prison number must be <= 10 characters")
  val prisonNumber: String,
  override val logCode: String?,
  override val referral: SyncReferralRequest?,
  @field:Valid
  val plan: SyncPlanRequest?,

  val prisonCodeWhenRecorded: String?,
  override val actionedAt: LocalDateTime,
  override val actionedBy: String,
  override val activeCaseloadId: String?,
  override val legacyId: Long,
  override val id: UUID?,
  val personSummary: PersonSummaryRequest?,
) : NomisIdentifiable, CsipRequest, LegacyIdAware, LegacyActioned {
  fun findRequiredReferenceDataKeys(): Set<ReferenceDataKey> = buildSet {
    referral?.also { addAll(it.findRequiredReferenceDataKeys()) }
  }

  fun requestMappings(): Set<RequestMapping> = buildSet {
    add(RequestMapping(CsipComponent.RECORD, legacyId, id))
    referral?.also { addAll(it.requestMappings()) }
    plan?.also { addAll(it.requestMappings()) }
  }
}

data class PersonSummaryRequest(
  val firstName: String,
  val lastName: String,
  val status: String,
  val restrictedPatient: Boolean?,
  val prisonCode: String?,
  val cellLocation: String?,
  val supportingPrisonCode: String?,
)

data class RequestMapping(val component: CsipComponent, val legacyId: Long, val id: UUID?)
