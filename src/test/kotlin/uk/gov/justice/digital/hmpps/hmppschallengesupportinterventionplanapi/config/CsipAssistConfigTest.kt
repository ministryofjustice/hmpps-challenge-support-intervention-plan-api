package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ALL_PRISONS

class CsipAssistConfigTest {
  @Test
  fun `resolvedActivePrisons returns all prisons when wildcard configured`() {
    val config = CsipAssistConfig(setOf("***"))

    val result = config.resolvedActivePrisons()

    assertThat(result).isEqualTo(ALL_PRISONS)
  }

  @Test
  fun `resolvedActivePrisons returns configured prisons when not wildcard`() {
    val configuredPrisons = setOf("LEI", "NMI")
    val config = CsipAssistConfig(configuredPrisons)

    val result = config.resolvedActivePrisons()

    assertThat(result).isEqualTo(configuredPrisons)
  }
}
