package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.INTERVIEW_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INTERVIEWEE_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.UUID.randomUUID

class AddInterviewIntTest : IntegrationTestBase() {

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
    val response = addInterviewResponseSpec(randomUUID(), createInterviewRequest(), username = null)
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
      assertThat(userMessage).isEqualTo("Validation failure(s): Interviewee Role Code must be <= 12 characters")
    }
  }

  @ParameterizedTest
  @MethodSource("referenceDataValidation")
  fun `400 bad request - reference data invalid or inactive`(
    request: CreateInterviewRequest,
    invalid: InvalidRd,
  ) {
    val prisonNumber = givenValidPrisonNumber("R1234VI")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber)).withReferral()
    requireNotNull(record.referral).withInvestigation()

    val response = addInterviewResponseSpec(record.uuid, request).errorResponse(HttpStatus.BAD_REQUEST)
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
    val prisonNumber = givenValidPrisonNumber("I1234MR")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber))

    val response = addInterviewResponseSpec(record.uuid, createInterviewRequest())
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing a referral.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing a referral.")
      assertThat(moreInfo).isEqualTo(record.uuid.toString())
    }
  }

  @Test
  fun `400 bad request - missing investigation record`() {
    val prisonNumber = givenValidPrisonNumber("I1234MI")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber)).withReferral()

    val response = addInterviewResponseSpec(record.uuid, createInterviewRequest())
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing an investigation.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing an investigation.")
      assertThat(moreInfo).isEqualTo(record.uuid.toString())
    }
  }

  @Test
  fun `201 created - interview added DPS`() {
    val prisonNumber = givenValidPrisonNumber("I1234DP")
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral()
      requireNotNull(it.referral).withInvestigation()
      it
    }

    val request = createInterviewRequest(notes = "Some notes about the interview")
    val response = addInterview(record.uuid, request)

    val interview = getInterview(record.uuid, response.interviewUuid)
    interview.verifyAgainst(request)

    verifyAudit(
      interview,
      RevisionType.ADD,
      setOf(CsipComponent.Interview),
    )

    verifyDomainEvents(
      prisonNumber,
      record.uuid,
      setOf(CsipComponent.Interview),
      setOf(INTERVIEW_CREATED),
      setOf(response.interviewUuid),
    )
  }

  @Test
  fun `201 created - interview added NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("C1234NM")
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral()
      requireNotNull(it.referral).withInvestigation()
      it
    }

    val request = createInterviewRequest(notes = "Created By NOMIS")
    val response = addInterview(record.uuid, request, NOMIS, NOMIS_SYS_USER, ROLE_NOMIS)

    val interview = getInterview(record.uuid, response.interviewUuid)
    interview.verifyAgainst(request, NOMIS_SYS_USER, NOMIS_SYS_USER_DISPLAY_NAME)

    verifyAudit(
      interview,
      RevisionType.ADD,
      setOf(CsipComponent.Interview),
      nomisContext(),
    )

    verifyDomainEvents(
      prisonNumber,
      record.uuid,
      setOf(CsipComponent.Interview),
      setOf(INTERVIEW_CREATED),
      setOf(response.interviewUuid),
      source = NOMIS,
    )
  }

  private fun getInterview(csipUuid: UUID, interviewUuid: UUID): Interview = transactionTemplate.execute {
    val investigation = requireNotNull(csipRecordRepository.getCsipRecord(csipUuid).referral?.investigation)
    investigation.interviews().first { it.uuid == interviewUuid }
  }!!

  private fun Interview.verifyAgainst(
    request: CreateInterviewRequest,
    createdBy: String = TEST_USER,
    createdByDisplayName: String = TEST_USER_NAME,
  ) {
    assertThat(interviewee).isEqualTo(request.interviewee)
    assertThat(intervieweeRole.code).isEqualTo(request.intervieweeRoleCode)
    assertThat(interviewDate).isEqualTo(request.interviewDate)
    assertThat(interviewText).isEqualTo(request.interviewText)
    assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(this.createdBy).isEqualTo(createdBy)
    assertThat(this.createdByDisplayName).isEqualTo(createdByDisplayName)
  }

  private fun urlToTest(csipRecordUuid: UUID) = "/csip-records/$csipRecordUuid/referral/investigation/interviews"

  private fun addInterviewResponseSpec(
    csipUuid: UUID,
    request: CreateInterviewRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post()
    .uri(urlToTest(csipUuid))
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username))
    .exchange()

  private fun addInterview(
    csipUuid: UUID,
    request: CreateInterviewRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Interview =
    addInterviewResponseSpec(csipUuid, request, source, username, role).successResponse(CREATED)

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
