package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.toJdaRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipAssistConfig
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest
import java.util.UUID

@Service
class JdaService(
  private val caseNotesService: CaseNotesService,
  private val jdaClient: JdaClient,
  private val caseNoteAnnotationsService: CaseNoteAnnotationsService,
  private val csipRecordService: CsipRecordService,
  private val csipAssistConfig: CsipAssistConfig,
  @Value("\${jda.prompt-key}")
  private val promptKey: String,
  @Value("\${jda.prompt-version:1}")
  private val promptVersion: Int,
  @Value("\${feature.csip-assist}")
  private val featureFlag: Boolean,
) {

  private companion object {
    private val log: Logger = LoggerFactory.getLogger(this::class.java)
  }

  fun submitCaseNotesForAnalysis(
    offenderIdentifier: String,
    prisonCode: String,
    correlationId: String,
  ) {
    if (!featureFlag || !csipAssistConfig.isActivePrison(prisonCode)) return

    val caseNotes =
      caseNotesService.getCaseNotes(
        CaseNotesLookupRequest(
          offenderIdentifier = offenderIdentifier,
        ),
      )

    val response = jdaClient.submitRequest(
      caseNotes.toJdaRequest(correlationId, promptKey, promptVersion),
    )

    // Persist annotations immediately from the synchronous response
    try {
      val correlationUuid = UUID.fromString(correlationId)
      val prisonerNumber = csipRecordService.retrieveCsipRecord(correlationUuid).prisonNumber
      caseNoteAnnotationsService.persistSynchronousAnnotations(response, prisonerNumber)
    } catch (e: Exception) {
      log.error("Failed to persist annotations from submitRequest response for correlation ID $correlationId", e)
      // Continue without throwing - annotation persistence should not block the main flow
      // but log the error for operational awareness
    }
  }
}
