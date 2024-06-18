package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ContributoryFactor as ContributoryFactorModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord as CsipRecordModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Referral as ReferralModel

fun CreateCsipRecordRequest.toCsipRecord(
  prisonNumber: String,
  csipRequestContext: CsipRequestContext,
) = CsipRecord(
  prisonNumber = prisonNumber,
  logNumber = logNumber,
  createdAt = csipRequestContext.requestAt,
  createdBy = csipRequestContext.username,
  createdByDisplayName = csipRequestContext.userDisplayName,
)

fun CreateCsipRecordRequest.toInitialReferralEntity(
  csipRecord: CsipRecord,
  csipRequestContext: CsipRequestContext,
  incidentType: ReferenceData,
  incidentLocation: ReferenceData,
  referrerAreaOfWork: ReferenceData,
  incidentInvolvement: ReferenceData?,
) =
  Referral(
    csipRecord = csipRecord,
    incidentDate = referral.incidentDate,
    incidentTime = referral.incidentTime,
    referredBy = referral.referredBy,
    referralDate = csipRequestContext.requestAt.toLocalDate(),
    referralSummary = referral.referralSummary,
    proactiveReferral = referral.isProactiveReferral,
    staffAssaulted = referral.isStaffAssaulted,
    assaultedStaffName = referral.assaultedStaffName,
    descriptionOfConcern = referral.descriptionOfConcern,
    knownReasons = referral.knownReasons,
    otherInformation = referral.otherInformation,
    saferCustodyTeamInformed = referral.isSaferCustodyTeamInformed,
    incidentType = incidentType,
    incidentLocation = incidentLocation,
    refererAreaOfWork = referrerAreaOfWork,
    incidentInvolvement = incidentInvolvement,
  )

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
    referralSummary = referralSummary,
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
    logNumber = logNumber,
    createdAt = createdAt,
    createdBy = createdBy,
    createdByDisplayName = createdByDisplayName,
    lastModifiedAt = lastModifiedAt,
    lastModifiedBy = lastModifiedBy,
    lastModifiedByDisplayName = lastModifiedByDisplayName,
    referral = referral!!.toModel(),
    plan = null,
  )
