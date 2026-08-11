package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface CaseNoteAnnotationRepository : JpaRepository<CaseNoteAnnotation, UUID> {
  fun findByPrisonerNumberAndBehaviourType(prisonerNumber: String, behaviourType: String): List<CaseNoteAnnotation>
}
