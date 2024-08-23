package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import jakarta.validation.Valid
import jakarta.validation.constraints.Size
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ContributoryFactorsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.LegacyIdAware
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.ScreeningOutcomeRequest
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

data class SyncReferralRequest(
  override val incidentDate: LocalDate,
  override val incidentTime: LocalTime?,
  @field:Size(min = 1, max = 12, message = "Incident Type code must be <= 12 characters")
  override val incidentTypeCode: String,
  @field:Size(min = 1, max = 12, message = "Incident Location code must be <= 12 characters")
  override val incidentLocationCode: String,
  @field:Size(min = 1, max = 240, message = "Referer name must be <= 240 characters")
  override val referredBy: String,
  @field:Size(min = 1, max = 12, message = "Area code must be <= 12 characters")
  override val refererAreaCode: String,
  override val isProactiveReferral: Boolean?,
  override val isStaffAssaulted: Boolean?,
  @field:Size(min = 0, max = 1000, message = "Name or names must be <= 1000 characters")
  override val assaultedStaffName: String?,
  @field:Size(min = 1, max = 12, message = "Involvement code must be <= 12 characters")
  override val incidentInvolvementCode: String?,
  override val descriptionOfConcern: String?,
  override val knownReasons: String?,
  override val otherInformation: String?,
  override val isSaferCustodyTeamInformed: OptionalYesNoAnswer,
  override val isReferralComplete: Boolean?,
  override val completedDate: LocalDate?,
  @field:Size(min = 0, max = 64, message = "Completed by username must be <= 64 characters")
  override val completedBy: String?,
  @field:Size(min = 0, max = 255, message = "Completed by display name must be <= 255 characters")
  override val completedByDisplayName: String?,
  @field:Valid
  override val contributoryFactors: List<SyncContributoryFactorRequest>,
  @field:Valid
  val saferCustodyScreeningOutcome: SyncScreeningOutcomeRequest?,
  @field:Valid
  val investigation: SyncInvestigationRequest?,
  @field:Valid
  val decisionAndActions: SyncDecisionAndActionsRequest?,
) : ReferralRequest, ContributoryFactorsRequest {
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
}

data class SyncContributoryFactorRequest(
  @field:Size(min = 1, max = 12, message = "Contributory factor type code must be <= 12 characters")
  override val factorTypeCode: String,
  override val comment: String?,
  override val legacyId: Long,
  override val id: UUID?,
) : NomisAudited(), NomisIdentifiable, ContributoryFactorRequest, LegacyIdAware {
  fun findRequiredReferenceDataKeys() =
    setOf(ReferenceDataKey(ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE, factorTypeCode))
}

data class SyncScreeningOutcomeRequest(
  @field:Size(min = 1, max = 12, message = "Screening outcome code must be <= 12 characters")
  override val outcomeTypeCode: String,
  @field:Size(min = 0, max = 4000, message = "Reason for decision must be <= 4000 characters")
  override val reasonForDecision: String,
  override val date: LocalDate,
  @field:Size(min = 0, max = 64, message = "Recorded by username must be <= 64 characters")
  override val recordedBy: String,
  @field:Size(min = 0, max = 255, message = "Recorded by display name must be <= 255 characters")
  override val recordedByDisplayName: String,
) : ScreeningOutcomeRequest {
  fun findRequiredReferenceDataKeys() = setOf(ReferenceDataKey(ReferenceDataType.SCREENING_OUTCOME_TYPE, outcomeTypeCode))
}
