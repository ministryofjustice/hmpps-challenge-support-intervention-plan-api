package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.jda

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.casenotes.CaseNote

data class JdaRequestData(
  val caseNotes: List<CaseNote>,
)
