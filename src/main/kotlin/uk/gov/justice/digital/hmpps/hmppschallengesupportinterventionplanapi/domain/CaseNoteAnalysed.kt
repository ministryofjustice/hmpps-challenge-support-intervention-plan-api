package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import java.util.UUID

@Entity
@Table(name = "case_notes_analysed")
class CaseNoteAnalysed(
  @Id
  val id: UUID = newUuid(),

  val requestId: UUID,

  val investigationId: UUID,

  @Column(nullable = false, length = 10)
  val prisonerNumber: String,

  val caseNoteId: UUID,

  val promptKey: String?,

  val promptVersion: Int?,

  @Column(nullable = false)
  val usualBehaviourRelevancy: Int = 0,

  @Column(nullable = false)
  val risksAndTriggersRelevancy: Int = 0,

  @Column(nullable = false)
  val protectiveFactorsRelevancy: Int = 0,
) {
  @OneToMany(mappedBy = "caseNotesAnalysed", fetch = FetchType.LAZY)
  val annotations: MutableList<CaseNoteAnnotation> = mutableListOf()

  fun relevancyFor(behaviourType: BehaviourType): Int = when (behaviourType) {
    BehaviourType.USUAL_BEHAVIOUR_PRESENTATION -> usualBehaviourRelevancy
    BehaviourType.RISKS_AND_TRIGGERS -> risksAndTriggersRelevancy
    BehaviourType.PROTECTIVE_FACTORS -> protectiveFactorsRelevancy
  }
}
