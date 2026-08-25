package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.toJdaRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.jda.JdaClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipAssistConfig
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CaseNotesLookupRequest

@Service
class JdaService(
  private val caseNotesService: CaseNotesService,
  private val jdaClient: JdaClient,
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
    if (!featureFlag || !csipAssistConfig.isActivePrison(prisonCode)) {
      log.debug("Skipping JDA enqueue for correlationId={}, prisonCode={} (feature disabled or prison not active)", correlationId, prisonCode)
      return
    }

    log.info("Queueing case notes for JDA analysis for correlationId={}, offenderIdentifier={}, prisonCode={}", correlationId, offenderIdentifier, prisonCode)

    val caseNotes =
      caseNotesService.getCaseNotes(
        CaseNotesLookupRequest(
          offenderIdentifier = offenderIdentifier,
        ),
      )

    jdaClient.queueRequest(
      caseNotes.toJdaRequest(correlationId, promptKey, promptVersion),
    )

    log.info("Queued case notes for JDA analysis for correlationId={}", correlationId)
  }
}
