package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.internal

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.byId
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.byLegacyId
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.ResponseMapping
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.withAuditInfo
import java.util.UUID

@Service
@Transactional
class SyncReferral {
  fun sync(
    referral: Referral,
    request: SyncReferralRequest,
    rdSupplier: (ReferenceDataType, String) -> ReferenceData,
  ): Set<ResponseMapping> {
    val factorMappings = request.contributoryFactors.map {
      val cf = referral.findContributoryFactor(it.id, it.legacyId)?.update(it, rdSupplier)
        ?: referral.addContributoryFactor(it, rdSupplier)
      cf.withAuditInfo(it)
      ResponseMapping(CsipComponent.CONTRIBUTORY_FACTOR, it.legacyId, cf.id)
    }.toSet()
    request.saferCustodyScreeningOutcome?.also {
      referral.saferCustodyScreeningOutcome?.update(it, rdSupplier)
        ?: referral.createSaferCustodyScreeningOutcome(request.saferCustodyScreeningOutcome, rdSupplier)
    }
    val interviewMappings = request.investigation?.let { inv ->
      val investigation = referral.upsertInvestigation(inv)
      inv.interviews.map {
        val interview = investigation.findInterview(it.id, it.legacyId)?.update(it, rdSupplier)
          ?: investigation.addInterview(it, rdSupplier)
        interview.withAuditInfo(it)
        ResponseMapping(CsipComponent.INTERVIEW, it.legacyId, interview.id)
      }
    } ?: emptySet()
    request.decisionAndActions?.also {
      referral.upsertDecisionAndActions(it, rdSupplier)
    }
    return factorMappings + interviewMappings
  }
}

private fun Referral.findContributoryFactor(uuid: UUID?, legacyId: Long): ContributoryFactor? =
  contributoryFactors().find { it.byId(uuid) || it.byLegacyId(legacyId) }

private fun Investigation.findInterview(uuid: UUID?, legacyId: Long) =
  interviews().find { it.byId(uuid) || it.byLegacyId(legacyId) }
