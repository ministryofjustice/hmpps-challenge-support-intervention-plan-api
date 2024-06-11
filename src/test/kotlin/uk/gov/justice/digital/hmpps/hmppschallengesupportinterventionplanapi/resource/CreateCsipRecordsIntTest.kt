package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.ContributoryFactorAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.ContributoryFactorDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createCsipRecordRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CreateCsipRecordsIntTest(
  @Autowired private val csipRecordRepository: CsipRecordRepository,
) : IntegrationTestBase() {

  @Test
  fun `403 forbidden - no required role`() {
    val response = webTestClient.get()
      .uri("/prisoners/AB123456/csip-records")
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .exchange()
      .expectStatus().isForbidden
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage)
        .isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage)
        .isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.post()
      .uri("/prisoners/AB123456/csip-records")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post()
      .uri("/prisoners/AB123456/csip-records")
      .bodyValue(createCsipRecordRequest())
      .headers(setAuthorisation())
      .headers(setCsipRequestContext())
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post()
      .uri("/prisoners/AB123456/csip-records")
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI)))
      .headers { it.set(SOURCE, "INVALID") }
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: No enum constant uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.INVALID")
      assertThat(developerMessage).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.INVALID")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = webTestClient.post()
      .uri("/prisoners/AB123456/csip-records")
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI)))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Could not find non empty username from user_name or username token claims or Username header")
      assertThat(developerMessage).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = webTestClient.post()
      .uri("/prisoners/AB123456/csip-records")
      .bodyValue(createCsipRecordRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI)))
      .headers(setCsipRequestContext(username = USER_NOT_FOUND))
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.post()
      .uri("/prisoners/AB123456/csip-records")
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI)))
      .headers(setCsipRequestContext())
      .exchange()
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
      assertThat(developerMessage).isEqualTo("Required request body is missing: public uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource.CsipRecordsController.createCsipRecord(java.lang.String,uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest,jakarta.servlet.http.HttpServletRequest)")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - prisoner not found`() {
    val request = createCsipRecordRequest()

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER_NOT_FOUND)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prisoner with prison number NOT_FOUND could not be found")
      assertThat(developerMessage).isEqualTo("Prisoner with prison number NOT_FOUND could not be found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - incident type not found`() {
    val request = createCsipRecordRequest()

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INCIDENT_TYPE code 'A' does not exist")
      assertThat(developerMessage).isEqualTo("INCIDENT_TYPE code 'A' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - incident location not found`() {
    val request = createCsipRecordRequest(incidentTypeCode = "ATO")

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INCIDENT_LOCATION code 'B' does not exist")
      assertThat(developerMessage).isEqualTo("INCIDENT_LOCATION code 'B' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - area of work not found`() {
    val request = createCsipRecordRequest(incidentTypeCode = "ATO", incidentLocationCode = "EDU")

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: AREA_OF_WORK code 'C' does not exist")
      assertThat(developerMessage).isEqualTo("AREA_OF_WORK code 'C' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - incident involvement not found`() {
    val request =
      createCsipRecordRequest(incidentTypeCode = "ATO", incidentLocationCode = "EDU", refererAreaCode = "ACT")

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INCIDENT_INVOLVEMENT code 'D' does not exist")
      assertThat(developerMessage).isEqualTo("INCIDENT_INVOLVEMENT code 'D' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - contributory factor not found`() {
    val request = createCsipRecordRequest(
      incidentTypeCode = "ATO",
      incidentLocationCode = "EDU",
      refererAreaCode = "ACT",
      incidentInvolvementCode = "OTH",
    )

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: CONTRIBUTORY_FACTOR_TYPE code 'D' does not exist")
      assertThat(developerMessage).isEqualTo("CONTRIBUTORY_FACTOR_TYPE code 'D' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - multiple contributory factor not found`() {
    val request = createCsipRecordRequest(
      incidentTypeCode = "ATO",
      incidentLocationCode = "EDU",
      refererAreaCode = "ACT",
      incidentInvolvementCode = "OTH",
      contributoryFactorTypeCode = listOf("D", "E", "F"),
    )

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
      .expectStatus().isBadRequest
      .expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: CONTRIBUTORY_FACTOR_TYPE code(s) 'D', 'E', 'F' does not exist")
      assertThat(developerMessage).isEqualTo("CONTRIBUTORY_FACTOR_TYPE code(s) 'D', 'E', 'F' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - CSIP record created via DPS`() {
    val request = createCsipRecordRequest(
      incidentTypeCode = "ATO",
      incidentLocationCode = "EDU",
      refererAreaCode = "ACT",
      incidentInvolvementCode = "OTH",
      contributoryFactorTypeCode = listOf("AFL"),
    )

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER, source = DPS)
      .expectStatus().isCreated
      .expectBody(CsipRecord::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(logNumber).isEqualTo(LOG_NUMBER)
      assertThat(recordUuid).isNotNull()
      assertThat(referral).isNotNull()
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), Assertions.within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdByDisplayName).isEqualTo("Test User")
    }

    with(csipRecordRepository.findByRecordUuid(response.recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("CSIP record created via referral with 1 contributory factors")
      assertThat(isRecordAffected).isTrue()
      assertThat(isReferralAffected).isTrue()
      assertThat(isContributoryFactorAffected).isTrue()
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByCapturedName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(DPS)
      assertThat(reason).isEqualTo(Reason.USER)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val events = hmppsEventsQueue.receiveDomainEventsOnQueue()
    val csipDomainEvent = events.find { it is CsipDomainEvent }!!

    assertThat(csipDomainEvent).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        DomainEventType.CSIP_CREATED.eventType,
        CsipAdditionalInformation(
          url = "http://localhost:8080/csip-records/${response.recordUuid}",
          recordUuid = response.recordUuid,
          prisonNumber = "A1234AA",
          isRecordAffected = true,
          isReferralAffected = true,
          isContributoryFactorAffected = true,
          isSaferCustodyScreeningOutcomeAffected = false,
          isInvestigationAffected = false,
          isInterviewAffected = false,
          isDecisionAndActionsAffected = false,
          isPlanAffected = false,
          isIdentifiedNeedAffected = false,
          isReviewAffected = false,
          isAttendeeAffected = false,
          source = DPS,
          reason = Reason.USER,
        ),
        1,
        DomainEventType.CSIP_CREATED.description,
        csipDomainEvent.occurredAt,
      ),
    )

    val contributoryFactoryDomainEvent = events.find { it is ContributoryFactorDomainEvent }!!

    assertThat(contributoryFactoryDomainEvent).usingRecursiveComparison().isEqualTo(
      ContributoryFactorDomainEvent(
        DomainEventType.CONTRIBUTORY_FACTOR_CREATED.eventType,
        ContributoryFactorAdditionalInformation(
          url = "http://localhost:8080/csip-records/${response.recordUuid}",
          contributoryFactorUuid = response.referral.contributoryFactors.first().factorUuid,
          recordUuid = response.recordUuid,
          prisonNumber = "A1234AA",
          source = DPS,
          reason = Reason.USER,
        ),
        1,
        DomainEventType.CONTRIBUTORY_FACTOR_CREATED.description,
        contributoryFactoryDomainEvent.occurredAt,
      ),
    )
  }

  @Test
  fun `201 created - CSIP record created via NOMIS`() {
    val request = createCsipRecordRequest(
      incidentTypeCode = "ATO",
      incidentLocationCode = "EDU",
      refererAreaCode = "ACT",
      incidentInvolvementCode = "OTH",
      contributoryFactorTypeCode = listOf("AFL"),
    )

    val response = webTestClient.createCsipResponseSpec(
      request = request,
      prisonNumber = PRISON_NUMBER,
      source = NOMIS,
      user = NOMIS_SYS_USER,
    )
      .expectStatus().isCreated
      .expectBody(CsipRecord::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(logNumber).isEqualTo(LOG_NUMBER)
      assertThat(recordUuid).isNotNull()
      assertThat(referral).isNotNull()
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), Assertions.within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(createdByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
    }

    with(csipRecordRepository.findByRecordUuid(response.recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("CSIP record created via referral with 1 contributory factors")
      assertThat(isRecordAffected).isTrue()
      assertThat(isReferralAffected).isTrue()
      assertThat(isContributoryFactorAffected).isTrue()
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByCapturedName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(NOMIS)
      assertThat(reason).isEqualTo(Reason.USER)
      assertThat(activeCaseLoadId).isNull()
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 2 }
    val events = hmppsEventsQueue.receiveDomainEventsOnQueue()
    val csipDomainEvent = events.find { it is CsipDomainEvent }!!

    assertThat(csipDomainEvent).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        DomainEventType.CSIP_CREATED.eventType,
        CsipAdditionalInformation(
          url = "http://localhost:8080/csip-records/${response.recordUuid}",
          recordUuid = response.recordUuid,
          prisonNumber = "A1234AA",
          isRecordAffected = true,
          isReferralAffected = true,
          isContributoryFactorAffected = true,
          isSaferCustodyScreeningOutcomeAffected = false,
          isInvestigationAffected = false,
          isInterviewAffected = false,
          isDecisionAndActionsAffected = false,
          isPlanAffected = false,
          isIdentifiedNeedAffected = false,
          isReviewAffected = false,
          isAttendeeAffected = false,
          source = NOMIS,
          reason = Reason.USER,
        ),
        1,
        DomainEventType.CSIP_CREATED.description,
        csipDomainEvent.occurredAt,
      ),
    )

    val contributoryFactoryDomainEvent = events.find { it is ContributoryFactorDomainEvent }!!

    assertThat(contributoryFactoryDomainEvent).usingRecursiveComparison().isEqualTo(
      ContributoryFactorDomainEvent(
        DomainEventType.CONTRIBUTORY_FACTOR_CREATED.eventType,
        ContributoryFactorAdditionalInformation(
          url = "http://localhost:8080/csip-records/${response.recordUuid}",
          contributoryFactorUuid = response.referral.contributoryFactors.first().factorUuid,
          recordUuid = response.recordUuid,
          prisonNumber = "A1234AA",
          source = NOMIS,
          reason = Reason.USER,
        ),
        1,
        DomainEventType.CONTRIBUTORY_FACTOR_CREATED.description,
        contributoryFactoryDomainEvent.occurredAt,
      ),
    )
  }

  private fun WebTestClient.createCsipResponseSpec(
    source: Source = DPS,
    user: String = TEST_USER,
    request: CreateCsipRecordRequest,
    prisonNumber: String = "AB123456",
  ) =
    post()
      .uri("/prisoners/$prisonNumber/csip-records")
      .bodyValue(request)
      .headers(
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
      )
      .headers(setCsipRequestContext(source = source, username = user))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createCsip(
    source: Source = DPS,
    user: String = TEST_USER,
    request: CreateCsipRecordRequest,
    prisonNumber: String = "AB123456",
  ) =
    createCsipResponseSpec(source, user, request, prisonNumber)
      .expectStatus().isCreated
      .expectBody(CsipRecord::class.java)
      .returnResult().responseBody!!
}
