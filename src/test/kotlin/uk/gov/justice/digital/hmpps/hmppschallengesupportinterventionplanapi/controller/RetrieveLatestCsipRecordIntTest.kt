package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_RO
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.SCREENING_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CurrentCsipDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner

class RetrieveLatestCsipRecordIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/prisoners/${prisonNumber()}/csip-records/current").exchange()
      .expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["ROLE_SOME_OTHER", ROLE_CSIP_UI])
  fun `403 forbidden - no required role`(role: String?) {
    val response = getLatestCsipResponseSpec(prisonNumber(), role).errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - when no csip records`() {
    val response = getLatestCsipRecord("NEXIST")

    with(response) {
      assertThat(currentCsip).isNull()
      assertThat(totalOpenedCsipCount).isEqualTo(0)
      assertThat(totalReferralCount).isEqualTo(0)
    }
  }

  @Test
  fun `200 ok - returns matching CSIP OPEN record`() {
    val prisoner = prisoner().toPersonSummary()
    val current = dataSetup(generateCsipRecord(prisoner).withCompletedReferral().withPlan()) { it }
    dataSetup(generateCsipRecord(prisoner).withCompletedReferral().withPlan()) {
      requireNotNull(it.plan).withReview(actions = setOf(ReviewAction.CLOSE_CSIP))
      it
    }

    val response = getLatestCsipRecord(prisoner.prisonNumber)
    with(response) {
      with(requireNotNull(currentCsip)) {
        assertThat(referralDate).isEqualTo(current.referral?.referralDate)
        assertThat(nextReviewDate).isEqualTo(current.plan?.firstCaseReviewDate)
        assertThat(status).isEqualTo(CsipStatus.CSIP_OPEN)
      }
      assertThat(totalOpenedCsipCount).isEqualTo(2)
      assertThat(totalReferralCount).isEqualTo(2)
    }
  }

  @Test
  fun `200 ok - returns matching AWAITING DECISION record`() {
    val prisoner = prisoner().toPersonSummary()
    val current = dataSetup(generateCsipRecord(prisoner).withCompletedReferral()) {
      requireNotNull(it.referral).withInvestigation()
      it
    }
    dataSetup(generateCsipRecord(prisoner).withCompletedReferral().withPlan()) {
      requireNotNull(it.plan).withReview(actions = setOf(ReviewAction.CLOSE_CSIP))
      it
    }

    val response = getLatestCsipRecord(prisoner.prisonNumber)
    with(response) {
      with(requireNotNull(currentCsip)) {
        assertThat(referralDate).isEqualTo(current.referral?.referralDate)
        assertThat(nextReviewDate).isEqualTo(current.plan?.firstCaseReviewDate)
        assertThat(status).isEqualTo(CsipStatus.AWAITING_DECISION)
      }
      assertThat(totalOpenedCsipCount).isEqualTo(1)
      assertThat(totalReferralCount).isEqualTo(2)
    }
  }

  @Test
  fun `200 ok - returns matching CSIP_CLOSED record`() {
    val prisoner = prisoner().toPersonSummary()
    val current = dataSetup(generateCsipRecord(prisoner).withCompletedReferral().withPlan()) {
      requireNotNull(it.plan).withReview(actions = setOf(ReviewAction.CLOSE_CSIP))
      it
    }
    dataSetup(generateCsipRecord(prisoner).withCompletedReferral()) {
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(SCREENING_OUTCOME_TYPE, "NFA"),
      )
      it
    }

    val response = getLatestCsipRecord(prisoner.prisonNumber)
    with(response) {
      with(requireNotNull(currentCsip)) {
        assertThat(referralDate).isEqualTo(current.referral?.referralDate)
        assertThat(nextReviewDate).isEqualTo(current.plan?.nextReviewDate())
        assertThat(status).isEqualTo(CsipStatus.CSIP_CLOSED)
      }
      assertThat(totalOpenedCsipCount).isEqualTo(1)
      assertThat(totalReferralCount).isEqualTo(2)
    }
  }

  fun getLatestCsipResponseSpec(prisonNumber: String, role: String? = ROLE_CSIP_RO): WebTestClient.ResponseSpec =
    webTestClient.get()
      .uri("/prisoners/$prisonNumber/csip-records/current")
      .headers(setAuthorisation(roles = listOfNotNull(role)))
      .exchange()

  fun getLatestCsipRecord(prisonNumber: String, role: String = ROLE_CSIP_RO): CurrentCsipDetail =
    getLatestCsipResponseSpec(prisonNumber, role).successResponse()
}
