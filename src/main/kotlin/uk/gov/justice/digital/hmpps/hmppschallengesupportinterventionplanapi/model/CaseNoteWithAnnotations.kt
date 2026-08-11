package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote
import java.time.LocalDateTime
import java.util.UUID

data class CaseNoteWithAnnotations(
  val caseNote: CaseNote,
  val annotations: List<CaseNoteAnnotationSummary>,
)

data class CaseNoteAnnotationSummary(
  val id: UUID,
  val requestId: UUID?,
  val prisonerNumber: String,
  val caseNoteId: UUID?,
  val promptKey: String?,
  val promptVersion: Int?,
  val behaviourType: String?,
  val confidenceLevel: String?,
  val annotatedText: String?,
  val createdDate: LocalDateTime?,
)
