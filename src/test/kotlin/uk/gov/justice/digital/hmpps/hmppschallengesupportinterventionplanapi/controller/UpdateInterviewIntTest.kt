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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.InterviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.getInterview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INTERVIEWEE_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpdateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.util.UUID

class UpdateInterviewIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var interviewRepository: InterviewRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = updateInterviewResponseSpec(
      UUID.randomUUID(),
      interviewRequest(),
      role = role,
    ).errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username was not found`() {
    val response = updateInterviewResponseSpec(
      UUID.randomUUID(),
      interviewRequest(),
      username = "UNKNOWN",
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
  fun `400 bad request - invalid reference code`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withReferral()
      requireNotNull(it.referral).withInvestigation().investigation!!.withInterview()
      it
    }
    val interviewUuid = requireNotNull(record.referral?.investigation?.interviews()?.firstOrNull()).id

    val response = updateInterviewResponseSpec(
      interviewUuid,
      UpdateInterviewRequest(
        interviewee = "interviewee",
        interviewDate = LocalDate.of(2047, 7, 1),
        intervieweeRoleCode = "WRONG_CODE",
        interviewText = "interviewText",
      ),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INTERVIEWEE_ROLE is invalid")
      assertThat(developerMessage).isEqualTo("Details => INTERVIEWEE_ROLE:WRONG_CODE")
    }
  }

  @Test
  fun `400 bad request - invalid text values`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withReferral()
      requireNotNull(it.referral).withInvestigation().investigation!!.withInterview()
      it
    }
    val interviewUuid = requireNotNull(record.referral?.investigation?.interviews()?.firstOrNull()).id

    val response = updateInterviewResponseSpec(
      interviewUuid,
      UpdateInterviewRequest(
        interviewee = "n".repeat(101),
        interviewDate = LocalDate.of(2047, 7, 1),
        intervieweeRoleCode = "OTHER",
        interviewText = "n".repeat(4001),
      ),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(
        """Validation failures: 
        |Interview text must be <= 4000 characters
        |Interviewee name must be <= 100 characters
        |
        """.trimMargin(),
      )
      assertThat(developerMessage).isEqualTo(
        """400 BAD_REQUEST Validation failures: 
        |Interview text must be <= 4000 characters
        |Interviewee name must be <= 100 characters
        |
        """.trimMargin(),
      )
    }
  }

  @Test
  fun `404 not found - Interview not found`() {
    val interviewUuid = UUID.randomUUID()
    val response = updateInterviewResponseSpec(interviewUuid, interviewRequest()).errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Interview not found")
      assertThat(developerMessage).isEqualTo("Interview not found with identifier $interviewUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - no changes made to interview`() {
    val request = interviewRequest()

    val record = dataSetup(generateCsipRecord().withReferral()) {
      requireNotNull(it.referral).withInvestigation().investigation!!.withInterview(
        interviewee = request.interviewee,
        interviewDate = request.interviewDate,
        intervieweeRole = givenReferenceData(INTERVIEWEE_ROLE, request.intervieweeRoleCode),
        interviewText = request.interviewText,
      )
      it
    }

    val interviewUuid = requireNotNull(record.referral?.investigation?.interviews()?.firstOrNull()).id

    val response = updateInterview(interviewUuid, request, status = HttpStatus.OK)
    response.verifyAgainst(request)
    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsTestQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - update interview`() {
    val record = dataSetup(generateCsipRecord().withReferral()) {
      requireNotNull(it.referral).withInvestigation().investigation!!.withInterview(
        interviewee = "oldInterviewee",
        interviewDate = LocalDate.of(1999, 12, 31),
        intervieweeRole = givenReferenceData(INTERVIEWEE_ROLE, "WITNESS"),
        interviewText = "oldInterviewText",
      )
      it
    }

    val interviewUuid = requireNotNull(record.referral?.investigation?.interviews()?.firstOrNull()).id
    val request = interviewRequest()

    val response = updateInterview(interviewUuid, request, status = HttpStatus.OK)
    response.verifyAgainst(request)

    val interview = getInterview(interviewUuid)
    verifyAudit(interview, RevisionType.MOD, setOf(CsipComponent.INTERVIEW))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  private fun getInterview(interviewUuid: UUID) = interviewRepository.getInterview(interviewUuid)

  private fun Interview.verifyAgainst(request: UpdateInterviewRequest) {
    assertThat(interviewee).isEqualTo(request.interviewee)
    assertThat(interviewDate).isEqualTo(request.interviewDate)
    assertThat(intervieweeRole.code).isEqualTo(request.intervieweeRoleCode)
    assertThat(interviewText).isEqualTo(request.interviewText)
  }

  private fun interviewRequest() = UpdateInterviewRequest(
    interviewee = "interviewee",
    interviewDate = LocalDate.of(2024, 7, 1),
    intervieweeRoleCode = "OTHER",
    interviewText = "an updated block of text for the interview",
  )

  private fun urlToTest(interviewUuid: UUID) = "/csip-records/referral/investigation/interviews/$interviewUuid"

  private fun updateInterviewResponseSpec(
    recordUuid: UUID,
    request: UpdateInterviewRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role))).exchange()

  private fun updateInterview(
    recordUuid: UUID,
    request: UpdateInterviewRequest,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
    status: HttpStatus,
  ) = updateInterviewResponseSpec(recordUuid, request, username, role).successResponse<Interview>(status)
}
