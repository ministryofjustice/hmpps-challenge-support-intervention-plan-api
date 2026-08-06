package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

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
  @Value("\${jda.prompt-version:0}")
  private val promptVersion: Int,
  @Value("\${feature.csip-assist}")
  private val featureFlag: Boolean,
) {

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

    jdaClient.submitRequest(
      caseNotes.toJdaRequest(correlationId, promptKey, promptVersion),
    )
  }
}
