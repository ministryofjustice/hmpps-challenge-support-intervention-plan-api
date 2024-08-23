package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.util.Optional

class ReferenceDataTranslationIntTest : IntegrationTestBase() {
  @Test
  fun `reference data items are bought back in sequence`() {
    val referenceData = webTestClient.getReferenceData(ReferenceDataType.AREA_OF_WORK, null)
    assertThat(referenceData).isNotEmpty()
    assertThat(referenceData.map { it.listSequence }).isSorted()
  }

  @Test
  fun `reference data for outcome type are bought back in the correct sequence`() {
    val referenceData = webTestClient.getReferenceData(ReferenceDataType.OUTCOME_TYPE, null)
    assertThat(referenceData[0].code).isEqualTo("CUR")
    assertThat(referenceData[1].code).isEqualTo("OPE")
    assertThat(referenceData[2].code).isEqualTo("WIN")
    assertThat(referenceData[3].code).isEqualTo("ACC")
    assertThat(referenceData[4].code).isEqualTo("NFA")
  }

  private fun WebTestClient.getReferenceData(
    referenceType: ReferenceDataType,
    includeInactive: Boolean?,
  ) = get().uri { builder ->
    builder.path("/reference-data/${referenceType.domain}")
      .queryParamIfPresent("includeInactive", Optional.ofNullable(includeInactive)).build()
  }.headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).exchange().expectStatus().isOk.expectHeader()
    .contentType(MediaType.APPLICATION_JSON).expectBodyList(ReferenceData::class.java).returnResult().responseBody!!
}
