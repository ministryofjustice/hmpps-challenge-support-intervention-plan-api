package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnalysedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CaseNoteAnnotationRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.BehaviourType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaDequeueResponseData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaMetadata
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaPrompt
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JdaRequestType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda.JustifyingSpan
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.UUID

class CaseNoteAnnotationsPersistenceIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var caseNoteAnnotationsService: CaseNoteAnnotationsService

  @Autowired
  lateinit var caseNoteAnalysedRepository: CaseNoteAnalysedRepository

  @Autowired
  lateinit var caseNoteAnnotationRepository: CaseNoteAnnotationRepository

  @BeforeEach
  fun cleanCaseNoteTables() {
    caseNoteAnnotationRepository.deleteAll()
    caseNoteAnalysedRepository.deleteAll()
    entityManager.clear()
  }

  @Test
  fun `persistSynchronousAnnotations updates existing analysed row and replaces child annotations for matching investigation and case note`() {
    val investigationId = UUID.randomUUID()
    val caseNoteId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"
    val originalRequestId = UUID.randomUUID()
    val latestRequestId = UUID.randomUUID()
    val originalAnalysed = caseNoteAnalysedRepository.saveAndFlush(
      CaseNoteAnalysed(
        requestId = originalRequestId,
        investigationId = investigationId,
        prisonerNumber = prisonerNumber,
        caseNoteId = caseNoteId,
        promptKey = "old-prompt",
        promptVersion = 1,
        usualBehaviourRelevancy = 4,
        risksAndTriggersRelevancy = 1,
        protectiveFactorsRelevancy = 0,
      ),
    )
    caseNoteAnnotationRepository.saveAll(
      listOf(
        CaseNoteAnnotation(
          caseNotesAnalysed = originalAnalysed,
          requestId = originalRequestId,
          investigationId = investigationId,
          caseNoteId = caseNoteId,
          behaviourType = BehaviourType.RISKS_AND_TRIGGERS,
          annotatedText = "old annotation text",
          createdDate = LocalDateTime.now().minusDays(1),
        ),
        CaseNoteAnnotation(
          caseNotesAnalysed = originalAnalysed,
          requestId = originalRequestId,
          investigationId = investigationId,
          caseNoteId = caseNoteId,
          behaviourType = BehaviourType.PROTECTIVE_FACTORS,
          annotatedText = "old protective annotation",
          createdDate = LocalDateTime.now().minusHours(12),
        ),
      ),
    )
    caseNoteAnnotationRepository.flush()
    entityManager.clear()

    assertThat(countAnalysedRows(investigationId, caseNoteId)).isEqualTo(1)
    assertThat(findAnnotationTexts(investigationId, caseNoteId))
      .containsExactlyInAnyOrder("old annotation text", "old protective annotation")

    val response = JdaRequestResponse(
      requestId = latestRequestId,
      correlationId = investigationId,
      prompt = JdaPrompt(
        key = "case-note-analysis",
        version = 3,
      ),
      status = JdaRequestStatus.SUCCEEDED,
      responseData = listOf(
        JdaDequeueResponseData(
          caseNoteId = caseNoteId,
          usualBehaviourPresentation = 1,
          risksAndTriggers = 5,
          protectiveFactors = 2,
          justifyingSpans = listOf(
            JustifyingSpan(
              text = "latest risk annotation",
              justifies = BehaviourType.RISKS_AND_TRIGGERS,
            ),
            JustifyingSpan(
              text = "latest protective annotation",
              justifies = BehaviourType.PROTECTIVE_FACTORS,
            ),
          ),
        ),
      ),
      metaData = JdaMetadata(
        requestType = JdaRequestType.SYNC,
        submittedAt = OffsetDateTime.now(),
      ),
    )

    caseNoteAnnotationsService.persistSynchronousAnnotations(response, prisonerNumber)
    entityManager.clear()

    assertThat(countAnalysedRows(investigationId, caseNoteId)).isEqualTo(1)

    val persistedAnalysed = requireNotNull(
      caseNoteAnalysedRepository.findByInvestigationIdAndCaseNoteId(investigationId, caseNoteId),
    )
    assertThat(persistedAnalysed.id).isEqualTo(originalAnalysed.id)
    assertThat(persistedAnalysed.requestId).isEqualTo(latestRequestId)
    assertThat(persistedAnalysed.promptKey).isEqualTo("case-note-analysis")
    assertThat(persistedAnalysed.promptVersion).isEqualTo(3)
    assertThat(persistedAnalysed.usualBehaviourRelevancy).isEqualTo(1)
    assertThat(persistedAnalysed.risksAndTriggersRelevancy).isEqualTo(5)
    assertThat(persistedAnalysed.protectiveFactorsRelevancy).isEqualTo(2)

    val persistedAnnotations = findPersistedAnnotations(investigationId, caseNoteId)
    assertThat(persistedAnnotations).hasSize(2)
    assertThat(persistedAnnotations.mapNotNull { it.annotatedText })
      .containsExactlyInAnyOrder("latest risk annotation", "latest protective annotation")
      .doesNotContain("old annotation text", "old protective annotation")
    assertThat(persistedAnnotations.map { it.caseNotesAnalysed.id }.distinct()).containsExactly(originalAnalysed.id)
    assertThat(persistedAnnotations.map { it.requestId }.distinct()).containsExactly(latestRequestId)
  }

  @Test
  fun `deleteCaseNote removes only deleted case note rows from a pending investigation`() {
    val investigationId = UUID.randomUUID()
    val deletedCaseNoteId = UUID.randomUUID()
    val remainingCaseNoteIdOne = UUID.randomUUID()
    val remainingCaseNoteIdTwo = UUID.randomUUID()
    val prisonerNumber = "A1234BC"

    seedAnalysedWithAnnotations(
      investigationId = investigationId,
      caseNoteId = deletedCaseNoteId,
      prisonerNumber = prisonerNumber,
      annotationTexts = listOf("deleted base text", "deleted amendment text"),
    )
    seedAnalysedWithAnnotations(
      investigationId = investigationId,
      caseNoteId = remainingCaseNoteIdOne,
      prisonerNumber = prisonerNumber,
      annotationTexts = listOf("remaining one base text", "remaining one amendment text"),
    )
    seedAnalysedWithAnnotations(
      investigationId = investigationId,
      caseNoteId = remainingCaseNoteIdTwo,
      prisonerNumber = prisonerNumber,
      annotationTexts = listOf("remaining two base text", "remaining two amendment text"),
    )
    entityManager.clear()

    val deleted = caseNoteAnnotationsService.deleteCaseNote(deletedCaseNoteId, listOf(investigationId))
    entityManager.clear()

    assertThat(deleted).isTrue()
    assertThat(countAnalysedRows(investigationId, deletedCaseNoteId)).isZero()
    assertThat(findPersistedAnnotations(investigationId, deletedCaseNoteId)).isEmpty()

    assertThat(countAnalysedRows(investigationId, remainingCaseNoteIdOne)).isEqualTo(1)
    assertThat(findAnnotationTexts(investigationId, remainingCaseNoteIdOne))
      .containsExactlyInAnyOrder("remaining one base text", "remaining one amendment text")

    assertThat(countAnalysedRows(investigationId, remainingCaseNoteIdTwo)).isEqualTo(1)
    assertThat(findAnnotationTexts(investigationId, remainingCaseNoteIdTwo))
      .containsExactlyInAnyOrder("remaining two base text", "remaining two amendment text")
  }

  @Test
  fun `deleteCaseNote removes deleted case note from each pending investigation and keeps other notes`() {
    val pendingInvestigationIdOne = UUID.randomUUID()
    val pendingInvestigationIdTwo = UUID.randomUUID()
    val deletedCaseNoteId = UUID.randomUUID()
    val survivorCaseNoteIdOne = UUID.randomUUID()
    val survivorCaseNoteIdTwo = UUID.randomUUID()
    val prisonerNumber = "A1234BC"

    seedAnalysedWithAnnotations(
      investigationId = pendingInvestigationIdOne,
      caseNoteId = deletedCaseNoteId,
      prisonerNumber = prisonerNumber,
      annotationTexts = listOf("pending one deleted base", "pending one deleted amendment"),
    )
    seedAnalysedWithAnnotations(
      investigationId = pendingInvestigationIdTwo,
      caseNoteId = deletedCaseNoteId,
      prisonerNumber = prisonerNumber,
      annotationTexts = listOf("pending two deleted base", "pending two deleted amendment"),
    )
    seedAnalysedWithAnnotations(
      investigationId = pendingInvestigationIdOne,
      caseNoteId = survivorCaseNoteIdOne,
      prisonerNumber = prisonerNumber,
      annotationTexts = listOf("survivor one base", "survivor one amendment"),
    )
    seedAnalysedWithAnnotations(
      investigationId = pendingInvestigationIdTwo,
      caseNoteId = survivorCaseNoteIdTwo,
      prisonerNumber = prisonerNumber,
      annotationTexts = listOf("survivor two base", "survivor two amendment"),
    )
    entityManager.clear()

    val deleted = caseNoteAnnotationsService.deleteCaseNote(
      deletedCaseNoteId,
      listOf(pendingInvestigationIdOne, pendingInvestigationIdTwo),
    )
    entityManager.clear()

    assertThat(deleted).isTrue()
    assertThat(countAnalysedRows(pendingInvestigationIdOne, deletedCaseNoteId)).isZero()
    assertThat(countAnalysedRows(pendingInvestigationIdTwo, deletedCaseNoteId)).isZero()
    assertThat(findPersistedAnnotations(pendingInvestigationIdOne, deletedCaseNoteId)).isEmpty()
    assertThat(findPersistedAnnotations(pendingInvestigationIdTwo, deletedCaseNoteId)).isEmpty()

    assertThat(countAnalysedRows(pendingInvestigationIdOne, survivorCaseNoteIdOne)).isEqualTo(1)
    assertThat(findAnnotationTexts(pendingInvestigationIdOne, survivorCaseNoteIdOne))
      .containsExactlyInAnyOrder("survivor one base", "survivor one amendment")

    assertThat(countAnalysedRows(pendingInvestigationIdTwo, survivorCaseNoteIdTwo)).isEqualTo(1)
    assertThat(findAnnotationTexts(pendingInvestigationIdTwo, survivorCaseNoteIdTwo))
      .containsExactlyInAnyOrder("survivor two base", "survivor two amendment")
  }

  @Test
  fun `deleteCaseNote returns false and preserves data when target case note is not stored in pending investigation`() {
    val pendingInvestigationId = UUID.randomUUID()
    val targetCaseNoteId = UUID.randomUUID()
    val existingCaseNoteId = UUID.randomUUID()
    val prisonerNumber = "A1234BC"

    seedAnalysedWithAnnotations(
      investigationId = pendingInvestigationId,
      caseNoteId = existingCaseNoteId,
      prisonerNumber = prisonerNumber,
      annotationTexts = listOf("existing base", "existing amendment"),
    )
    entityManager.clear()

    val deleted = caseNoteAnnotationsService.deleteCaseNote(targetCaseNoteId, listOf(pendingInvestigationId))
    entityManager.clear()

    assertThat(deleted).isFalse()
    assertThat(countAnalysedRows(pendingInvestigationId, targetCaseNoteId)).isZero()
    assertThat(findPersistedAnnotations(pendingInvestigationId, targetCaseNoteId)).isEmpty()
    assertThat(countAnalysedRows(pendingInvestigationId, existingCaseNoteId)).isEqualTo(1)
    assertThat(findAnnotationTexts(pendingInvestigationId, existingCaseNoteId))
      .containsExactlyInAnyOrder("existing base", "existing amendment")
  }

  private fun seedAnalysedWithAnnotations(
    investigationId: UUID,
    caseNoteId: UUID,
    prisonerNumber: String,
    annotationTexts: List<String>,
  ) {
    val analysed = caseNoteAnalysedRepository.saveAndFlush(
      CaseNoteAnalysed(
        requestId = UUID.randomUUID(),
        investigationId = investigationId,
        prisonerNumber = prisonerNumber,
        caseNoteId = caseNoteId,
        promptKey = "case-note-analysis",
        promptVersion = 3,
        usualBehaviourRelevancy = 2,
        risksAndTriggersRelevancy = 2,
        protectiveFactorsRelevancy = 2,
      ),
    )

    caseNoteAnnotationRepository.saveAll(
      annotationTexts.mapIndexed { index, text ->
        CaseNoteAnnotation(
          caseNotesAnalysed = analysed,
          requestId = analysed.requestId,
          investigationId = investigationId,
          caseNoteId = caseNoteId,
          behaviourType = if (index % 2 == 0) BehaviourType.RISKS_AND_TRIGGERS else BehaviourType.PROTECTIVE_FACTORS,
          annotatedText = text,
          createdDate = LocalDateTime.now().minusMinutes(index.toLong()),
        )
      },
    )
    caseNoteAnnotationRepository.flush()
  }

  private fun countAnalysedRows(investigationId: UUID, caseNoteId: UUID): Long = requireNotNull(
    transactionTemplate.execute {
      entityManager.createQuery(
        """
      select count(c)
      from CaseNoteAnalysed c
      where c.investigationId = :investigationId
        and c.caseNoteId = :caseNoteId
        """.trimIndent(),
        Number::class.java,
      )
        .setParameter("investigationId", investigationId)
        .setParameter("caseNoteId", caseNoteId)
        .singleResult
        .toLong()
    },
  )

  private fun findPersistedAnnotations(investigationId: UUID, caseNoteId: UUID): List<CaseNoteAnnotation> = requireNotNull(
    transactionTemplate.execute {
      entityManager.createQuery(
        """
      select a
      from CaseNoteAnnotation a
      where a.investigationId = :investigationId
        and a.caseNoteId = :caseNoteId
      order by a.annotatedText asc
        """.trimIndent(),
        CaseNoteAnnotation::class.java,
      )
        .setParameter("investigationId", investigationId)
        .setParameter("caseNoteId", caseNoteId)
        .resultList
    },
  )

  private fun findAnnotationTexts(investigationId: UUID, caseNoteId: UUID): List<String> = findPersistedAnnotations(investigationId, caseNoteId).mapNotNull { it.annotatedText }
}
