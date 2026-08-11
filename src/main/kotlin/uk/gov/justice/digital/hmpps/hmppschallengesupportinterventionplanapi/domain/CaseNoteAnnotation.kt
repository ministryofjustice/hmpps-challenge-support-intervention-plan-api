package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(name = "case_note_annotations")
class CaseNoteAnnotation(
  @Id
  val id: UUID,
  @Column(name = "request_id")
  val requestId: UUID?,
  @Column(name = "prisoner_number")
  val prisonerNumber: String,
  @Column(name = "case_note_id")
  val caseNoteId: UUID?,
  @Column(name = "prompt_key")
  val promptKey: String?,
  @Column(name = "prompt_version")
  val promptVersion: Int?,
  @Column(name = "behaviour_type")
  val behaviourType: String?,
  @Column(name = "confidence_level")
  val confidenceLevel: String?,
  @Column(name = "annotated_text")
  val annotatedText: String?,
  @Column(name = "created_date")
  val createdDate: LocalDateTime?,
)
