package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.InterviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.getInterview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INTERVIEWEE_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createInterviewRequest
import java.util.UUID
import java.util.UUID.randomUUID

class AddInterviewIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var interviewRepository: InterviewRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = addInterviewResponseSpec(randomUUID(), createInterviewRequest(), role = role)
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
    val response = addInterviewResponseSpec(randomUUID(), createInterviewRequest(), username = USER_NOT_FOUND)
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
  fun `400 bad request - interviewee role code too long`() {
    val response = addInterviewResponseSpec(randomUUID(), createInterviewRequest(roleCode = "n".repeat(13)))
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure: Interviewee role code must be <= 12 characters")
    }
  }

  @ParameterizedTest
  @MethodSource("referenceDataValidation")
  fun `400 bad request - reference data invalid or inactive`(
    request: CreateInterviewRequest,
    invalid: InvalidRd,
  ) {
    val record = dataSetup(generateCsipRecord()) {
      it.withReferral()
      requireNotNull(it.referral).withInvestigation()
      it
    }

    val response = addInterviewResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: ${invalid.type} ${invalid.message}")
      assertThat(developerMessage).isEqualTo("Details => ${invalid.type}:${invalid.code(request)}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `404 not found - csip record not found`() {
    val uuid = randomUUID()
    val response = addInterviewResponseSpec(uuid, createInterviewRequest())
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
  fun `400 bad request - missing referral record`() {
    val record = givenCsipRecord(generateCsipRecord())

    val response = addInterviewResponseSpec(record.id, createInterviewRequest())
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing a referral.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing a referral.")
      assertThat(moreInfo).isEqualTo(record.id.toString())
    }
  }

  @Test
  fun `400 bad request - missing investigation record`() {
    val record = givenCsipRecord(generateCsipRecord().withReferral())

    val response = addInterviewResponseSpec(record.id, createInterviewRequest())
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing an investigation.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing an investigation.")
      assertThat(moreInfo).isEqualTo(record.id.toString())
    }
  }

  @Test
  fun `201 created - interview added DPS`() {
    val record = dataSetup(generateCsipRecord().withReferral()) {
      requireNotNull(it.referral).withInvestigation()
      it
    }

    val request = createInterviewRequest(notes = "Some notes about the interview")
    val response = addInterview(record.id, request)

    val interview = getInterview(response.interviewUuid)
    interview.verifyAgainst(request)
    verifyAudit(interview, RevisionType.ADD, setOf(CsipComponent.INTERVIEW))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  private fun getInterview(uuid: UUID): Interview = interviewRepository.getInterview(uuid)

  private fun Interview.verifyAgainst(
    request: CreateInterviewRequest,
  ) {
    assertThat(interviewee).isEqualTo(request.interviewee)
    assertThat(intervieweeRole.code).isEqualTo(request.intervieweeRoleCode)
    assertThat(interviewDate).isEqualTo(request.interviewDate)
    assertThat(interviewText).isEqualTo(request.interviewText)
  }

  private fun urlToTest(csipRecordUuid: UUID) = "/csip-records/$csipRecordUuid/referral/investigation/interviews"

  private fun addInterviewResponseSpec(
    csipUuid: UUID,
    request: CreateInterviewRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post()
    .uri(urlToTest(csipUuid))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun addInterview(
    csipUuid: UUID,
    request: CreateInterviewRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Interview =
    addInterviewResponseSpec(csipUuid, request, username, role).successResponse(CREATED)

  companion object {
    private const val INVALID = "is invalid"
    private const val NOT_ACTIVE = "is not active"

    @JvmStatic
    fun referenceDataValidation() = listOf(
      Arguments.of(
        createInterviewRequest(roleCode = "NON_EXISTENT"),
        InvalidRd(INTERVIEWEE_ROLE, CreateInterviewRequest::intervieweeRoleCode, INVALID),
      ),
      Arguments.of(
        createInterviewRequest(roleCode = "IR_INACT"),
        InvalidRd(INTERVIEWEE_ROLE, CreateInterviewRequest::intervieweeRoleCode, NOT_ACTIVE),
      ),
    )

    data class InvalidRd(
      val type: ReferenceDataType,
      val code: (CreateInterviewRequest) -> String,
      val message: String,
    )
  }
}
