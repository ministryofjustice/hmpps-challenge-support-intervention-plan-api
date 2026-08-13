package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ConfidenceLevel
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "case_note_annotations")
class CaseNoteAnnotation(
  @Id
  val id: UUID = newUuid(),
  val requestId: UUID?,
  @Column(nullable = false, length = 10)
  val prisonerNumber: String,
  val caseNoteId: UUID?,
  val promptKey: String?,
  val promptVersion: Int?,
  @Enumerated(EnumType.STRING)
  val behaviourType: BehaviourType?,
  @Enumerated(EnumType.STRING)
  val confidenceLevel: ConfidenceLevel?,
  @Column(columnDefinition = "TEXT")
  val annotatedText: String?,
  val createdDate: LocalDateTime?,
)
