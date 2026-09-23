package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "case_note_annotations")
class CaseNoteAnnotation(
  @Id
  val id: UUID = newUuid(),

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  @JoinColumn(name = "case_notes_analysed_id", nullable = false)
  val caseNotesAnalysed: CaseNoteAnalysed,

  val requestId: UUID,

  val investigationId: UUID,

  val caseNoteId: UUID,

  @Enumerated(EnumType.STRING)
  val behaviourType: BehaviourType,

  @Column(columnDefinition = "TEXT")
  val annotatedText: String,

  val createdDate: LocalDateTime,
)
