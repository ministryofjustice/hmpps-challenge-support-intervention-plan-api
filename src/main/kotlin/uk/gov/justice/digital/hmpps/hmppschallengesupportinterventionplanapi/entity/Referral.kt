package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.MapsId
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import org.springframework.data.domain.AbstractAggregateRoot
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipUpdatedEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.SaferCustodyScreeningOutcomeAlreadyExistException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table
data class Referral(
  @OneToOne(fetch = FetchType.LAZY) @MapsId("record_id") @JoinColumn(
    name = "record_id",
    referencedColumnName = "record_id",
  ) @Id val csipRecord: CsipRecord,

  @Column(nullable = false) val incidentDate: LocalDate,

  @Column val incidentTime: LocalTime? = null,

  @Column(nullable = false, length = 240) val referredBy: String,

  @Column(nullable = false) val referralDate: LocalDate,

  @Column val referralSummary: String? = null,

  @Column val proactiveReferral: Boolean? = null,

  @Column val staffAssaulted: Boolean? = null,

  @Column val assaultedStaffName: String? = null,

  @Column val releaseDate: LocalDate? = null,

  @Column(nullable = false) val descriptionOfConcern: String,

  @Column(nullable = false) val knownReasons: String,

  @Column val otherInformation: String? = null,

  @Column val saferCustodyTeamInformed: Boolean? = null,

  @Column val referralComplete: Boolean? = null,

  @Column(length = 32) val referralCompletedBy: String? = null,

  @Column(length = 255) val referralCompletedByDisplayName: String? = null,

  @Column val referralCompletedDate: LocalDate? = null,

  @ManyToOne @JoinColumn(
    name = "incident_type_id",
    referencedColumnName = "reference_data_id",
  ) val incidentType: ReferenceData,

  @ManyToOne @JoinColumn(
    name = "incident_location_id",
    referencedColumnName = "reference_data_id",
  ) val incidentLocation: ReferenceData,

  @ManyToOne @JoinColumn(
    name = "referer_area_of_work_id",
    referencedColumnName = "reference_data_id",
  ) val refererAreaOfWork: ReferenceData,

  @ManyToOne @JoinColumn(
    name = "incident_involvement_id",
    referencedColumnName = "reference_Data_id",
  ) val incidentInvolvement: ReferenceData,
) : AbstractAggregateRoot<Referral>() {
  @OneToOne(
    mappedBy = "referral",
    fetch = FetchType.LAZY,
    cascade = [CascadeType.PERSIST, CascadeType.MERGE, CascadeType.REMOVE],
  )
  private var saferCustodyScreeningOutcome: SaferCustodyScreeningOutcome? = null

  fun saferCustodyScreeningOutcome() = saferCustodyScreeningOutcome

  fun createSaferCustodyScreeningOutcome(
    outcomeType: ReferenceData,
    date: LocalDate,
    reasonForDecision: String,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String,
    actionedByDisplayName: String,
    source: Source,
    reason: Reason = Reason.USER,
    activeCaseLoadId: String?,
    description: String = "Safer custody screening outcome added to referral",
  ): CsipRecord {
    if (saferCustodyScreeningOutcome != null) {
      throw SaferCustodyScreeningOutcomeAlreadyExistException(csipRecord.recordUuid)
    }

    saferCustodyScreeningOutcome = SaferCustodyScreeningOutcome(
      referral = this,
      outcomeType = outcomeType,
      recordedBy = actionedBy,
      recordedByDisplayName = actionedByDisplayName,
      date = date,
      reasonForDecision = reasonForDecision,
    )

    with(csipRecord) {
      addAuditEvent(
        action = AuditEventAction.UPDATED,
        description = description,
        actionedAt = actionedAt,
        actionedBy = actionedBy,
        actionedByCapturedName = actionedByDisplayName,
        source = source,
        reason = reason,
        activeCaseLoadId = activeCaseLoadId,
        isSaferCustodyScreeningOutcomeAffected = true,
      )
      registerCsipEvent(
        CsipUpdatedEvent(
          recordUuid = csipRecord.recordUuid,
          prisonNumber = csipRecord.prisonNumber,
          description = description,
          occurredAt = actionedAt,
          source = source,
          reason = reason,
          updatedBy = actionedBy,
          isSaferCustodyScreeningOutcomeAffected = true,
        ),
      )
    }

    return csipRecord
  }
}
