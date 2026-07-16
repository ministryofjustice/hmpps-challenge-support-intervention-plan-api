package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ALL_PRISONS

@ConfigurationProperties(prefix = "csip.assist")
data class CsipAssistConfig(
  val activePrisons: Set<String>,
) {
  // csip assist to be available in all prisons for dev, but none in pre or prod, to begin with.
  fun resolvedActivePrisons(): Set<String> = if (activePrisons == setOf("***")) ALL_PRISONS else activePrisons
}
