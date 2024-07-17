package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBasicDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBasicInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.badRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class InvestigationsIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/investigation").exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - no roles`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/investigation")
      .bodyValue(investigationRequest()).headers(setAuthorisation()).headers(setCsipRequestContext()).exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `403 forbidden - incorrect role`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/investigation")
      .bodyValue(investigationRequest()).headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .headers(setCsipRequestContext()).exchange().expectStatus().isForbidden
  }

  @Test
  fun `400 bad request - invalid source`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/investigation")
      .bodyValue(investigationRequest()).headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .headers { it.set(SOURCE, "INVALID") }.exchange().expectStatus().isBadRequest
  }

  @Test
  fun `400 bad request - request body validation failure`() {
    val recordUuid = UUID.randomUUID()
    val request = investigationRequest(interviewRequest(roleCode = "n".repeat(13)))
    val response = createInvestigationResponseSpec(recordUuid, request).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure(s): Interviewee Role Code must be <= 12 characters")
      assertThat(developerMessage).isEqualTo(
        "Validation failed for argument [1] in public uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource.InvestigationsController.createInvestigation(java.util.UUID,uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest,jakarta.servlet.http.HttpServletRequest): [Field error in object 'createInvestigationRequest' on field 'interviews[0].intervieweeRoleCode': rejected value [nnnnnnnnnnnnn]; codes [Size.createInvestigationRequest.interviews[0].intervieweeRoleCode,Size.createInvestigationRequest.interviews.intervieweeRoleCode,Size.interviews[0].intervieweeRoleCode,Size.interviews.intervieweeRoleCode,Size.intervieweeRoleCode,Size.java.lang.String,Size]; arguments [org.springframework.context.support.DefaultMessageSourceResolvable: codes [createInvestigationRequest.interviews[0].intervieweeRoleCode,interviews[0].intervieweeRoleCode]; arguments []; default message [interviews[0].intervieweeRoleCode],12,1]; default message [Interviewee Role Code must be <= 12 characters]] ",
      )
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - invalid Outcome Type code`() {
    val recordUuid = UUID.randomUUID()
    val request = investigationRequest(interviewRequest(roleCode = "WRONG_CODE"))
    val response = createInvestigationResponseSpec(recordUuid, request).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INTERVIEWEE_ROLE is invalid")
      assertThat(developerMessage).isEqualTo("Details => INTERVIEWEE_ROLE:WRONG_CODE")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - CSIP record missing a referral`() {
    val prisonNumber = givenValidPrisonNumber("I2234MR")
    val csipRecord = givenCsipRecord(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid

    val response = createInvestigationResponseSpec(recordUuid, investigationRequest(interviewRequest())).badRequest()

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record with UUID: $recordUuid is missing a referral.")
      assertThat(developerMessage).isEqualTo("CSIP Record with UUID: $recordUuid is missing a referral.")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `404 not found - CSIP record not found`() {
    val recordUuid = UUID.randomUUID()
    val response = createInvestigationResponseSpec(recordUuid, investigationRequest(interviewRequest()))
      .expectStatus().isNotFound
      .expectBody<ErrorResponse>()
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $recordUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `409 conflict - CSIP record already has Screening Outcome created`() {
    val prisonNumber = givenValidPrisonNumber("S1234AC")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val intervieweeRole = givenRandom(ReferenceDataType.INTERVIEWEE_ROLE)

    csipRecord.referral!!.createInvestigation(
      createRequest = investigationRequest(
        interviewRequest(intervieweeRole.code),
        interviewRequest(intervieweeRole.code),
      ),
      intervieweeRoleMap = mapOf(intervieweeRole.code to intervieweeRole),
      actionedAt = LocalDateTime.now(),
      actionedBy = "actionedBy",
      actionedByDisplayName = "actionedByDisplayName",
      source = Source.DPS,
      activeCaseLoadId = PRISON_CODE_LEEDS,
    )
    csipRecordRepository.save(csipRecord)

    val response = createInvestigationResponseSpec(recordUuid, investigationRequest())
      .expectStatus().is4xxClientError
      .expectBody<ErrorResponse>()
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: Referral already has an Investigation")
      assertThat(developerMessage).isEqualTo("Referral already has an Investigation")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `create investigation via DPS UI`() {
    val prisonNumber = givenValidPrisonNumber("I1234DS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = investigationRequest(
      interviewRequest(name = "John"),
      interviewRequest(name = "Jane"),
    )

    val response = createInvestigation(recordUuid, request)

    // Investigation populated with data from request and context
    with(response) {
      assertThat(staffInvolved).isEqualTo(request.staffInvolved)
      assertThat(evidenceSecured).isEqualTo(request.evidenceSecured)
      assertThat(occurrenceReason).isEqualTo(request.occurrenceReason)
      assertThat(personsUsualBehaviour).isEqualTo(request.personsUsualBehaviour)
      assertThat(personsTrigger).isEqualTo(request.personsTrigger)
      assertThat(protectiveFactors).isEqualTo(request.protectiveFactors)

      assertThat(interviews.map { it.interviewee }).containsExactlyInAnyOrder("John", "Jane")
      assertThat(interviews.map { it.createdBy }).allMatch { it.equals(TEST_USER) }
      assertThat(interviews.map { it.createdByDisplayName }).allMatch { it.equals(TEST_USER_NAME) }
    }

    // Audit event saved
    with(csipRecordRepository.findByRecordUuid(recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Investigation with 2 interviews added to referral")
      assertThat(affectedComponents).containsExactlyInAnyOrder(
        AffectedComponent.Investigation,
        AffectedComponent.Interview,
      )
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByCapturedName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(Source.DPS)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }

    // person.csip.record.updated domain event published
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 3 }

    val domainEvents = hmppsEventsQueue.receiveDomainEventsOnQueue()

    val csipEvent = domainEvents.find { it is CsipDomainEvent } as CsipDomainEvent
    assertThat(csipEvent).usingRecursiveComparison().ignoringFields("additionalInformation.affectedComponents")
      .isEqualTo(
        CsipDomainEvent(
          eventType = DomainEventType.CSIP_UPDATED.eventType,
          additionalInformation = CsipAdditionalInformation(
            recordUuid = recordUuid,
            affectedComponents = setOf(),
            source = Source.DPS,
          ),
          version = 1,
          description = "Investigation with 2 interviews added to referral",
          occurredAt = csipEvent.occurredAt,
          detailUrl = "http://localhost:8080/csip-records/$recordUuid",
          personReference = PersonReference.withPrisonNumber(prisonNumber),
        ),
      )
    assertThat(csipEvent.additionalInformation.affectedComponents).containsExactlyInAnyOrder(
      AffectedComponent.Investigation,
      AffectedComponent.Interview,
    )

    val interviewEvents = domainEvents.filterIsInstance<CsipBasicDomainEvent>()
      .filter { it.eventType == DomainEventType.INTERVIEW_CREATED.eventType }

    assertThat(interviewEvents).hasSize(2)
    assertThat(interviewEvents[0]).usingRecursiveComparison().ignoringFields("additionalInformation.entityUuid")
      .isEqualTo(
        CsipBasicDomainEvent(
          eventType = DomainEventType.INTERVIEW_CREATED.eventType,
          additionalInformation = CsipBasicInformation(
            entityUuid = UUID.randomUUID(),
            recordUuid = recordUuid,
            source = Source.DPS,
          ),
          description = DomainEventType.INTERVIEW_CREATED.description,
          occurredAt = interviewEvents[0].occurredAt,
          detailUrl = "http://localhost:8080/csip-records/$recordUuid",
          personReference = PersonReference.withPrisonNumber(prisonNumber),
        ),
      )
  }

  @Test
  fun `create investigation via NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("I1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = investigationRequest()

    val response = createInvestigation(recordUuid, request, source = Source.NOMIS, username = NOMIS_SYS_USER)

    // Investigation populated with data from request
    with(response) {
      assertThat(staffInvolved).isEqualTo(request.staffInvolved)
      assertThat(evidenceSecured).isEqualTo(request.evidenceSecured)
      assertThat(occurrenceReason).isEqualTo(request.occurrenceReason)
      assertThat(personsUsualBehaviour).isEqualTo(request.personsUsualBehaviour)
      assertThat(personsTrigger).isEqualTo(request.personsTrigger)
      assertThat(protectiveFactors).isEqualTo(request.protectiveFactors)
    }

    // Audit event saved
    with(csipRecordRepository.findByRecordUuid(recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.CREATED)
      assertThat(description).isEqualTo("Investigation with 0 interviews added to referral")
      assertThat(affectedComponents).containsOnly(AffectedComponent.Investigation)
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByCapturedName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(Source.NOMIS)
      assertThat(activeCaseLoadId).isNull()
    }

    // person.csip.record.updated domain event published
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveCsipDomainEventOnQueue()
    assertThat(event).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        eventType = DomainEventType.CSIP_UPDATED.eventType,
        additionalInformation = CsipAdditionalInformation(
          recordUuid = recordUuid,
          affectedComponents = setOf(AffectedComponent.Investigation),
          source = Source.NOMIS,
        ),
        version = 1,
        description = "Investigation with 0 interviews added to referral",
        occurredAt = event.occurredAt,
        detailUrl = "http://localhost:8080/csip-records/$recordUuid",
        personReference = PersonReference.withPrisonNumber(prisonNumber),
      ),
    )
  }

  private fun interviewRequest(roleCode: String = "OTHER", name: String = "Joe") =
    CreateInterviewRequest(
      interviewee = name,
      interviewDate = LocalDate.now(),
      intervieweeRoleCode = roleCode,
      interviewText = null,
    )

  private fun investigationRequest(vararg interviews: CreateInterviewRequest) = CreateInvestigationRequest(
    staffInvolved = "staffInvolved",
    evidenceSecured = "evidenceSecured",
    occurrenceReason = "occurrenceReason",
    personsUsualBehaviour = "personsUsualBehaviour",
    personsTrigger = "personsTrigger",
    protectiveFactors = "protectiveFactors",
    interviews = interviews.takeIf { it.isNotEmpty() }?.toList(),
  )

  private fun createInvestigationResponseSpec(
    recordUuid: UUID,
    request: CreateInvestigationRequest,
    source: Source = Source.DPS,
    username: String = TEST_USER,
  ) = webTestClient.post().uri("/csip-records/$recordUuid/referral/investigation").bodyValue(request)
    .headers(setAuthorisation(roles = listOf(ROLE_NOMIS)))
    .headers(setCsipRequestContext(source = source, username = username)).exchange()

  private fun createInvestigation(
    recordUuid: UUID,
    request: CreateInvestigationRequest,
    source: Source = Source.DPS,
    username: String = TEST_USER,
  ) = createInvestigationResponseSpec(recordUuid, request, source, username)
    .expectHeader().contentType(MediaType.APPLICATION_JSON)
    .expectBody<Investigation>()
    .returnResult().responseBody!!
}
