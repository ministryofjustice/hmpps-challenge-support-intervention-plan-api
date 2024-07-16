package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
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
    lastModifiedAt: LocalDateTime? = null,
    lastModifiedBy: String? = null,
    lastModifiedByDisplayName: String? = null,
    uuid: UUID = UUID.randomUUID(),
    id: Long = IdGenerator.newId(),
  ) = CsipRecord(
    id,
    uuid,
    prisonNumber,
    prisonCodeWhenRecorded,
    logCode,
    createdAt,
    createdBy,
    createdByDisplayName,
    lastModifiedAt,
    lastModifiedBy,
    lastModifiedByDisplayName,
  )

  fun CsipRecord.withReferral(
    incidentType: () -> ReferenceData,
    incidentLocation: () -> ReferenceData,
    refererAreaOfWork: () -> ReferenceData,
    incidentInvolvement: () -> ReferenceData? = { null },
    incidentDate: LocalDate = LocalDate.now(),
    incidentTime: LocalTime? = null,
    referredBy: String = "referredBy",
    referralDate: LocalDate = LocalDate.now(),
    referralSummary: String? = "referralSummary",
    proactiveReferral: Boolean? = null,
    staffAssaulted: Boolean? = null,
    assaultedStaffName: String? = null,
    releaseDate: LocalDate? = null,
    descriptionOfConcern: String? = "descriptionOfConcern",
    knownReasons: String? = "knownReasons",
    otherInformation: String? = "otherInformation",
    saferCustodyTeamInformed: Boolean? = false,
    referralComplete: Boolean? = true,
    referralCompletedBy: String? = "referralCompletedBy",
    referralCompletedByDisplayName: String? = "referralCompletedByDisplayName",
    referralCompletedDate: LocalDate? = LocalDate.now(),
    id: Long = IdGenerator.newId(),
  ): CsipRecord {
    val referral = Referral(
      id,
      this,
      incidentDate,
      incidentTime,
      referredBy,
      referralDate,
      referralSummary,
      proactiveReferral,
      staffAssaulted,
      assaultedStaffName,
      releaseDate,
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
    )
    this.referral = referral
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
    contributoryFactorType = contributoryFactorType(),
    referral = referral,
    comment = comment,
    createdAt = createdAt,
    createdBy = createdBy,
    createdByDisplayName = createdByDisplayName,
    lastModifiedAt = lastModifiedAt,
    lastModifiedBy = lastModifiedBy,
    lastModifiedByDisplayName = lastModifiedByDisplayName,
    contributoryFactorUuid = uuid,
    contributoryFactorId = id,
  )
}
