package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.validation.Valid
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CompletableRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ContributoryFactorsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ReferralDateRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.ScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class SyncReferralRequest(
  override val referralDate: LocalDate,
  override val incidentDate: LocalDate,
  override val incidentTime: LocalTime?,
  override val incidentTypeCode: String,
  override val incidentLocationCode: String,
  override val referredBy: String,
  override val refererAreaCode: String,
  override val isProactiveReferral: Boolean?,
  override val isStaffAssaulted: Boolean?,
  override val assaultedStaffName: String?,
  override val incidentInvolvementCode: String?,
  override val descriptionOfConcern: String?,
  override val knownReasons: String?,
  override val otherInformation: String?,
  override val isSaferCustodyTeamInformed: OptionalYesNoAnswer,
  override val isReferralComplete: Boolean?,
  override val completedDate: LocalDate?,
  override val completedBy: String?,
  override val completedByDisplayName: String?,
  override val contributoryFactors: List<SyncContributoryFactorRequest>,
  @field:Valid
  val saferCustodyScreeningOutcome: SyncScreeningOutcomeRequest?,
  @field:Valid
  val investigation: SyncInvestigationRequest?,
  @field:Valid
  val decisionAndActions: SyncDecisionAndActionsRequest?,
) : ReferralRequest,
  CompletableRequest,
  ReferralDateRequest,
  ContributoryFactorsRequest {
  fun findRequiredReferenceDataKeys(): Set<ReferenceDataKey> = buildSet {
    add(ReferenceDataKey(INCIDENT_TYPE, incidentTypeCode))
    add(ReferenceDataKey(INCIDENT_LOCATION, incidentLocationCode))
    add(ReferenceDataKey(AREA_OF_WORK, refererAreaCode))
    incidentInvolvementCode?.also { add(ReferenceDataKey(INCIDENT_INVOLVEMENT, it)) }
    addAll(contributoryFactors.flatMap { it.findRequiredReferenceDataKeys() })
    saferCustodyScreeningOutcome?.also { addAll(it.findRequiredReferenceDataKeys()) }
    investigation?.also { addAll(it.findRequiredReferenceDataKeys()) }
    decisionAndActions?.also { addAll(it.findRequiredReferenceDataKeys()) }
  }

  fun requestMappings(): Set<RequestMapping> = buildSet {
    addAll(contributoryFactors.map { RequestMapping(CsipComponent.CONTRIBUTORY_FACTOR, it.legacyId, it.id) })
    investigation?.also { addAll(it.requestMappings()) }
  }

  @JsonIgnore
  override val completed: Boolean? = isReferralComplete
}

data class SyncContributoryFactorRequest(
  override val factorTypeCode: String,
  override val comment: String?,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisIdentifiable,
  ContributoryFactorRequest,
  LegacyIdAware {
  fun findRequiredReferenceDataKeys() = setOf(ReferenceDataKey(ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE, factorTypeCode))
}

data class SyncScreeningOutcomeRequest(
  override val outcomeTypeCode: String,
  override val reasonForDecision: String?,
  override val date: LocalDate,
  override val recordedBy: String,
  override val recordedByDisplayName: String,
) : ScreeningOutcomeRequest {
  fun findRequiredReferenceDataKeys() = setOf(ReferenceDataKey(ReferenceDataType.SCREENING_OUTCOME_TYPE, outcomeTypeCode))
}
