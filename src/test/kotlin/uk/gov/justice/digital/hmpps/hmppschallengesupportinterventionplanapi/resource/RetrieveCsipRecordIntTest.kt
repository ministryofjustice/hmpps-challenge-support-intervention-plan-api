package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.util.UUID

class RetrieveCsipRecordIntTest : IntegrationTestBase() {
  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = getCsipRecordResponseSpec(UUID.randomUUID(), role).errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}").exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `404 not found - when no csip record exists with matching uuid`() {
    val notExistingUuid = UUID.randomUUID()
    val response = getCsipRecordResponseSpec(notExistingUuid).errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $notExistingUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CSIP_UI, ROLE_NOMIS])
  fun `200 ok - returns matching CSIP record`(role: String) {
    val prisonNumber = givenValidPrisonNumber("G1234CR")
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))

    val response = getCsipRecord(record.recordUuid, role)
    with(response) {
      assertThat(prisonNumber).isEqualTo(prisonNumber)
      assertThat(recordUuid).isEqualTo(record.recordUuid)
    }
  }

  fun getCsipRecordResponseSpec(recordUuid: UUID, role: String? = ROLE_CSIP_UI): WebTestClient.ResponseSpec =
    webTestClient.get()
      .uri("/csip-records/$recordUuid")
      .headers(setAuthorisation(roles = listOfNotNull(role)))
      .exchange()

  fun getCsipRecord(recordUuid: UUID, role: String = ROLE_CSIP_UI): CsipRecord =
    getCsipRecordResponseSpec(recordUuid, role).successResponse()
}
