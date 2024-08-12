package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.ATTENDEE_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getReview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.util.UUID
import java.util.UUID.randomUUID

class AddAttendeeIntTest : IntegrationTestBase() {

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
    val response = addAttendeeResponseSpec(randomUUID(), createAttendeeRequest(), role = role)
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
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post().uri(urlToTest(randomUUID()))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers { it.set(SOURCE, "INVALID") }
      .exchange().errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: No enum constant uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.INVALID")
      assertThat(developerMessage).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.INVALID")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = addAttendeeResponseSpec(randomUUID(), createAttendeeRequest(), username = null)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Could not find non empty username from user_name or username token claims or Username header")
      assertThat(developerMessage).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = addAttendeeResponseSpec(randomUUID(), createAttendeeRequest(), username = USER_NOT_FOUND)
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
  fun `404 not found - review not found`() {
    val uuid = randomUUID()
    val response = addAttendeeResponseSpec(uuid, createAttendeeRequest())
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
  fun `201 created - attendee added DPS`() {
    val prisonNumber = givenValidPrisonNumber("N1234DP")
    val review = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withPlan()
      val plan = requireNotNull(it.plan).withReview()
      plan.reviews().first()
    }

    val request = createAttendeeRequest(name = "A Person", role = "A special role")
    val response = addAttendee(review.uuid, request)

    val attendee = getAttendee(review.uuid, response.attendeeUuid)
    attendee.verifyAgainst(request)

    val record = review.plan.csipRecord
    verifyAudit(
      attendee,
      RevisionType.ADD,
      setOf(CsipComponent.Attendee),
    )

    verifyDomainEvents(
      prisonNumber,
      record.uuid,
      setOf(CsipComponent.Attendee),
      setOf(ATTENDEE_CREATED),
      setOf(attendee.uuid),
    )
  }

  @Test
  fun `201 created - attendee added NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("N1234NM")
    val review = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withPlan()
      val plan = requireNotNull(it.plan).withReview()
      plan.reviews().first()
    }

    val request = createAttendeeRequest(name = "A Person", role = "A special role")
    val response = addAttendee(review.uuid, request, NOMIS, NOMIS_SYS_USER, ROLE_NOMIS)

    val attendee = getAttendee(review.uuid, response.attendeeUuid)
    attendee.verifyAgainst(request)

    val record = review.plan.csipRecord
    verifyAudit(
      attendee,
      RevisionType.ADD,
      setOf(CsipComponent.Attendee),
      nomisContext(),
    )

    verifyDomainEvents(
      prisonNumber,
      record.uuid,
      setOf(CsipComponent.Attendee),
      setOf(ATTENDEE_CREATED),
      setOf(attendee.uuid),
      source = NOMIS,
    )
  }

  private fun urlToTest(reviewUuid: UUID) = "/csip-records/plan/reviews/$reviewUuid/attendees"

  private fun addAttendeeResponseSpec(
    csipUuid: UUID,
    request: CreateAttendeeRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post()
    .uri(urlToTest(csipUuid))
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username))
    .exchange()

  private fun addAttendee(
    csipUuid: UUID,
    request: CreateAttendeeRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Attendee =
    addAttendeeResponseSpec(csipUuid, request, source, username, role).successResponse(CREATED)

  private fun createAttendeeRequest(
    name: String? = "name",
    role: String? = "role",
    attended: Boolean? = true,
    contribution: String? = "a small contribution",
  ) = CreateAttendeeRequest(name, role, attended, contribution)

  private fun Attendee.verifyAgainst(request: CreateAttendeeRequest) {
    assertThat(name).isEqualTo(request.name)
    assertThat(role).isEqualTo(request.role)
    assertThat(attended).isEqualTo(request.isAttended)
    assertThat(contribution).isEqualTo(request.contribution)
  }

  private fun getAttendee(reviewUuid: UUID, attendeeUuid: UUID): Attendee =
    reviewRepository.getReview(reviewUuid).attendees().first { it.uuid == attendeeUuid }
}
