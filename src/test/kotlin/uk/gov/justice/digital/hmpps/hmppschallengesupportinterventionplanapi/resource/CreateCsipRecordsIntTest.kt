package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createCsipRecordRequest
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class CreateCsipRecordsIntTest : IntegrationTestBase() {

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
      assertThat(userMessage).isEqualTo("Validation failure: Incident type code A could not be found")
      assertThat(developerMessage).isEqualTo("Incident type code A could not be found")
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
      assertThat(userMessage).isEqualTo("Validation failure: Incident location code B could not be found")
      assertThat(developerMessage).isEqualTo("Incident location code B could not be found")
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
      assertThat(userMessage).isEqualTo("Validation failure: Area of work code C could not be found")
      assertThat(developerMessage).isEqualTo("Area of work code C could not be found")
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
      assertThat(userMessage).isEqualTo("Validation failure: Incident involvement code D could not be found")
      assertThat(developerMessage).isEqualTo("Incident involvement code D could not be found")
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
      assertThat(userMessage).isEqualTo("Validation failure: Contributory factor type code D could not be found")
      assertThat(developerMessage).isEqualTo("Contributory factor type code D could not be found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - CSIP record created`() {
    val request = createCsipRecordRequest(
      incidentTypeCode = "ATO",
      incidentLocationCode = "EDU",
      refererAreaCode = "ACT",
      incidentInvolvementCode = "OTH",
      contributoryFactorTypeCode = "AFL",
    )

    val response = webTestClient.createCsipResponseSpec(request = request, prisonNumber = PRISON_NUMBER)
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

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it!! > 0 }
    val event = hmppsEventsQueue.receiveCsipDomainEventOnQueue()
    assertThat(event).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        DomainEventType.CSIP_CREATED.eventType,
        CsipAdditionalInformation(
          url = "http://localhost:8080/csip-records/${response.recordUuid}",
          recordUuid = response.recordUuid,
          prisonNumber = "A1234AA",
          isRecordAffected = false,
          isReferralAffected = false,
          isContributoryFactorAffected = false,
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
        event.occurredAt,
      ),
    )
  }

  private fun WebTestClient.createCsipResponseSpec(
    source: Source = DPS,
    request: CreateCsipRecordRequest,
    prisonNumber: String = "AB123456",
  ) =
    post()
      .uri("/prisoners/$prisonNumber/csip-records")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI)))
      .headers(setCsipRequestContext(source = source))
      .exchange()
      .expectHeader().contentType(MediaType.APPLICATION_JSON)

  private fun WebTestClient.createCsip(
    source: Source = DPS,
    request: CreateCsipRecordRequest,
    prisonNumber: String = "AB123456",
  ) =
    createCsipResponseSpec(source, request, prisonNumber)
      .expectStatus().isCreated
      .expectBody(CsipRecord::class.java)
      .returnResult().responseBody!!
}
