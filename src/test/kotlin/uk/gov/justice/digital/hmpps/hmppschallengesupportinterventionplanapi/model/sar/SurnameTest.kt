package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.sar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

class SurnameTest {

  @ParameterizedTest(name = "\"{0}\" -> \"{1}\"")
  @CsvSource(
    "John Smith, Smith",
    "Mary Jane Smith, Smith",
    "'  John   Smith  ', Smith",
    "JOHN SMITH, Smith",
    "JANE O'NEILL, O'Neill",
    "PAT SCOTT-GREEN, Scott-Green",
    "john smith, Smith",
    "Ian McDonald, McDonald",
    "Smith, Smith",
  )
  fun `takes the last word and gives names in one case ordinary capitalisation`(displayName: String, expected: String) {
    assertThat(surname(displayName)).isEqualTo(expected)
  }

  @Test
  fun `nothing recorded gives nothing`() {
    assertThat(surname(null)).isNull()
    assertThat(surname("   ")).isNull()
  }
}
