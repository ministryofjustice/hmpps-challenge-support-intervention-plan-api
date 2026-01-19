package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referencedata.ReferenceData
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
    val referenceData = webTestClient.getReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, null)
    assertThat(referenceData[0].code).isEqualTo("CUR")
    assertThat(referenceData[1].code).isEqualTo("OPE")
    assertThat(referenceData[2].code).isEqualTo("WIN")
    assertThat(referenceData[3].code).isEqualTo("NFA")
  }

  private fun WebTestClient.getReferenceData(
    referenceType: ReferenceDataType,
    includeInactive: Boolean?,
  ) = get().uri { builder ->
    builder.path("/reference-data/${referenceType.domain}")
      .queryParamIfPresent("includeInactive", Optional.ofNullable(includeInactive)).build()
  }.headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).exchange().expectStatus().isOk.expectHeader()
    .contentType(MediaType.APPLICATION_JSON).expectBodyList<ReferenceData>().returnResult().responseBody!!
}
