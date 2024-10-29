package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipCounts
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipOverview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import java.time.LocalDate
import kotlin.random.Random

private const val OVERVIEW_PRISON_CODE = "OVE"

class CsipOverviewIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.get().uri(urlToTest(OVERVIEW_PRISON_CODE)).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no required role`() {
    val response = getCsipOverviewResponseSpec(role = "WRONG_ROLE").errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - when no csip record exists for prison`() {
    val response = getCsipOverview("NON_EXISTENT")
    assertThat(response.counts).isEqualTo(CsipCounts.NONE)
  }

  @Test
  fun `200 ok - correctly counts csip records for overview`() {
    val referralsSubmitted = (0..Random(10).nextInt()).map { referralSubmitted() }
    val pendingInvestigations = (0..Random(10).nextInt()).map { pendingInvestigation() }
    val awaitingDecisions = (0..Random(10).nextInt()).map { awaitingDecision() }
    val plansPending = (0..Random(10).nextInt()).map { pendingPlan() }
    val openPlans = (0..Random(10).nextInt()).map { openPlan() }
    val reviewsOverdue = (0..Random(10).nextInt()).map { reviewOverdue() }

    val response = getCsipOverview()
    assertThat(response.counts).isEqualTo(
      CsipCounts(
        submittedReferrals = referralsSubmitted.size.toLong(),
        pendingInvestigations = pendingInvestigations.size.toLong(),
        awaitingDecisions = awaitingDecisions.size.toLong(),
        pendingPlans = plansPending.size.toLong(),
        open = openPlans.size.toLong(),
        overdueReviews = reviewsOverdue.size.toLong(),
      ),
    )
    assertThat(response.counts.overdueReviews).isGreaterThanOrEqualTo(response.counts.open)
  }

  private fun getCsipOverviewResponseSpec(
    prisonCode: String = OVERVIEW_PRISON_CODE,
    role: String = ROLE_CSIP_UI,
  ): WebTestClient.ResponseSpec =
    webTestClient.get().uri(urlToTest(prisonCode))
      .headers(setAuthorisation(roles = listOfNotNull(role)))
      .exchange()

  private fun getCsipOverview(prisonCode: String = "OVE", role: String = ROLE_CSIP_UI): CsipOverview =
    getCsipOverviewResponseSpec(prisonCode, role).successResponse()

  private fun urlToTest(prisonCode: String) = "/prisons/$prisonCode/csip-records/overview"

  private fun csip() =
    generateCsipRecord(prisoner(prisonId = OVERVIEW_PRISON_CODE).toPersonSummary(), OVERVIEW_PRISON_CODE)

  private fun referralSubmitted() = dataSetup(csip()) { it.withCompletedReferral() }

  private fun pendingInvestigation(): CsipRecord {
    val screeningOutcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE")
    return dataSetup(csip()) {
      it.withCompletedReferral()
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(screeningOutcome)
      it
    }
  }

  private fun awaitingDecision(): CsipRecord {
    val screeningOutcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE")
    return dataSetup(csip()) {
      it.withCompletedReferral()
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(screeningOutcome).withInvestigation()
      it
    }
  }

  private fun pendingPlan(): CsipRecord {
    val screeningOutcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "OPE")
    val decisionOutcome = givenReferenceData(ReferenceDataType.DECISION_OUTCOME_TYPE, "CUR")
    return dataSetup(csip()) {
      it.withCompletedReferral()
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(screeningOutcome)
        .withInvestigation().withDecisionAndActions(decisionOutcome)
      it
    }
  }

  private fun openPlan(): CsipRecord {
    val screeningOutcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "CUR")
    return dataSetup(csip()) {
      it.withCompletedReferral()
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(screeningOutcome)
      it.withPlan()
    }
  }

  private fun reviewOverdue(): CsipRecord {
    val screeningOutcome = givenReferenceData(ReferenceDataType.SCREENING_OUTCOME_TYPE, "CUR")
    return dataSetup(csip()) {
      it.withCompletedReferral(
        referralDate = LocalDate.now().minusMonths(2),
        referralCompletedDate = LocalDate.now().minusMonths(2),
      )
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(screeningOutcome)
      it.withPlan(firstCaseReviewDate = LocalDate.now().minusMonths(1))
      requireNotNull(it.plan).withReview(nextReviewDate = LocalDate.now().minusDays(1))
      it
    }
  }
}
