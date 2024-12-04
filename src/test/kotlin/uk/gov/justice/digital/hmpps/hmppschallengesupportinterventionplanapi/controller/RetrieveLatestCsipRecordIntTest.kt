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
import java.time.LocalDate

class RetrieveLatestCsipRecordIntTest : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/prisoners/${prisonNumber()}/csip-records/current").exchange()
      .expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["ROLE_SOME_OTHER"])
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

  @ParameterizedTest
  @ValueSource(strings = [ROLE_CSIP_RO, ROLE_CSIP_UI])
  fun `200 ok - when no csip records`(role: String) {
    val response = getLatestCsipRecord("NEXIST", role = role)

    with(response) {
      assertThat(currentCsip).isNull()
      assertThat(totalOpenedCsipCount).isEqualTo(0)
      assertThat(totalReferralCount).isEqualTo(0)
    }
  }

  @Test
  fun `200 ok - returns matching CSIP OPEN record`() {
    val prisoner = prisoner().toPersonSummary()
    val firstReviewDate = LocalDate.now().plusWeeks(1)
    val current = dataSetup(
      generateCsipRecord(prisoner).withCompletedReferral()
        .withPlan(firstCaseReviewDate = firstReviewDate),
    ) { it }
    dataSetup(generateCsipRecord(prisoner).withCompletedReferral().withPlan()) {
      requireNotNull(it.plan).withReview(actions = setOf(ReviewAction.CLOSE_CSIP))
      it
    }

    val response = getLatestCsipRecord(prisoner.prisonNumber)
    with(response) {
      with(requireNotNull(currentCsip)) {
        assertThat(referralDate).isEqualTo(current.referral?.referralDate)
        assertThat(nextReviewDate).isEqualTo(firstReviewDate)
        assertThat(status.code).isEqualTo(CsipStatus.CSIP_OPEN.name)
        assertThat(status.description).isEqualTo(current.status!!.description)
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
        assertThat(nextReviewDate).isNull()
        assertThat(status.code).isEqualTo(CsipStatus.AWAITING_DECISION.name)
        assertThat(status.description).isEqualTo(current.status!!.description)
      }
      assertThat(totalOpenedCsipCount).isEqualTo(1)
      assertThat(totalReferralCount).isEqualTo(2)
    }
  }

  @Test
  fun `200 ok - returns matching CSIP_CLOSED record`() {
    val prisoner = prisoner().toPersonSummary()
    val current = dataSetup(generateCsipRecord(prisoner).withCompletedReferral().withPlan()) {
      requireNotNull(it.plan).withReview(
        actions = setOf(ReviewAction.CLOSE_CSIP),
        nextReviewDate = LocalDate.now().plusDays(7),
        csipClosedDate = LocalDate.now().minusDays(1),
      )
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
        assertThat(nextReviewDate).isNull()
        assertThat(status.code).isEqualTo(CsipStatus.CSIP_CLOSED.name)
        assertThat(status.description).isEqualTo(current.status!!.description)
        assertThat(closedDate).isEqualTo(current.plan!!.reviews().first().csipClosedDate)
      }
      assertThat(totalOpenedCsipCount).isEqualTo(1)
      assertThat(totalReferralCount).isEqualTo(2)
    }
  }

  @Test
  fun `200 ok - returns no referral date when referral pending`() {
    val prisoner = prisoner().toPersonSummary()
    val current = dataSetup(generateCsipRecord(prisoner).withReferral()) { it }

    val response = getLatestCsipRecord(prisoner.prisonNumber)
    with(response) {
      with(requireNotNull(currentCsip)) {
        assertThat(status.code).isEqualTo(CsipStatus.REFERRAL_PENDING.name)
        assertThat(status.description).isEqualTo(current.status!!.description)
        assertThat(referralDate).isNull()
        assertThat(nextReviewDate).isNull()
      }
      assertThat(totalOpenedCsipCount).isEqualTo(0)
      assertThat(totalReferralCount).isEqualTo(0)
    }
  }

  @Test
  fun `200 ok - returns matching overdue review date`() {
    val prisoner = prisoner().toPersonSummary()
    val overdueReviewDate = LocalDate.now().minusDays(10)
    val current = dataSetup(
      generateCsipRecord(prisoner).withCompletedReferral()
        .withPlan(firstCaseReviewDate = LocalDate.now().minusDays(30)),
    ) {
      requireNotNull(it.plan).withReview(
        actions = setOf(ReviewAction.REMAIN_ON_CSIP),
        nextReviewDate = overdueReviewDate,
      )
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
        assertThat(nextReviewDate).isEqualTo(overdueReviewDate)
        assertThat(status.code).isEqualTo(CsipStatus.CSIP_OPEN.name)
        assertThat(status.description).isEqualTo(current.status!!.description)
        assertThat(reviewOverdueDays).isEqualTo(10)
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
