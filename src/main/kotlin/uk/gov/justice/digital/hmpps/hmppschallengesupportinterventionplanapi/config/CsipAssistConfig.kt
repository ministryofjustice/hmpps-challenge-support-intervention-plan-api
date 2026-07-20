package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "csip.assist")
data class CsipAssistConfig(
  val activePrisons: List<String>,
) {
  fun isActivePrison(prisonId: String): Boolean = if (activePrisons == listOf<String>("***")) true else activePrisons.contains(prisonId)
}
