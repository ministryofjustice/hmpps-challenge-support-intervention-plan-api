package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.toModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ContributoryFactor as ContributoryFactorModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord as CsipRecordModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Referral as ReferralModel

fun ContributoryFactor.toModel() =
  ContributoryFactorModel(
    factorUuid = id,
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
    referralCompletedDate = referralCompletedDate,
    referralCompletedBy = referralCompletedBy,
    referralCompletedByDisplayName = referralCompletedByDisplayName,
    isSaferCustodyTeamInformed = saferCustodyTeamInformed,
    saferCustodyScreeningOutcome = saferCustodyScreeningOutcome?.toModel(),
    decisionAndActions = decisionAndActions?.toModel(),
    investigation = investigation?.toModel(),
    contributoryFactors = contributoryFactors().map { it.toModel() },
  )

fun CsipRecord.toModel() =
  CsipRecordModel(
    recordUuid = id,
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
    status = status,
  )
