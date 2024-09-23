package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.ATTENDEE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.PLAN
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.AttendeeRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getAttendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.Duration.ofSeconds
import java.util.UUID
import java.util.UUID.randomUUID

class UpdateAttendeeIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var attendeeRepository: AttendeeRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch().uri(urlToTest(randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = updateAttendeeResponseSpec(randomUUID(), updateAttendeeRequest(), role = role)
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
    val response = updateAttendeeResponseSpec(randomUUID(), updateAttendeeRequest(), username = USER_NOT_FOUND)
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
  fun `404 not found - attendee not found`() {
    val uuid = randomUUID()
    val response = updateAttendeeResponseSpec(uuid, updateAttendeeRequest())
      .errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Attendee not found")
      assertThat(developerMessage).isEqualTo("Attendee not found with identifier $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 updated - attendee updated`() {
    val prisonNumber = givenValidPrisonNumber("A1234DP")
    val attendee = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withPlan()
      val plan = requireNotNull(it.plan).withReview()
      val review = plan.reviews().first().withAttendee()
      review.attendees().first()
    }

    val request = updateAttendeeRequest(name = "A Person", role = "A special role")
    val response = updateAttendee(attendee.id, request)

    val saved = getAttendee(response.attendeeUuid)
    saved.verifyAgainst(request)

    val record = saved.review.plan.csipRecord
    verifyAudit(attendee, RevisionType.MOD, setOf(ATTENDEE))
    verifyDomainEvents(prisonNumber, record.id, CSIP_UPDATED)
  }

  @Test
  fun `200 ok - attendee not updated with no change`() {
    val prisonNumber = givenValidPrisonNumber("A1234NC")
    val attendee = dataSetup(generateCsipRecord(prisonNumber).withPlan()) {
      val plan = requireNotNull(it.plan).withReview()
      val review = plan.reviews().first().withAttendee().withAttendee()
      review.attendees()[1]
    }

    val request = updateAttendeeRequest(
      attendee.name,
      attendee.role,
      attendee.attended,
      attendee.contribution,
    )
    val response = updateAttendee(attendee.id, request)

    val saved = getAttendee(response.attendeeUuid)
    assertThat(saved.lastModifiedAt).isNull()
    assertThat(saved.lastModifiedBy).isNull()
    assertThat(saved.lastModifiedByDisplayName).isNull()
    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(RECORD, PLAN, REVIEW, ATTENDEE),
      nomisContext().copy(source = Source.DPS),
    )
    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  private fun urlToTest(attendeeId: UUID) = "/csip-records/plan/reviews/attendees/$attendeeId"

  private fun updateAttendeeResponseSpec(
    attendeeId: UUID,
    request: UpdateAttendeeRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch()
    .uri(urlToTest(attendeeId))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun updateAttendee(
    attendeeId: UUID,
    request: UpdateAttendeeRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Attendee =
    updateAttendeeResponseSpec(attendeeId, request, username, role).successResponse()

  private fun updateAttendeeRequest(
    name: String? = "name",
    role: String? = "role",
    attended: Boolean? = true,
    contribution: String? = "a small contribution",
  ) = UpdateAttendeeRequest(name, role, attended, contribution)

  private fun Attendee.verifyAgainst(request: UpdateAttendeeRequest) {
    assertThat(name).isEqualTo(request.name)
    assertThat(role).isEqualTo(request.role)
    assertThat(attended).isEqualTo(request.isAttended)
    assertThat(contribution).isEqualTo(request.contribution)
  }

  private fun getAttendee(attendeeUuid: UUID): Attendee = attendeeRepository.getAttendee(attendeeUuid)
}
