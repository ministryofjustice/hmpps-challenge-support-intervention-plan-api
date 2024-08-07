package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.data.history.RevisionMetadata.RevisionType.INSERT
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Record
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.NO
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.verifyAgainst
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

class CreateCsipRecordsIntTest : IntegrationTestBase() {

  @Test
  fun `403 forbidden - no required role`() {
    val response = webTestClient.get().uri(urlToTest("A1234BC"))
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE"))).exchange().errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest("A1234BC")).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri(urlToTest("A1234BC")).bodyValue(createCsipRecordRequest())
      .headers(setAuthorisation()).headers(setCsipRequestContext()).exchange().errorResponse(HttpStatus.FORBIDDEN)
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post().uri(urlToTest("A1234BC"))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers { it.set(SOURCE, "INVALID") }.exchange()
      .errorResponse(HttpStatus.BAD_REQUEST)

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
    val response = webTestClient.post().uri(urlToTest("A1234BC"))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).exchange()
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
    val response = webTestClient.post().uri(urlToTest("A1234BC")).bodyValue(createCsipRecordRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers(setCsipRequestContext(username = USER_NOT_FOUND))
      .exchange().errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post().uri(urlToTest("A1234BC"))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers(setCsipRequestContext()).exchange()
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource.CsipRecordsController.createCsipRecord(java.lang.String,uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - prisoner not found`() {
    val request = createCsipRecordRequest()

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER_NOT_FOUND)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prisoner number invalid")
      assertThat(developerMessage).isEqualTo("Prisoner number invalid")
      assertThat(moreInfo).isNull()
    }
  }

  @ParameterizedTest
  @MethodSource("referenceDataValidation")
  fun `400 bad request - when reference data code invalid or inactive`(
    request: CreateCsipRecordRequest,
    invalid: InvalidRd,
  ) {
    val prisonNumber = givenValidPrisonNumber("R1234VC")

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = prisonNumber)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: ${invalid.type} ${invalid.message}")
      assertThat(developerMessage).isEqualTo("Details => ${invalid.type}:${invalid.code(request.referral)}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - multiple contributory factor not found`() {
    val request = createCsipRecordRequest(createReferralRequest(contributoryFactorTypeCode = listOf("D", "E", "F")))

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Multiple invalid CONTRIBUTORY_FACTOR_TYPE")
      assertThat(developerMessage).isEqualTo("Details => CONTRIBUTORY_FACTOR_TYPE:[D,E,F]")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - single contributory factor not found`() {
    val request = createCsipRecordRequest(createReferralRequest(contributoryFactorTypeCode = listOf("D")))

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: CONTRIBUTORY_FACTOR_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => CONTRIBUTORY_FACTOR_TYPE:D")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - single contributory factor not active`() {
    val request = createCsipRecordRequest(
      createReferralRequest(
        incidentTypeCode = "ATO",
        incidentLocationCode = "EDU",
        refererAreaCode = "ACT",
        incidentInvolvementCode = "OTH",
        contributoryFactorTypeCode = listOf("CFT_INACT"),
      ),
    )

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: CONTRIBUTORY_FACTOR_TYPE is not active")
      assertThat(developerMessage).isEqualTo("Details => CONTRIBUTORY_FACTOR_TYPE:CFT_INACT")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - no contributory factors with source DPS`() {
    val request = createCsipRecordRequest(createReferralRequest(contributoryFactorTypeCode = listOf()))

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure: A referral must have at least one contributory factor.")
    }
  }

  @Test
  fun `201 created - CSIP record created via DPS`() {
    val request = createCsipRecordRequest(
      createReferralRequest(contributoryFactorTypeCode = listOf("AFL")),
    )

    val prisonNumber = givenValidPrisonNumber("C1234SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request)

    with(response) {
      assertThat(logCode).isEqualTo(LOG_CODE)
      assertThat(recordUuid).isNotNull()
      assertThat(referral).isNotNull()
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdByDisplayName).isEqualTo("Test User")
      assertThat(prisonCodeWhenRecorded).isEqualTo(PRISON_CODE_LEEDS)
      assertThat(status).isEqualTo(CsipStatus.REFERRAL_PENDING)
    }

    val saved = csipRecordRepository.getCsipRecord(response.recordUuid)
    saved.verifyAgainst(request)

    verifyAudit(saved, INSERT, setOf(Record, Referral, ContributoryFactor))

    verifyDomainEvents(
      prisonNumber,
      response.recordUuid,
      setOf(Record, Referral, ContributoryFactor),
      setOf(DomainEventType.CSIP_CREATED, DomainEventType.CONTRIBUTORY_FACTOR_CREATED),
      response.referral.contributoryFactors.map { it.factorUuid }.toSet(),
      2,
    )
  }

  @Test
  fun `201 created - CSIP record created via DPS with empty logCode`() {
    val request = createCsipRecordRequest(
      createReferralRequest(
        contributoryFactorTypeCode = listOf("AFL"),
      ),
      logCode = null,
    )

    val prisonNumber = givenValidPrisonNumber("C1235SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request)

    with(response) {
      assertThat(referral).isNotNull()
      assertThat(logCode).isNull()
    }

    val saved = csipRecordRepository.getCsipRecord(response.recordUuid)
    assertThat(saved.logCode).isNull()
    verifyAudit(saved, INSERT, setOf(Record, Referral, ContributoryFactor))
  }

  @Test
  fun `201 created - CSIP record created via NOMIS`() {
    val request = createCsipRecordRequest(
      createReferralRequest(
        contributoryFactorTypeCode = listOf("AFL"),
      ),
    )

    val prisonNumber = givenValidPrisonNumber("C1236SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request, NOMIS, NOMIS_SYS_USER)

    with(response) {
      assertThat(logCode).isEqualTo(LOG_CODE)
      assertThat(recordUuid).isNotNull()
      assertThat(referral).isNotNull()
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(createdByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
    }

    val saved = csipRecordRepository.getCsipRecord(response.recordUuid)

    verifyAudit(
      saved,
      INSERT,
      setOf(Record, Referral, ContributoryFactor),
      nomisContext(),
    )

    verifyDomainEvents(
      prisonNumber,
      response.recordUuid,
      setOf(Record, Referral, ContributoryFactor),
      setOf(DomainEventType.CSIP_CREATED, DomainEventType.CONTRIBUTORY_FACTOR_CREATED),
      response.referral.contributoryFactors.map { it.factorUuid }.toSet(),
      2,
      NOMIS,
    )
  }

  @Test
  fun `201 created - no contributory factors with source NOMIS`() {
    val request = createCsipRecordRequest(
      createReferralRequest(
        contributoryFactorTypeCode = listOf(),
        referralComplete = true,
        completedDate = LocalDate.now(),
        completedBy = "CompletedBy",
        completedByDisplayName = "Some Longer Display Name",
      ),
    )

    val prisonNumber = givenValidPrisonNumber("C1237SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request, NOMIS, NOMIS_SYS_USER)
    val saved = csipRecordRepository.getCsipRecord(response.recordUuid)
    saved.verifyAgainst(request)

    verifyAudit(saved, INSERT, setOf(Record, Referral), nomisContext())

    verifyDomainEvents(
      prisonNumber,
      response.recordUuid,
      setOf(Record, Referral),
      setOf(DomainEventType.CSIP_CREATED),
      source = NOMIS,
    )
  }

  private fun urlToTest(prisonNumber: String) = "/prisoners/$prisonNumber/csip-records"

  private fun WebTestClient.createCsipResponseSpec(
    source: Source = DPS,
    user: String = TEST_USER,
    request: CreateCsipRecordRequest,
    prisonNumber: String = PRISON_NUMBER,
  ) = post()
    .uri(urlToTest(prisonNumber))
    .bodyValue(request).headers(
      setAuthorisation(
        roles = listOf(
          source.let {
            if (it == NOMIS) {
              ROLE_NOMIS
            } else {
              ROLE_CSIP_UI
            }
          },
        ),
      ),
    ).headers(setCsipRequestContext(source = source, username = user)).exchange().expectHeader()
    .contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createCsipRecord(
    prisonNumber: String,
    request: CreateCsipRecordRequest,
    source: Source = DPS,
    user: String = TEST_USER,
  ) = createCsipResponseSpec(source, user, request, prisonNumber).successResponse<CsipRecord>(HttpStatus.CREATED)

  companion object {
    private const val INVALID = "is invalid"
    private const val NOT_ACTIVE = "is not active"

    private fun createCsipRequestWithReferralRd(
      incidentTypeCode: String = "ATO",
      incidentLocationCode: String = "EDU",
      refererAreaCode: String = "ACT",
      incidentInvolvementCode: String = "OTH",
    ) = createCsipRecordRequest(
      createReferralRequest(
        incidentTypeCode,
        incidentLocationCode,
        refererAreaCode,
        incidentInvolvementCode,
      ),
    )

    @JvmStatic
    fun referenceDataValidation() = listOf(
      Arguments.of(
        createCsipRequestWithReferralRd(incidentTypeCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, CreateReferralRequest::incidentTypeCode, INVALID),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentLocationCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_LOCATION, CreateReferralRequest::incidentLocationCode, INVALID),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(refererAreaCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, CreateReferralRequest::refererAreaCode, INVALID),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentInvolvementCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_INVOLVEMENT, { it.incidentInvolvementCode!! }, INVALID),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentTypeCode = "IT_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, CreateReferralRequest::incidentTypeCode, NOT_ACTIVE),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentLocationCode = "IL_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_LOCATION, CreateReferralRequest::incidentLocationCode, NOT_ACTIVE),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(refererAreaCode = "AOW_INACT"),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, CreateReferralRequest::refererAreaCode, NOT_ACTIVE),
      ),
      Arguments.of(
        createCsipRequestWithReferralRd(incidentInvolvementCode = "II_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_INVOLVEMENT, { it.incidentInvolvementCode!! }, NOT_ACTIVE),
      ),
    )

    data class InvalidRd(
      val type: ReferenceDataType,
      val code: (CreateReferralRequest) -> String,
      val message: String,
    )

    private fun createCsipRecordRequest(
      createReferralRequest: CreateReferralRequest = createReferralRequest(),
      logCode: String? = LOG_CODE,
    ) = CreateCsipRecordRequest(
      logCode,
      createReferralRequest,
    )

    private fun createReferralRequest(
      incidentTypeCode: String = "ATO",
      incidentLocationCode: String = "EDU",
      refererAreaCode: String = "ACT",
      incidentInvolvementCode: String = "OTH",
      contributoryFactorTypeCode: Collection<String> = listOf("AFL"),
      referralComplete: Boolean? = null,
      completedDate: LocalDate? = null,
      completedBy: String? = null,
      completedByDisplayName: String? = null,
    ) = CreateReferralRequest(
      LocalDate.now(),
      LocalTime.now(),
      incidentTypeCode,
      incidentLocationCode,
      "REFERRER",
      refererAreaCode,
      isProactiveReferral = false,
      isStaffAssaulted = false,
      "",
      incidentInvolvementCode,
      "concern description",
      "known reasons",
      "",
      NO,
      referralComplete,
      completedDate,
      completedBy,
      completedByDisplayName,
      contributoryFactorTypeCode.map { createContributoryFactorRequest(it) },
    )
  }
}
