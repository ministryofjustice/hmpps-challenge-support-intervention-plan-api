package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CsipAssistConfigTest {
  @Test
  fun `resolvedActivePrisons returns all prisons when wildcard configured`() {
    val config = CsipAssistConfig(listOf("***"))
    val result = config.isActivePrison("MDI")
    assertThat(result).isTrue()
  }

  @Test
  fun `resolvedActivePrisons returns configured prisons when not wildcard`() {
    val configuredPrisons = listOf("LEI", "NMI")
    val config = CsipAssistConfig(configuredPrisons)
    val result = config.isActivePrison("LEI")
    assertThat(result).isTrue()
  }

  @Test
  fun `isActivePrison returns false when prison is not configured`() {
    val config = CsipAssistConfig(listOf("LEI", "NMI"))
    val result = config.isActivePrison("MDI")
    assertThat(result).isFalse()
  }
}
