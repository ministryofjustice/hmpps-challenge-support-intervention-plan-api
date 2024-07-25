package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

object EntityGenerator {

  fun generateCsipRecord(
    prisonNumber: String,
    prisonCodeWhenRecorded: String? = null,
    logCode: String? = null,
    createdAt: LocalDateTime = LocalDateTime.now().minusDays(1),
    createdBy: String = "createdBy",
    createdByDisplayName: String = "createdByDisplayName",
    uuid: UUID = UUID.randomUUID(),
    id: Long = IdGenerator.newId(),
  ) = CsipRecord(
    prisonNumber,
    prisonCodeWhenRecorded,
    logCode,
    uuid,
    id,
  ).apply {
    this.createdAt = createdAt
    this.createdBy = createdBy
    this.createdByDisplayName = createdByDisplayName
  }

  fun CsipRecord.withReferral(
    incidentType: () -> ReferenceData,
    incidentLocation: () -> ReferenceData,
    refererAreaOfWork: () -> ReferenceData,
    incidentInvolvement: () -> ReferenceData? = { null },
    incidentDate: LocalDate = LocalDate.now(),
    incidentTime: LocalTime? = null,
    referredBy: String = "referredBy",
    referralDate: LocalDate = LocalDate.now(),
    proactiveReferral: Boolean? = null,
    staffAssaulted: Boolean? = null,
    assaultedStaffName: String? = null,
    descriptionOfConcern: String? = "descriptionOfConcern",
    knownReasons: String? = "knownReasons",
    otherInformation: String? = "otherInformation",
    saferCustodyTeamInformed: OptionalYesNoAnswer = DO_NOT_KNOW,
    referralComplete: Boolean? = false,
    referralCompletedBy: String? = null,
    referralCompletedByDisplayName: String? = null,
    referralCompletedDate: LocalDate? = null,
    id: Long = IdGenerator.newId(),
  ): CsipRecord {
    this.referral = Referral(
      this,
      referralDate,
      incidentDate,
      incidentTime,
      referredBy,
      proactiveReferral,
      staffAssaulted,
      assaultedStaffName,
      descriptionOfConcern,
      knownReasons,
      otherInformation,
      saferCustodyTeamInformed,
      referralComplete,
      referralCompletedBy,
      referralCompletedByDisplayName,
      referralCompletedDate,
      incidentType(),
      incidentLocation(),
      refererAreaOfWork(),
      incidentInvolvement(),
      id,
    )
    return this
  }

  fun generateContributoryFactor(
    contributoryFactorType: () -> ReferenceData,
    referral: Referral,
    comment: String? = null,
    createdAt: LocalDateTime = LocalDateTime.now().minusDays(1),
    createdBy: String = "AP1234",
    createdByDisplayName: String = "A Person",
    lastModifiedAt: LocalDateTime? = null,
    lastModifiedBy: String? = null,
    lastModifiedByDisplayName: String? = null,
    uuid: UUID = UUID.randomUUID(),
    id: Long = IdGenerator.newId(),
  ) = ContributoryFactor(
    referral = referral,
    contributoryFactorType = contributoryFactorType(),
    comment = comment,
    contributoryFactorUuid = uuid,
    id = id,
  ).apply {
    this.createdAt = createdAt
    this.createdBy = createdBy
    this.createdByDisplayName = createdByDisplayName
    if (lastModifiedAt != null && lastModifiedBy != null && lastModifiedByDisplayName != null) {
      recordModifiedDetails(
        CsipRequestContext(lastModifiedAt, username = lastModifiedBy, userDisplayName = lastModifiedByDisplayName),
      )
    }
  }
}
