package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.verifyAgainst
import java.time.LocalDate
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
    val record = dataSetup(generateCsipRecord().withCompletedReferral().withPlan()) {
      val referral = requireNotNull(it.referral).withContributoryFactor()
        .withSaferCustodyScreeningOutcome().withInvestigation().withDecisionAndActions()
      requireNotNull(referral.investigation).withInterview("A N Other")
      val plan = requireNotNull(it.plan).withNeed("One need").withNeed("Another need")
        .withReview(LocalDate.now().minusDays(1))
        .withReview(actions = setOf(ReviewAction.REMAIN_ON_CSIP))
      requireNotNull(plan.reviews().random()).withAttendee()
      it
    }

    val response = getCsipRecord(record.id, role)
    response.verifyAgainst(record)
  }

  fun getCsipRecordResponseSpec(recordUuid: UUID, role: String? = ROLE_CSIP_UI): WebTestClient.ResponseSpec = webTestClient.get()
    .uri("/csip-records/$recordUuid")
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .exchange()

  fun getCsipRecord(recordUuid: UUID, role: String = ROLE_CSIP_UI): CsipRecord = getCsipRecordResponseSpec(recordUuid, role).successResponse()
}
