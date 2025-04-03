package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.getReview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.IDENTIFIED_NEED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.PLAN
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction.CLOSE_CSIP
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction.RESPONSIBLE_PEOPLE_INFORMED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.ReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.UpdateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.verifyAgainst
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.util.SortedSet
import java.util.UUID
import java.util.UUID.randomUUID
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Review as ReviewEntity

class UpdateReviewIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var reviewRepository: ReviewRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch().uri(urlToTest(randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE", ROLE_NOMIS])
  fun `403 forbidden - no required role`(role: String?) {
    val response = updateReviewResponseSpec(randomUUID(), updateReviewRequest(), role = role)
      .errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = updateReviewResponseSpec(
      randomUUID(),
      updateReviewRequest(),
      username = USER_NOT_FOUND,
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - close csip without closed date`() {
    val response = updateReviewResponseSpec(
      randomUUID(),
      updateReviewRequest(actions = sortedSetOf(CLOSE_CSIP), csipClosedDate = null),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Closing a CSIP record requires both the action and closed date to be provided.")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: Closing a CSIP record requires both the action and closed date to be provided.")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - close date without action to close`() {
    val response = updateReviewResponseSpec(
      randomUUID(),
      updateReviewRequest(actions = sortedSetOf(ReviewAction.REMAIN_ON_CSIP), csipClosedDate = LocalDate.now()),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Closing a CSIP record requires both the action and closed date to be provided.")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: Closing a CSIP record requires both the action and closed date to be provided.")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `404 not found - review not found`() {
    val uuid = randomUUID()
    val response = updateReviewResponseSpec(uuid, updateReviewRequest())
      .errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Review not found")
      assertThat(developerMessage).isEqualTo("Review not found with identifier $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - review updated`() {
    val review = dataSetup(generateCsipRecord().withPlan()) {
      val plan = requireNotNull(it.plan).withReview()
      plan.reviews().first()
    }

    val request = updateReviewRequest(
      summary = "a summary goes here",
      actions = sortedSetOf(RESPONSIBLE_PEOPLE_INFORMED),
      reviewDate = LocalDate.now().plusDays(3),
      nextReviewDate = LocalDate.now().plusDays(4),
    )
    val response = updateReview(review.id, request)

    val saved = getReview(response.reviewUuid)
    saved.verifyAgainst(request)
    assertThat(saved.plan.nextReviewDate).isEqualTo(saved.nextReviewDate)
    assertThat(saved.plan.closedDate).isNull()

    verifyAudit(saved, RevisionType.MOD, setOf(REVIEW))
    verifyDomainEvents(review.csipRecord().prisonNumber, review.csipRecord().id, CSIP_UPDATED)
  }

  @Test
  fun `200 ok - review updated to close csip, closes open needs`() {
    val review = dataSetup(generateCsipRecord().withPlan()) {
      val plan = requireNotNull(it.plan).withNeed().withReview()
      plan.reviews().first()
    }

    val request = updateReviewRequest(
      summary = "a summary goes here",
      actions = sortedSetOf(CLOSE_CSIP),
      reviewDate = LocalDate.now().minusDays(10),
      nextReviewDate = LocalDate.now().minusDays(1),
      csipClosedDate = LocalDate.now(),
    )
    val response = updateReview(review.id, request)

    val saved = getReview(response.reviewUuid)
    saved.verifyAgainst(request)
    assertThat(saved.plan.nextReviewDate).isNull()
    assertThat(saved.plan.closedDate).isEqualTo(saved.csipClosedDate)

    val need = saved.plan.identifiedNeeds().first()
    assertThat(need.closedDate).isEqualTo(saved.csipClosedDate)

    verifyAudit(saved, RevisionType.MOD, setOf(REVIEW, IDENTIFIED_NEED))
    verifyAudit(need, RevisionType.MOD, setOf(REVIEW, IDENTIFIED_NEED))
    verifyDomainEvents(review.csipRecord().prisonNumber, review.csipRecord().id, CSIP_UPDATED)
  }

  @Test
  fun `200 ok - review not updated with no change`() {
    val review = dataSetup(generateCsipRecord().withPlan()) {
      val plan = requireNotNull(it.plan).withReview(
        reviewDate = LocalDate.now(),
        recordedBy = TEST_USER,
        recordedByDisplayName = TEST_USER_NAME,
        nextReviewDate = LocalDate.now().plusWeeks(4),
        csipClosedDate = null,
        summary = "A brief summary of the review",
        actions = sortedSetOf(),
      )
      plan.reviews().first()
    }

    val request = updateReviewRequest(
      summary = review.summary,
      actions = review.actions,
      reviewDate = review.reviewDate,
      nextReviewDate = review.nextReviewDate,
      csipClosedDate = review.csipClosedDate,
    )
    val response = updateReview(review.id, request)

    val saved = getReview(response.reviewUuid)
    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(RECORD, PLAN, REVIEW),
      nomisContext().copy(source = Source.DPS),
    )
    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsTestQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  private fun urlToTest(reviewUuid: UUID) = "/csip-records/plan/reviews/$reviewUuid"

  private fun updateReviewRequest(
    summary: String? = "review summary",
    actions: SortedSet<ReviewAction> = sortedSetOf(),
    reviewDate: LocalDate = LocalDate.now(),
    nextReviewDate: LocalDate? = LocalDate.now().plusDays(3),
    csipClosedDate: LocalDate? = null,
  ) = UpdateReviewRequest(
    reviewDate,
    recordedBy = TEST_USER,
    recordedByDisplayName = TEST_USER_NAME,
    nextReviewDate,
    csipClosedDate,
    summary,
    actions,
  )

  private fun updateReviewResponseSpec(
    reviewId: UUID,
    request: UpdateReviewRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch()
    .uri(urlToTest(reviewId))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun updateReview(
    reviewUuid: UUID,
    request: UpdateReviewRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): Review = updateReviewResponseSpec(reviewUuid, request, username, role).successResponse(OK)

  private fun getReview(uuid: UUID): ReviewEntity = transactionTemplate.execute {
    val review = reviewRepository.getReview(uuid)
    review.plan.identifiedNeeds()
    review
  }!!

  private fun ReviewEntity.verifyAgainst(request: ReviewRequest) {
    assertThat(reviewDate).isEqualTo(request.reviewDate)
    assertThat(recordedBy).isEqualTo(request.recordedBy)
    assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    assertThat(nextReviewDate).isEqualTo(request.nextReviewDate)
    assertThat(csipClosedDate).isEqualTo(request.csipClosedDate)
    assertThat(summary).isEqualTo(request.summary)
    assertThat(actions).containsExactlyInAnyOrderElementsOf(request.actions)
  }
}
