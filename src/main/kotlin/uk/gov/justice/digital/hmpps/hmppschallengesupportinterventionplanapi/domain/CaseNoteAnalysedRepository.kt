package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import java.util.UUID

@Repository
interface CaseNoteAnalysedRepository : JpaRepository<CaseNoteAnalysed, UUID> {
  fun findByPrisonerNumber(prisonerNumber: String): List<CaseNoteAnalysed>

  fun findByPrisonerNumberAndInvestigationIdAndBehaviourType(
    prisonerNumber: String,
    investigationId: UUID,
    behaviourType: BehaviourType,
  ): List<CaseNoteAnalysed>
}
