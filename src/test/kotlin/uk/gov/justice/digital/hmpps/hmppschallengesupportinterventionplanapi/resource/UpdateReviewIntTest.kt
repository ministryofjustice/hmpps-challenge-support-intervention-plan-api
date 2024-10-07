package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.PLAN
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getReview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.UUID.randomUUID

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
      actions = setOf(ReviewAction.RESPONSIBLE_PEOPLE_INFORMED),
      reviewDate = LocalDate.now().plusDays(3),
      nextReviewDate = LocalDate.now().plusDays(4),
      csipClosedDate = LocalDate.now().plusDays(5),
    )
    val response = updateReview(review.id, request)

    val saved = getReview(response.reviewUuid)
    assertThat(saved.summary).isEqualTo(request.summary)
    assertThat(saved.actions).contains(ReviewAction.RESPONSIBLE_PEOPLE_INFORMED)
    assertThat(saved.actions.size).isEqualTo(1)
    assertThat(saved.reviewDate).isEqualTo(LocalDate.now().plusDays(3))
    assertThat(saved.nextReviewDate).isEqualTo(LocalDate.now().plusDays(4))
    assertThat(saved.csipClosedDate).isEqualTo(LocalDate.now().plusDays(5))
    assertThat(saved.lastModifiedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(saved.lastModifiedBy).isEqualTo(TEST_USER)
    assertThat(saved.lastModifiedByDisplayName).isEqualTo(TEST_USER_NAME)
    verifyAudit(saved, RevisionType.MOD, setOf(REVIEW))
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
        actions = setOf(),
        attendees = null,
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
    assertThat(saved.lastModifiedAt).isNull()
    assertThat(saved.lastModifiedBy).isNull()
    assertThat(saved.lastModifiedByDisplayName).isNull()
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
    actions: Set<ReviewAction> = setOf(),
    reviewDate: LocalDate? = LocalDate.now(),
    nextReviewDate: LocalDate? = LocalDate.now().plusDays(3),
    csipClosedDate: LocalDate? = LocalDate.now().plusDays(5),
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
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Review =
    updateReviewResponseSpec(reviewUuid, request, username, role).successResponse(OK)

  private fun getReview(uuid: UUID) = reviewRepository.getReview(uuid)
}
