package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.getReview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createAttendeeRequest
import java.time.LocalDate
import java.util.SortedSet
import java.util.UUID
import java.util.UUID.randomUUID

class AddReviewIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var reviewRepository: ReviewRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = addReviewResponseSpec(randomUUID(), createReviewRequest(), role = role)
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
    val response = addReviewResponseSpec(randomUUID(), createReviewRequest(), username = USER_NOT_FOUND)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `404 not found - csip record not found`() {
    val uuid = randomUUID()
    val response = addReviewResponseSpec(uuid, createReviewRequest())
      .errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - review added DPS`() {
    val record = dataSetup(generateCsipRecord()) { it.withPlan() }

    val request = createReviewRequest(attendees = listOf(createAttendeeRequest(), createAttendeeRequest()))
    val response = addReview(record.id, request)

    val review = getReview(response.reviewUuid)
    review.verifyAgainst(request)
    assertThat(review.plan.nextReviewDate).isEqualTo(review.nextReviewDate)

    val attendeeUuids = review.attendees().map { it.id }
    assertThat(attendeeUuids.size).isEqualTo(2)
    verifyAudit(review, RevisionType.ADD, setOf(CsipComponent.REVIEW, CsipComponent.ATTENDEE))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  @Test
  fun `201 created - review closes csip and any open needs`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withPlan()
      requireNotNull(it.plan).withNeed()
      it
    }

    val request = createReviewRequest(
      nextReviewDate = null,
      attendees = listOf(createAttendeeRequest()),
      csipClosedDate = LocalDate.now(),
      actions = sortedSetOf(ReviewAction.CLOSE_CSIP),
    )
    val response = addReview(record.id, request)

    val review = getReview(response.reviewUuid)
    review.verifyAgainst(request)
    assertThat(review.plan.nextReviewDate).isNull()
    assertThat(review.plan.closedDate).isEqualTo(review.csipClosedDate)

    val need = review.plan.identifiedNeeds().first()
    assertThat(need.closedDate).isEqualTo(review.csipClosedDate)

    verifyAudit(
      review,
      RevisionType.ADD,
      setOf(CsipComponent.REVIEW, CsipComponent.ATTENDEE, CsipComponent.IDENTIFIED_NEED),
    )
    verifyAudit(
      need,
      RevisionType.MOD,
      setOf(CsipComponent.REVIEW, CsipComponent.ATTENDEE, CsipComponent.IDENTIFIED_NEED),
    )
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  private fun urlToTest(csipRecordUuid: UUID) = "/csip-records/$csipRecordUuid/plan/reviews"

  private fun addReviewResponseSpec(
    csipUuid: UUID,
    request: CreateReviewRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post()
    .uri(urlToTest(csipUuid))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun addReview(
    csipUuid: UUID,
    request: CreateReviewRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Review = addReviewResponseSpec(csipUuid, request, username, role).successResponse(CREATED)

  private fun createReviewRequest(
    reviewDate: LocalDate = LocalDate.now(),
    recordedBy: String = "recordedBy",
    recordedByDisplayName: String = "recordedByDisplayName",
    nextReviewDate: LocalDate? = LocalDate.now().plusWeeks(4),
    csipClosedDate: LocalDate? = null,
    summary: String? = "A brief summary of the review",
    actions: SortedSet<ReviewAction> = sortedSetOf(),
    attendees: List<CreateAttendeeRequest> = listOf(),
  ) = CreateReviewRequest(
    reviewDate,
    recordedBy,
    recordedByDisplayName,
    nextReviewDate,
    csipClosedDate,
    summary,
    actions,
    attendees,
  )

  private fun Review.verifyAgainst(request: CreateReviewRequest) {
    assertThat(reviewDate).isEqualTo(request.reviewDate)
    assertThat(recordedBy).isEqualTo(request.recordedBy)
    assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    assertThat(nextReviewDate).isEqualTo(request.nextReviewDate)
    assertThat(csipClosedDate).isEqualTo(request.csipClosedDate)
    assertThat(summary).isEqualTo(request.summary)
    assertThat(actions).isEqualTo(request.actions)
    assertThat(attendees().size).isEqualTo(request.attendees.size)
  }

  private fun getReview(uuid: UUID): Review = transactionTemplate.execute {
    val review = reviewRepository.getReview(uuid)
    review.plan.identifiedNeeds()
    review
  }!!
}
