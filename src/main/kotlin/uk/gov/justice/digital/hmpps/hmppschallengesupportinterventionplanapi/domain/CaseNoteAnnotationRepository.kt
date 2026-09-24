package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import java.util.UUID

@Repository
interface CaseNoteAnnotationRepository : JpaRepository<CaseNoteAnnotation, UUID> {
  @Query(
    """
    select a from CaseNoteAnnotation a
    join a.caseNotesAnalysed cna
    where cna.prisonerNumber = :prisonerNumber
      and a.behaviourType = :behaviourType
    order by a.createdDate desc
    """,
  )
  fun findByPrisonerNumberAndBehaviourType(
    @Param("prisonerNumber") prisonerNumber: String,
    @Param("behaviourType") behaviourType: BehaviourType,
  ): List<CaseNoteAnnotation>

  fun findByCaseNoteIdAndBehaviourType(caseNoteId: UUID, behaviourType: BehaviourType): List<CaseNoteAnnotation>

  @Query(
    """
    select a from CaseNoteAnnotation a
    where a.caseNotesAnalysed.id in :caseNotesAnalysedIds
      and a.behaviourType = :behaviourType
    order by a.createdDate desc
    """,
  )
  fun findByCaseNotesAnalysedIdInAndBehaviourType(
    @Param("caseNotesAnalysedIds") caseNotesAnalysedIds: Collection<UUID>,
    @Param("behaviourType") behaviourType: BehaviourType,
  ): List<CaseNoteAnnotation>
}
