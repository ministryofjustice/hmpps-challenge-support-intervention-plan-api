package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.InterviewAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.InterviewDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class InvestigationsIntTest(
  @Autowired private val csipRecordRepository: CsipRecordRepository,
  @Autowired private val referenceDataRepository: ReferenceDataRepository,
) : IntegrationTestBase() {
  private val intervieweeRole = referenceDataRepository.findByDomain(ReferenceDataType.INTERVIEWEE_ROLE).first()
  private val incidentType = referenceDataRepository.findByDomain(ReferenceDataType.INCIDENT_TYPE).first()
  private val incidentLocation = referenceDataRepository.findByDomain(ReferenceDataType.INCIDENT_LOCATION).first()
  private val incidentInvolvement = referenceDataRepository.findByDomain(ReferenceDataType.INCIDENT_INVOLVEMENT).first()
  private val refererAreaOfWork = referenceDataRepository.findByDomain(ReferenceDataType.AREA_OF_WORK).first()

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
    val response = webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/investigation")
      .bodyValue(investigationRequest(interviewRequest(roleCode = "n".repeat(13))))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
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
    val response = webTestClient.post().uri("/csip-records/${UUID.randomUUID()}/referral/investigation")
      .bodyValue(investigationRequest(interviewRequest(roleCode = "WRONG_CODE")))
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java)
      .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: INTERVIEWEE_ROLE code 'WRONG_CODE' does not exist")
      assertThat(developerMessage).isEqualTo("INTERVIEWEE_ROLE code 'WRONG_CODE' does not exist")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - CSIP record missing a referral`() {
    val csipRecord = createCsipRecord(withReferral = false)
    val recordUuid = csipRecord.recordUuid

    val response =
      webTestClient.post().uri("/csip-records/$recordUuid/referral/investigation").bodyValue(interviewRequest())
        .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
        .headers(setCsipRequestContext()).exchange().expectStatus().isBadRequest.expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

    with(response!!) {
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
    val response =
      webTestClient.post().uri("/csip-records/$recordUuid/referral/investigation").bodyValue(interviewRequest())
        .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
        .headers(setCsipRequestContext()).exchange().expectStatus().isNotFound.expectBody(ErrorResponse::class.java)
        .returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("No resource found failure: Could not find CSIP record with UUID $recordUuid")
      assertThat(developerMessage).isEqualTo("Could not find CSIP record with UUID $recordUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `409 conflict - CSIP record already has Screening Outcome created`() {
    val csipRecord = createCsipRecord()
    val recordUuid = csipRecord.recordUuid

    csipRecordRepository.save(
      csipRecord.let {
        it.referral!!.createInvestigation(
          createRequest = investigationRequest(
            interviewRequest(),
            interviewRequest(),
          ),
          intervieweeRoleMap = mapOf(intervieweeRole.code to intervieweeRole),
          actionedAt = LocalDateTime.now(),
          actionedBy = "actionedBy",
          actionedByDisplayName = "actionedByDisplayName",
          source = Source.DPS,
          activeCaseLoadId = PRISON_CODE_LEEDS,
        )
      },
    )

    val response =
      webTestClient.post().uri("/csip-records/$recordUuid/referral/investigation").bodyValue(investigationRequest())
        .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
        .headers(setCsipRequestContext()).exchange()
        .expectStatus().is4xxClientError.expectBody(ErrorResponse::class.java).returnResult().responseBody

    with(response!!) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: CSIP Record with UUID: $recordUuid already has an Investigation created.")
      assertThat(developerMessage).isEqualTo("CSIP Record with UUID: $recordUuid already has an Investigation created.")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `create investigation via DPS UI`() {
    val recordUuid = createCsipRecord().recordUuid
    val request = investigationRequest(
      interviewRequest(name = "John"),
      interviewRequest(name = "Jane"),
    )

    val response = webTestClient.post().uri("/csip-records/$recordUuid/referral/investigation").bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext()).exchange().expectStatus().isCreated.expectHeader()
      .contentType(MediaType.APPLICATION_JSON).expectBody(Investigation::class.java).returnResult().responseBody!!

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
      assertThat(isInvestigationAffected).isTrue()
      assertThat(isInterviewAffected).isTrue()
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(TEST_USER)
      assertThat(actionedByCapturedName).isEqualTo(TEST_USER_NAME)
      assertThat(source).isEqualTo(Source.DPS)
      assertThat(reason).isEqualTo(Reason.USER)
      assertThat(activeCaseLoadId).isEqualTo(PRISON_CODE_LEEDS)
    }

    // prisoner-csip.csip-record-updated domain event published
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 3 }

    val domainEvents = hmppsEventsQueue.receiveDomainEventsOnQueue()

    val csipEvent = domainEvents.find { it is CsipDomainEvent }!!
    assertThat(csipEvent).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        DomainEventType.CSIP_UPDATED.eventType,
        CsipAdditionalInformation(
          url = "http://localhost:8080/csip-records/$recordUuid",
          recordUuid = recordUuid,
          prisonNumber = PRISON_NUMBER,
          isRecordAffected = false,
          isReferralAffected = false,
          isContributoryFactorAffected = false,
          isSaferCustodyScreeningOutcomeAffected = false,
          isInvestigationAffected = true,
          isInterviewAffected = true,
          isDecisionAndActionsAffected = false,
          isPlanAffected = false,
          isIdentifiedNeedAffected = false,
          isReviewAffected = false,
          isAttendeeAffected = false,
          source = Source.DPS,
          reason = Reason.USER,
        ),
        1,
        "Investigation with 2 interviews added to referral",
        csipEvent.occurredAt,
      ),
    )

    val interviewEvents = domainEvents.filterIsInstance<InterviewDomainEvent>()
    assertThat(interviewEvents).hasSize(2)
    assertThat(interviewEvents[0]).usingRecursiveComparison().ignoringFields("additionalInformation.interviewUuid").isEqualTo(
      InterviewDomainEvent(
        eventType = DomainEventType.INTERVIEW_CREATED.eventType,
        additionalInformation = InterviewAdditionalInformation(
          url = "http://localhost:8080/csip-records/$recordUuid",
          interviewUuid = recordUuid,
          recordUuid = recordUuid,
          prisonNumber = PRISON_NUMBER,
          source = Source.DPS,
          reason = Reason.USER,
        ),
        description = DomainEventType.INTERVIEW_CREATED.description,
        occurredAt = interviewEvents[0].occurredAt,
      ),
    )
  }

  @Test
  fun `create investigation via NOMIS`() {
    val recordUuid = createCsipRecord().recordUuid
    val request = investigationRequest()

    val response = webTestClient.post().uri("/csip-records/$recordUuid/referral/investigation").bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_NOMIS)))
      .headers(setCsipRequestContext(source = Source.NOMIS, username = NOMIS_SYS_USER)).exchange()
      .expectStatus().isCreated.expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(Investigation::class.java).returnResult().responseBody!!

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
      assertThat(isInvestigationAffected).isTrue()
      assertThat(isInterviewAffected).isFalse()
      assertThat(actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(actionedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(actionedByCapturedName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
      assertThat(source).isEqualTo(Source.NOMIS)
      assertThat(reason).isEqualTo(Reason.USER)
      assertThat(activeCaseLoadId).isNull()
    }

    // prisoner-csip.csip-record-updated domain event published
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val event = hmppsEventsQueue.receiveCsipDomainEventOnQueue()
    assertThat(event).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        DomainEventType.CSIP_UPDATED.eventType,
        CsipAdditionalInformation(
          url = "http://localhost:8080/csip-records/$recordUuid",
          recordUuid = recordUuid,
          prisonNumber = PRISON_NUMBER,
          isRecordAffected = false,
          isReferralAffected = false,
          isContributoryFactorAffected = false,
          isSaferCustodyScreeningOutcomeAffected = false,
          isInvestigationAffected = true,
          isInterviewAffected = false,
          isDecisionAndActionsAffected = false,
          isPlanAffected = false,
          isIdentifiedNeedAffected = false,
          isReviewAffected = false,
          isAttendeeAffected = false,
          source = Source.NOMIS,
          reason = Reason.USER,
        ),
        1,
        "Investigation with 0 interviews added to referral",
        event.occurredAt,
      ),
    )
  }

  private fun createCsipRecord(withReferral: Boolean = true) = csipRecordRepository.saveAndFlush(
    CsipRecord(
      recordUuid = UUID.randomUUID(),
      prisonNumber = PRISON_NUMBER,
      prisonCodeWhenRecorded = PRISON_CODE_LEEDS,
      logNumber = "LOG",
      createdAt = LocalDateTime.now(),
      createdBy = "te",
      createdByDisplayName = "Bobbie Shepard",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
    ).let {
      if (withReferral) {
        it.setReferral(
          Referral(
            csipRecord = it,
            incidentDate = LocalDate.now(),
            referredBy = "referredBy",
            referralDate = LocalDate.now(),
            descriptionOfConcern = "descriptionOfConcern",
            knownReasons = "knownReasons",
            otherInformation = "otherInformation",
            saferCustodyTeamInformed = false,
            referralComplete = true,
            referralCompletedBy = "referralCompletedBy",
            referralCompletedByDisplayName = "referralCompletedByDisplayName",
            referralCompletedDate = LocalDate.now(),
            incidentType = incidentType,
            incidentLocation = incidentLocation,
            refererAreaOfWork = refererAreaOfWork,
            incidentInvolvement = incidentInvolvement,
          ),
        )
      } else {
        it
      }
    },
  )

  private fun interviewRequest(roleCode: String = intervieweeRole.code, name: String = "Joe") = CreateInterviewRequest(
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
}
