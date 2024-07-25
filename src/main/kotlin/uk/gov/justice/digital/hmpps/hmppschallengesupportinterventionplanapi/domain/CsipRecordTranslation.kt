package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ContributoryFactor as ContributoryFactorModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord as CsipRecordModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Referral as ReferralModel

fun CreateCsipRecordRequest.toInitialReferralEntity(
  csipRecord: CsipRecord,
  csipRequestContext: CsipRequestContext,
  referenceDataRepository: ReferenceDataRepository,
): Referral {
  return Referral(
    csipRecord = csipRecord,
    referralDate = csipRequestContext.requestAt.toLocalDate(),
    incidentDate = referral.incidentDate,
    incidentTime = referral.incidentTime,
    referredBy = referral.referredBy,
    proactiveReferral = referral.isProactiveReferral,
    staffAssaulted = referral.isStaffAssaulted,
    assaultedStaffName = referral.assaultedStaffName,
    descriptionOfConcern = referral.descriptionOfConcern,
    knownReasons = referral.knownReasons,
    otherInformation = referral.otherInformation,
    saferCustodyTeamInformed = referral.isSaferCustodyTeamInformed,
    incidentType = referenceDataRepository.getReferenceData(INCIDENT_TYPE, referral.incidentTypeCode),
    incidentLocation = referenceDataRepository.getReferenceData(INCIDENT_LOCATION, referral.incidentLocationCode),
    refererAreaOfWork = referenceDataRepository.getReferenceData(
      ReferenceDataType.AREA_OF_WORK,
      referral.refererAreaCode,
    ),
    incidentInvolvement = referral.incidentInvolvementCode?.let {
      referenceDataRepository.getReferenceData(
        INCIDENT_TYPE,
        it,
      )
    },
  )
}

fun ContributoryFactor.toModel() =
  ContributoryFactorModel(
    factorUuid = contributoryFactorUuid,
    factorType = contributoryFactorType.toReferenceDataModel(),
    comment = comment,
    createdAt = createdAt,
    createdBy = createdBy,
    createdByDisplayName = createdByDisplayName,
    lastModifiedAt = lastModifiedAt,
    lastModifiedBy = lastModifiedBy,
    lastModifiedByDisplayName = lastModifiedByDisplayName,
  )

fun Referral.toModel() =
  ReferralModel(
    incidentDate = incidentDate,
    incidentTime = incidentTime,
    incidentType = incidentType.toReferenceDataModel(),
    incidentLocation = incidentLocation.toReferenceDataModel(),
    incidentInvolvement = incidentInvolvement?.toReferenceDataModel(),
    refererArea = refererAreaOfWork.toReferenceDataModel(),
    referredBy = referredBy,
    otherInformation = otherInformation,
    knownReasons = knownReasons,
    descriptionOfConcern = descriptionOfConcern,
    assaultedStaffName = assaultedStaffName,
    isProactiveReferral = proactiveReferral,
    isStaffAssaulted = staffAssaulted,
    isReferralComplete = referralComplete,
    isSaferCustodyTeamInformed = saferCustodyTeamInformed,
    saferCustodyScreeningOutcome = null,
    decisionAndActions = null,
    investigation = null,
    contributoryFactors = contributoryFactors().map { it.toModel() },
  )

fun CsipRecord.toModel() =
  CsipRecordModel(
    recordUuid = recordUuid,
    prisonNumber = prisonNumber,
    prisonCodeWhenRecorded = prisonCodeWhenRecorded,
    logCode = logCode,
    createdAt = createdAt,
    createdBy = createdBy,
    createdByDisplayName = createdByDisplayName,
    lastModifiedAt = lastModifiedAt,
    lastModifiedBy = lastModifiedBy,
    lastModifiedByDisplayName = lastModifiedByDisplayName,
    referral = referral!!.toModel(),
    plan = null,
  )
