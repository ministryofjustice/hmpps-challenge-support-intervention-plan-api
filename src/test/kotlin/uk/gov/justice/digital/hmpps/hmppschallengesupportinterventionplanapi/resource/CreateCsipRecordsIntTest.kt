package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBasicDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBasicInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.badRequest
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createCsipRecordRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CreateCsipRecordsIntTest : IntegrationTestBase() {

  @Test
  fun `403 forbidden - no required role`() {
    val response = webTestClient.get().uri("/prisoners/A1234BC/csip-records")
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE"))).exchange().expectStatus().isForbidden.expectBody(
        ErrorResponse::class.java,
      ).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/prisoners/A1234BC/csip-records").exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri("/prisoners/A1234BC/csip-records").bodyValue(createCsipRecordRequest())
      .headers(setAuthorisation()).headers(setCsipRequestContext()).exchange().expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post().uri("/prisoners/A1234BC/csip-records")
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers { it.set(SOURCE, "INVALID") }.exchange()
      .badRequest()

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
    val response = webTestClient.post().uri("/prisoners/A1234BC/csip-records")
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).exchange()
      .badRequest()

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
    val response = webTestClient.post().uri("/prisoners/A1234BC/csip-records").bodyValue(createCsipRecordRequest())
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers(setCsipRequestContext(username = USER_NOT_FOUND))
      .exchange().badRequest()

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
    val response = webTestClient.post().uri("/prisoners/A1234BC/csip-records")
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers(setCsipRequestContext()).exchange()
      .badRequest()

    with(response) {
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
      .badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Prisoner number invalid")
      assertThat(developerMessage).isEqualTo("Prisoner number invalid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - incident type not found`() {
    val request = createCsipRecordRequest()

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INCIDENT_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => INCIDENT_TYPE:A")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - incident location not found`() {
    val request = createCsipRecordRequest(incidentTypeCode = "ATO")

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INCIDENT_LOCATION is invalid")
      assertThat(developerMessage).isEqualTo("Details => INCIDENT_LOCATION:B")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - area of work not found`() {
    val request = createCsipRecordRequest(incidentTypeCode = "ATO", incidentLocationCode = "EDU")

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: AREA_OF_WORK is invalid")
      assertThat(developerMessage).isEqualTo("Details => AREA_OF_WORK:C")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - incident involvement not found`() {
    val request =
      createCsipRecordRequest(incidentTypeCode = "ATO", incidentLocationCode = "EDU", refererAreaCode = "ACT")

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INCIDENT_INVOLVEMENT is invalid")
      assertThat(developerMessage).isEqualTo("Details => INCIDENT_INVOLVEMENT:D")
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

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: CONTRIBUTORY_FACTOR_TYPE is invalid")
      assertThat(developerMessage).isEqualTo("Details => CONTRIBUTORY_FACTOR_TYPE:D")
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

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER).badRequest()

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
    val request = createCsipRecordRequest(
      incidentTypeCode = "ATO",
      incidentLocationCode = "EDU",
      refererAreaCode = "ACT",
      incidentInvolvementCode = "OTH",
      contributoryFactorTypeCode = listOf("D"),
    )

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER).badRequest()

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
      incidentTypeCode = "ATO",
      incidentLocationCode = "EDU",
      refererAreaCode = "ACT",
      incidentInvolvementCode = "OTH",
      contributoryFactorTypeCode = listOf("CFT_INACT"),
    )

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: CONTRIBUTORY_FACTOR_TYPE is not active")
      assertThat(developerMessage).isEqualTo("Details => CONTRIBUTORY_FACTOR_TYPE:CFT_INACT")
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

    val prisonNumber = givenValidPrisonNumber("C1234SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request)

    with(response!!) {
      assertThat(logCode).isEqualTo(LOG_CODE)
      assertThat(recordUuid).isNotNull()
      assertThat(referral).isNotNull()
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo("TEST_USER")
      assertThat(createdByDisplayName).isEqualTo("Test User")
      assertThat(prisonCodeWhenRecorded).isEqualTo(PRISON_CODE_LEEDS)
      assertThat(referral.releaseDate).isNull()
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
    val csipDomainEvent = events.find { it is CsipDomainEvent } as CsipDomainEvent

    assertThat(csipDomainEvent).usingRecursiveComparison().ignoringFields("additionalInformation.affectedComponents")
      .isEqualTo(
        CsipDomainEvent(
          eventType = DomainEventType.CSIP_CREATED.eventType,
          additionalInformation = CsipAdditionalInformation(
            recordUuid = response.recordUuid,
            affectedComponents = setOf(),
            source = DPS,
          ),
          version = 1,
          description = DomainEventType.CSIP_CREATED.description,
          occurredAt = csipDomainEvent.occurredAt,
          detailUrl = "http://localhost:8080/csip-records/${response.recordUuid}",
          personReference = PersonReference.withPrisonNumber(prisonNumber),
        ),
      )

    assertThat(csipDomainEvent.additionalInformation.affectedComponents).containsExactlyInAnyOrder(
      AffectedComponent.Record,
      AffectedComponent.Referral,
      AffectedComponent.ContributoryFactor,
    )

    val contributoryFactoryDomainEvent = events.find { it is CsipBasicDomainEvent } as CsipBasicDomainEvent

    assertThat(contributoryFactoryDomainEvent).usingRecursiveComparison().isEqualTo(
      CsipBasicDomainEvent(
        eventType = DomainEventType.CONTRIBUTORY_FACTOR_CREATED.eventType,
        additionalInformation = CsipBasicInformation(
          entityUuid = response.referral.contributoryFactors.first().factorUuid,
          recordUuid = response.recordUuid,
          source = DPS,
        ),
        version = 1,
        description = DomainEventType.CONTRIBUTORY_FACTOR_CREATED.description,
        occurredAt = contributoryFactoryDomainEvent.occurredAt,
        detailUrl = "http://localhost:8080/csip-records/${response.recordUuid}",
        personReference = PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
  }

  @Test
  fun `201 created - CSIP record created via DPS with empty logCode`() {
    val request = createCsipRecordRequest(
      incidentTypeCode = "ATO",
      incidentLocationCode = "EDU",
      refererAreaCode = "ACT",
      incidentInvolvementCode = "OTH",
      contributoryFactorTypeCode = listOf("AFL"),
      logCode = null,
    )

    val prisonNumber = givenValidPrisonNumber("C1235SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request)

    with(response!!) {
      assertThat(referral).isNotNull()
      assertThat(logCode).isNull()
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

    val prisonNumber = givenValidPrisonNumber("C1236SP")
    val response = webTestClient.createCsipRecord(prisonNumber, request, NOMIS, NOMIS_SYS_USER)

    with(response!!) {
      assertThat(logCode).isEqualTo(LOG_CODE)
      assertThat(recordUuid).isNotNull()
      assertThat(referral).isNotNull()
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
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
    val csipDomainEvent = events.find { it is CsipDomainEvent } as CsipDomainEvent

    assertThat(csipDomainEvent).usingRecursiveComparison().ignoringFields("additionalInformation.affectedComponents")
      .isEqualTo(
        CsipDomainEvent(
          eventType = DomainEventType.CSIP_CREATED.eventType,
          additionalInformation = CsipAdditionalInformation(
            recordUuid = response.recordUuid,
            affectedComponents = setOf(),
            source = NOMIS,
          ),
          version = 1,
          description = DomainEventType.CSIP_CREATED.description,
          occurredAt = csipDomainEvent.occurredAt,
          detailUrl = "http://localhost:8080/csip-records/${response.recordUuid}",
          personReference = PersonReference.withPrisonNumber(prisonNumber),
        ),
      )
    assertThat(csipDomainEvent.additionalInformation.affectedComponents).containsExactlyInAnyOrder(
      AffectedComponent.Record,
      AffectedComponent.Referral,
      AffectedComponent.ContributoryFactor,
    )

    val contributoryFactoryDomainEvent = events.find { it is CsipBasicDomainEvent } as CsipBasicDomainEvent

    assertThat(contributoryFactoryDomainEvent).usingRecursiveComparison().isEqualTo(
      CsipBasicDomainEvent(
        eventType = DomainEventType.CONTRIBUTORY_FACTOR_CREATED.eventType,
        additionalInformation = CsipBasicInformation(
          entityUuid = response.referral.contributoryFactors.first().factorUuid,
          recordUuid = response.recordUuid,
          source = NOMIS,
        ),
        version = 1,
        description = DomainEventType.CONTRIBUTORY_FACTOR_CREATED.description,
        occurredAt = contributoryFactoryDomainEvent.occurredAt,
        detailUrl = "http://localhost:8080/csip-records/${response.recordUuid}",
        personReference = PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
  }

  private fun WebTestClient.createCsipResponseSpec(
    source: Source = DPS,
    user: String = TEST_USER,
    request: CreateCsipRecordRequest,
    prisonNumber: String = PRISON_NUMBER,
  ) = post()
    .uri("/prisoners/$prisonNumber/csip-records")
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
  ) = createCsipResponseSpec(source, user, request, prisonNumber)
    .expectStatus().isCreated
    .expectBody<CsipRecord>()
    .returnResult().responseBody
}
