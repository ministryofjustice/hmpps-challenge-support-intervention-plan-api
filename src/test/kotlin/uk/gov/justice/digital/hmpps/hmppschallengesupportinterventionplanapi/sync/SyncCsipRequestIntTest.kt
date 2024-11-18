package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBodyList
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.SYSTEM_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.AttendeeRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.IdentifiedNeedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.ContributoryFactorRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.InterviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.ATTENDEE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.CONTRIBUTORY_FACTOR
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.IDENTIFIED_NEED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.INTERVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.INVESTIGATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.PLAN
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ValidInvestigationDetail.Companion.WITH_INTERVIEW_MESSAGE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.badSyncRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.personSummaryRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncCsipRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncDecisionRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncPlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncReviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.syncScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.newId
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.verifyAgainst
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.allOf

class SyncCsipRequestIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var contributoryFactorRepository: ContributoryFactorRepository

  @Autowired
  lateinit var interviewRepository: InterviewRepository

  @Autowired
  lateinit var identifiedNeedRepository: IdentifiedNeedRepository

  @Autowired
  lateinit var reviewRepository: ReviewRepository

  @Autowired
  lateinit var attendeeRepository: AttendeeRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(URL).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - wrong role`() {
    val response = webTestClient.put().uri(URL)
      .headers(setAuthorisation(roles = listOf("WRONG_ROLE")))
      .bodyValue(syncCsipRequest())
      .exchange().errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - no body`() {
    val response = webTestClient.put().uri(URL)
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI)))
      .exchange().errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Couldn't read request body")
    }
  }

  @Test
  fun `400 bad request - field validation`() {
    val response = syncCsipResponseSpec(badSyncRequest()).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(
        """
        |Validation failures: 
        |Action other must be <= 4000 characters
        |Area code must be <= 12 characters
        |Attendee name must be <= 100 characters
        |Attendee role must be <= 50 characters
        |Case manager name must be <= 100 characters
        |Completed by display name must be <= 255 characters
        |Completed by username must be <= 64 characters
        |Conclusion must be <= 4000 characters
        |Contribution must be <= 4000 characters
        |Contributory factor type code must be <= 12 characters
        |Decision outcome code must be <= 12 characters
        |Description of concern must be <= 4000 characters
        |Evidence secured must be <= 4000 characters
        |Identified need must be <= 1000 characters
        |Incident Location code must be <= 12 characters
        |Incident Type code must be <= 12 characters
        |Intervention must be <= 4000 characters
        |Interview text must be <= 4000 characters
        |Interviewee name must be <= 100 characters
        |Interviewee role code must be <= 12 characters
        |Involvement code must be <= 12 characters
        |Known reasons must be <= 4000 characters
        |Log code must be <= 10 characters
        |Name or names must be <= 1000 characters
        |Next steps must be <= 4000 characters
        |Occurrence reason must be <= 4000 characters
        |Other information must be <= 4000 characters
        |Person's trigger must be <= 4000 characters
        |Person's usual behaviour must be <= 4000 characters
        |Prison number must be <= 10 characters
        |Progression must be <= 4000 characters
        |Protective factors must be <= 4000 characters
        |Reason for decision must be <= 4000 characters
        |Recorded by display name must be <= 255 characters
        |Recorded by username must be <= 64 characters
        |Referer name must be <= 240 characters
        |Responsible person name must be <= 100 characters
        |Screening outcome code must be <= 12 characters
        |Signed off by role code must be <= 12 characters
        |Staff involved must be <= 4000 characters
        |Summary must be <= 4000 characters
        |
        """.trimMargin(),
      )
    }
  }

  @ParameterizedTest
  @MethodSource("referenceDataValidation")
  fun `400 bad request - when reference data code is invalid`(
    request: SyncCsipRequest,
    invalid: InvalidRd,
  ) {
    val response = syncCsipResponseSpec(request).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: ${invalid.type} ${invalid.message}")
      assertThat(developerMessage).isEqualTo("Details => ${invalid.type}:${invalid.code(request)}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - when multiple reference data codes are invalid`() {
    val response = syncCsipResponseSpec(
      syncCsipRequest(
        referral = syncReferralRequest(incidentTypeCode = NON_EXISTENT, incidentLocationCode = NON_EXISTENT),
      ),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Multiple invalid Reference Data")
      assertThat(developerMessage).isEqualTo("Details => Reference Data:{ INCIDENT_TYPE:[NON_EXISTENT], INCIDENT_LOCATION:[NON_EXISTENT] }")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - save a new csip record with empty investigation and no interview`() {
    val request = syncCsipRequest(
      referral = syncReferralRequest(
        saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(),
        investigation = syncInvestigationRequest(
          staffInvolved = null,
          evidenceSecured = null,
          occurrenceReason = null,
          personsUsualBehaviour = null,
          personsTrigger = null,
          protectiveFactors = null,
        ),
      ),
    )

    val response = syncCsipResponseSpec(request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(userMessage).isEqualTo("Validation failure: $WITH_INTERVIEW_MESSAGE")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: $WITH_INTERVIEW_MESSAGE")
    }
  }

  @Test
  fun `400 bad request - attempt to save invalid decision`() {
    val request = syncCsipRequest(
      referral = syncReferralRequest(
        decisionAndActions = syncDecisionRequest(
          outcomeCode = null,
          conclusion = null,
          signedOffByRole = null,
          nextSteps = null,
          actionOther = null,
          actions = setOf(),
        ),
      ),
    )

    val response = syncCsipResponseSpec(request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(userMessage).isEqualTo("Validation failure: At least one decision field or at least one action must be provided")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: At least one decision field or at least one action must be provided")
    }
  }

  @Test
  fun `400 bad request - save a new csip record with empty plan and no children`() {
    val request = syncCsipRequest(
      referral = syncReferralRequest(),
      plan = syncPlanRequest(caseManager = null, reasonForPlan = null, firstCaseReviewDate = null),
    )

    val response = syncCsipResponseSpec(request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(userMessage).isEqualTo("Validation failure: At least one of caseManager, reasonForPlan, firstCaseReviewDate, must be non null or at least one child record should be provided (identified need or review)")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: At least one of caseManager, reasonForPlan, firstCaseReviewDate, must be non null or at least one child record should be provided (identified need or review)")
    }
  }

  @Test
  fun `200 success - save a new csip record with children`() {
    val request = syncCsipRequest(
      logCode = LOG_CODE,
      prisonCodeWhenRecorded = "MDI",
      referral = syncReferralRequest(
        contributoryFactors = listOf(syncContributoryFactorRequest(), syncContributoryFactorRequest(typeCode = "BAS")),
        saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(),
        investigation = syncInvestigationRequest(interviews = listOf(syncInterviewRequest())),
        decisionAndActions = syncDecisionRequest(),
        isReferralComplete = true,
        completedBy = "CompletedBy",
        completedDate = LocalDate.now(),
        completedByDisplayName = "Display Name",
      ),
      plan = syncPlanRequest(
        identifiedNeeds = listOf(syncNeedRequest(), syncNeedRequest("Another need")),
        reviews = listOf(
          syncReviewRequest(
            actions = setOf(ReviewAction.CASE_NOTE),
            attendees = listOf(
              syncAttendeeRequest(),
              syncAttendeeRequest("Another attendee"),
            ),
          ),
        ),
      ),
    )
    val response = syncCsipRecord(request)
    assertThat(response.mappings.size).isEqualTo(9)

    val csipMapping = response.mappings.first { it.component == RECORD }
    assertThat(csipMapping).isNotNull()

    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    assertThat(saved).isNotNull()

    saved.verifyAgainst(request)

    val factorRequests = request.referral!!.contributoryFactors
    val factorMappings = response.mappings.filter { it.component == CONTRIBUTORY_FACTOR }
    assertThat(factorRequests.size).isEqualTo(factorMappings.size)
    val factors = contributoryFactorRepository.findAllById(factorMappings.map { it.uuid })
    assertThat(factors.size).isEqualTo(factorMappings.size)
    factors.forEach { i -> i.verifyAgainst(requireNotNull(factorRequests.find { it.legacyId == i.legacyId })) }

    val interviewRequests = request.referral!!.investigation!!.interviews
    val interviewMappings = response.mappings.filter { it.component == INTERVIEW }
    assertThat(interviewRequests.size).isEqualTo(interviewMappings.size)
    val interviews = interviewRepository.findAllById(interviewMappings.map { it.uuid })
    assertThat(interviews.size).isEqualTo(interviewMappings.size)
    interviews.forEach { i -> i.verifyAgainst(requireNotNull(interviewRequests.find { it.legacyId == i.legacyId })) }

    val needRequests = request.plan!!.identifiedNeeds
    val needMappings = response.mappings.filter { it.component == IDENTIFIED_NEED }
    assertThat(needRequests.size).isEqualTo(needMappings.size)
    val needs = identifiedNeedRepository.findAllById(needMappings.map { it.uuid })
    assertThat(needs.size).isEqualTo(needMappings.size)
    needs.forEach { i -> i.verifyAgainst(requireNotNull(needRequests.find { it.legacyId == i.legacyId })) }

    val reviewRequests = request.plan!!.reviews
    val reviewMappings = response.mappings.filter { it.component == REVIEW }
    assertThat(reviewRequests.size).isEqualTo(reviewMappings.size)
    val reviews = reviewRepository.findAllById(reviewMappings.map { it.uuid })
    assertThat(reviews.size).isEqualTo(reviewMappings.size)
    reviews.forEach { i -> i.verifyAgainst(requireNotNull(reviewRequests.find { it.legacyId == i.legacyId })) }

    val attendeeRequests = request.plan!!.reviews.flatMap { it.attendees }
    val attendeeMappings = response.mappings.filter { it.component == ATTENDEE }
    assertThat(attendeeRequests.size).isEqualTo(attendeeMappings.size)
    val attendees = attendeeRepository.findAllById(attendeeMappings.map { it.uuid })
    assertThat(attendees.size).isEqualTo(attendeeMappings.size)
    attendees.forEach { i -> i.verifyAgainst(requireNotNull(attendeeRequests.find { it.legacyId == i.legacyId })) }

    verifyAudit(
      saved,
      RevisionType.ADD,
      CsipComponent.entries.toSet(),
      nomisContext().copy(
        requestAt = request.actionedAt,
        username = request.actionedBy,
        userDisplayName = request.actionedBy,
        activeCaseLoadId = request.activeCaseloadId,
      ),
    )

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsTestQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 success - save a new csip record with null reason for screening decision`() {
    val request = syncCsipRequest(
      referral = syncReferralRequest(
        saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(reasonForDecision = null),
      ),
    )

    val response = syncCsipRecord(request)
    val csipMapping = response.mappings.first { it.component == RECORD }
    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    assertThat(saved).isNotNull()
    saved.verifyAgainst(request)
  }

  @Test
  fun `200 success - save a new csip record with empty investigation with interview`() {
    val request = syncCsipRequest(
      referral = syncReferralRequest(
        saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(),
        investigation = syncInvestigationRequest(
          staffInvolved = null,
          evidenceSecured = null,
          occurrenceReason = null,
          personsUsualBehaviour = null,
          personsTrigger = null,
          protectiveFactors = null,
          interviews = listOf(syncInterviewRequest()),
        ),
      ),
    )

    val response = syncCsipRecord(request)
    val csipMapping = response.mappings.first { it.component == RECORD }
    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    assertThat(saved).isNotNull()
    saved.verifyAgainst(request)
  }

  @Test
  fun `200 success - can update an existing csip record`() {
    val initial = dataSetup(generateCsipRecord()) {
      val referral = requireNotNull(it.withReferral().referral)
        .withSaferCustodyScreeningOutcome()
        .withContributoryFactor(type = givenReferenceData(ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE, "DEB"))
        .withInvestigation()
        .withDecisionAndActions()
      requireNotNull(referral.investigation)
        .withInterview(interviewee = "Initial Interviewee", interviewText = "The initial text of the interview")
      val plan = requireNotNull(it.withPlan().plan)
        .withNeed()
        .withReview()
      plan.reviews().first().withAttendee()
      it
    }

    val request = syncCsipRequest(
      uuid = initial.id,
      prisonNumber = initial.prisonNumber,
      personSummary = null,
      logCode = LOG_CODE,
      referral = syncReferralRequest(
        incidentTime = LocalTime.now(),
        contributoryFactors = listOf(
          syncContributoryFactorRequest(
            uuid = initial.referral!!.contributoryFactors().first().id,
          ),
          syncContributoryFactorRequest(typeCode = "BAS"),
        ),
        saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(),
        investigation = syncInvestigationRequest(
          interviews = listOf(
            syncInterviewRequest(uuid = initial.referral!!.investigation!!.interviews().first().id),
            syncInterviewRequest(),
          ),
        ),
        decisionAndActions = syncDecisionRequest(signedOffByRole = null),
      ),
      plan = syncPlanRequest(
        identifiedNeeds = listOf(
          syncNeedRequest(uuid = initial.plan!!.identifiedNeeds().first().id),
          syncNeedRequest("Another need"),
        ),
        reviews = listOf(
          syncReviewRequest(
            uuid = initial.plan!!.reviews().first().id,
            actions = setOf(ReviewAction.CASE_NOTE),
            attendees = listOf(
              syncAttendeeRequest(uuid = initial.plan!!.reviews().first().attendees().first().id),
              syncAttendeeRequest("Another attendee"),
            ),
          ),
          syncReviewRequest(
            actions = setOf(ReviewAction.CSIP_UPDATED),
            attendees = listOf(
              syncAttendeeRequest(),
              syncAttendeeRequest("Another attendee"),
            ),
          ),
        ),
      ),
    )

    val response = syncCsipRecord(request)
    assertThat(response.mappings.size).isEqualTo(7)

    val saved = csipRecordRepository.getCsipRecord(initial.id)
    assertThat(saved).isNotNull()

    saved.verifyAgainst(request)

    val factorRequests = request.referral!!.contributoryFactors
    val factors = contributoryFactorRepository.findAllById(factorRequests.map { it.id })
    factors.forEach { i -> i.verifyAgainst(requireNotNull(factorRequests.find { it.legacyId == i.legacyId })) }

    val interviewRequests = request.referral!!.investigation!!.interviews
    val interviews = interviewRepository.findAllById(interviewRequests.map { it.id })
    interviews.forEach { i -> i.verifyAgainst(requireNotNull(interviewRequests.find { it.legacyId == i.legacyId })) }

    val needRequests = request.plan!!.identifiedNeeds
    val needs = identifiedNeedRepository.findAllById(needRequests.map { it.id })
    needs.forEach { i -> i.verifyAgainst(requireNotNull(needRequests.find { it.legacyId == i.legacyId })) }

    val reviewRequests = request.plan!!.reviews
    val reviews = reviewRepository.findAllById(reviewRequests.map { it.id })
    reviews.forEach { i -> i.verifyAgainst(requireNotNull(reviewRequests.find { it.legacyId == i.legacyId })) }

    val attendeeRequests = request.plan!!.reviews.flatMap { it.attendees }
    val attendees = attendeeRepository.findAllById(attendeeRequests.map { it.id })
    attendees.forEach { i -> i.verifyAgainst(requireNotNull(attendeeRequests.find { it.legacyId == i.legacyId })) }

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsTestQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 success - existing ids returned when updating an existing record from legacy id`() {
    val initial = dataSetup(generateCsipRecord(legacyId = newId())) {
      val referral = requireNotNull(it.withReferral().referral)
        .withSaferCustodyScreeningOutcome()
        .withContributoryFactor(legacyId = newId())
        .withInvestigation()
        .withDecisionAndActions()
      requireNotNull(referral.investigation)
        .withInterview(legacyId = newId())
      val plan = requireNotNull(it.withPlan().plan)
        .withNeed(legacyId = newId())
        .withReview(legacyId = newId())
      plan.reviews().first().withAttendee(legacyId = newId())
      it
    }

    val referral = checkNotNull(initial.referral)
    val factor = referral.contributoryFactors().first()
    val interview = referral.investigation!!.interviews().first()
    val plan = checkNotNull(initial.plan)
    val need = plan.identifiedNeeds().first()
    val review = plan.reviews().first()
    val attendee = review.attendees().first()

    val request = syncCsipRequest(
      id = checkNotNull(initial.legacyId),
      logCode = LOG_CODE,
      referral = syncReferralRequest(
        incidentTime = LocalTime.now(),
        contributoryFactors = listOf(syncContributoryFactorRequest(id = factor.legacyId!!)),
        saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(),
        investigation = syncInvestigationRequest(interviews = listOf(syncInterviewRequest(id = interview.legacyId!!))),
        decisionAndActions = syncDecisionRequest(),
      ),
      plan = syncPlanRequest(
        identifiedNeeds = listOf(syncNeedRequest(id = need.legacyId!!)),
        reviews = listOf(
          syncReviewRequest(
            id = review.legacyId!!,
            actions = setOf(ReviewAction.CASE_NOTE),
            attendees = listOf(syncAttendeeRequest(id = attendee.legacyId!!)),
          ),
        ),
      ),
    )

    val response = syncCsipRecord(request)
    assertThat(response.mappings.size).isEqualTo(6)

    assertThat(response.mappings.single { it.component == RECORD })
      .isEqualTo(ResponseMapping(RECORD, request.legacyId, initial.id))

    val factorRequest = request.referral!!.contributoryFactors.first()
    assertThat(response.mappings.single { it.component == CONTRIBUTORY_FACTOR })
      .isEqualTo(ResponseMapping(CONTRIBUTORY_FACTOR, factorRequest.legacyId, factor.id))

    val interviewRequest = request.referral!!.investigation!!.interviews.first()
    assertThat(response.mappings.single { it.component == INTERVIEW })
      .isEqualTo(ResponseMapping(INTERVIEW, interviewRequest.legacyId, interview.id))

    val needRequest = request.plan!!.identifiedNeeds.first()
    assertThat(response.mappings.single { it.component == IDENTIFIED_NEED })
      .isEqualTo(ResponseMapping(IDENTIFIED_NEED, needRequest.legacyId, need.id))

    val reviewRequest = request.plan!!.reviews.first()
    assertThat(response.mappings.single { it.component == REVIEW })
      .isEqualTo(ResponseMapping(REVIEW, reviewRequest.legacyId, review.id))

    val attendeeRequest = reviewRequest.attendees.first()
    assertThat(response.mappings.single { it.component == ATTENDEE })
      .isEqualTo(ResponseMapping(ATTENDEE, attendeeRequest.legacyId, attendee.id))

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsTestQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 success - save a new csip record with empty plan with identified needs`() {
    val request = syncCsipRequest(
      referral = syncReferralRequest(),
      plan = syncPlanRequest(
        caseManager = null,
        reasonForPlan = null,
        firstCaseReviewDate = null,
        identifiedNeeds = listOf(syncNeedRequest()),
      ),
    )

    val response = syncCsipRecord(request)
    val csipMapping = response.mappings.first { it.component == RECORD }
    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    saved.verifyAgainst(request)
  }

  @Test
  fun `200 success - save a new csip record with conclusion and no outcome`() {
    val request = syncCsipRequest(
      referral = syncReferralRequest(
        decisionAndActions = syncDecisionRequest(
          outcomeCode = null,
          signedOffByRole = null,
          conclusion = "A conclusion without outcome",
          actions = setOf(),
        ),
      ),
    )

    val response = syncCsipRecord(request)
    val csipMapping = response.mappings.first { it.component == RECORD }
    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    saved.verifyAgainst(request)
  }

  @Test
  fun `200 success - save a new csip record with decision actions`() {
    val request = syncCsipRequest(
      referral = syncReferralRequest(
        decisionAndActions = syncDecisionRequest(
          outcomeCode = null,
          signedOffByRole = null,
          conclusion = null,
        ),
      ),
    )

    val response = syncCsipRecord(request)
    val csipMapping = response.mappings.first { it.component == RECORD }
    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    saved.verifyAgainst(request)
  }

  @Test
  fun `200 success - concurrently saves the same investigation`() {
    val investigationRequest = syncInvestigationRequest(
      interviews = listOf(syncInterviewRequest(), syncInterviewRequest()),
    )
    val csipRequest = syncCsipRequest(referral = syncReferralRequest(investigation = investigationRequest))
    val request1 = CompletableFuture.supplyAsync { syncCsipRecord(csipRequest) }
    val request2 = CompletableFuture.supplyAsync {
      syncCsipRecord(
        csipRequest.copy(
          referral = csipRequest.referral!!.copy(investigation = investigationRequest.copy(interviews = listOf())),
        ),
      )
    }

    allOf(request1, request2).join()

    val csipMapping = request1.get().mappings.firstOrNull { it.component == RECORD }
      ?: request2.get().mappings.first { it.component == RECORD }
    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    saved.verifyAgainst(csipRequest)
  }

  @Test
  fun `200 success - concurrently updates the same investigation`() {
    val csip = dataSetup(generateCsipRecord(legacyId = newId()).withReferral()) {
      requireNotNull(it.referral).withInvestigation()
      it
    }

    val investigationRequest = syncInvestigationRequest(occurrenceReason = "Updated concurrently")
    val csipRequest = syncCsipRequest(
      id = csip.legacyId!!,
      uuid = csip.id,
      prisonNumber = csip.prisonNumber,
      referral = syncReferralRequest(investigation = investigationRequest),
    )
    val request1 = CompletableFuture.supplyAsync { syncCsipRecord(csipRequest) }
    val request2 = CompletableFuture.supplyAsync {
      syncCsipRecord(
        csipRequest.copy(
          referral = csipRequest.referral!!.copy(investigation = investigationRequest.copy(interviews = listOf())),
        ),
      )
    }

    allOf(request1, request2).join()

    val saved = csipRecordRepository.getCsipRecord(csip.id)
    saved.verifyAgainst(csipRequest)
  }

  @Test
  fun `200 success - concurrently updates the same reviews`() {
    val csip = dataSetup(generateCsipRecord(legacyId = newId()).withReferral()) {
      requireNotNull(it.referral).withInvestigation()
      it.withPlan()
      val plan = requireNotNull(it.plan).withReview(legacyId = newId())
      requireNotNull(plan.reviews().first()).withAttendee(legacyId = newId())
      it
    }
    val review = csip.plan!!.reviews().first()
    val attendee = review.attendees().first()

    val investigationRequest = syncInvestigationRequest(occurrenceReason = "Updated concurrently")
    val planRequest = syncPlanRequest(
      reviews = listOf(
        syncReviewRequest(
          id = review.legacyId!!,
          uuid = review.id,
          attendees = listOf(syncAttendeeRequest(id = attendee.legacyId!!, uuid = attendee.id)),
        ),
      ),
    )
    val csipRequest = syncCsipRequest(
      id = csip.legacyId!!,
      uuid = csip.id,
      prisonNumber = csip.prisonNumber,
      referral = syncReferralRequest(investigation = investigationRequest),
      plan = planRequest,
    )
    val request1 = CompletableFuture.supplyAsync { syncCsipRecord(csipRequest) }
    val request2 = CompletableFuture.supplyAsync { syncCsipRecord(csipRequest) }
    val request3 = CompletableFuture.supplyAsync { syncCsipRecord(csipRequest) }
    val request4 = CompletableFuture.supplyAsync { syncCsipRecord(csipRequest) }

    allOf(request1, request2, request3, request4).join()

    val saved = csipRecordRepository.getCsipRecord(csip.id)
    saved.verifyAgainst(csipRequest)
  }

  @Test
  fun `200 success - save a new csip record where person summary already exists`() {
    val existing = dataSetup(generateCsipRecord()) { it.withCompletedReferral().withPlan() }
    val request = syncCsipRequest(referral = syncReferralRequest(), prisonNumber = existing.prisonNumber)

    val response = syncCsipRecord(request)
    val csipMapping = response.mappings.first { it.component == RECORD }
    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    assertThat(saved).isNotNull()
    saved.verifyAgainst(request)
    saved.personSummary.verifyAgainst(
      with(existing.personSummary) {
        prisoner(request.prisonNumber, firstName, lastName, prisonCode, status, cellLocation)
      },
    )
  }

  @Test
  fun `200 success - save a new csip record providing person summary details`() {
    val request = syncCsipRequest(referral = syncReferralRequest(), personSummary = personSummaryRequest())

    val response = syncCsipRecord(request)
    val csipMapping = response.mappings.first { it.component == RECORD }
    val saved = csipRecordRepository.getCsipRecord(csipMapping.uuid)
    assertThat(saved).isNotNull()
    saved.verifyAgainst(request)
    saved.personSummary.verifyAgainst(
      with(request.personSummary!!) {
        prisoner(request.prisonNumber, firstName, lastName, prisonCode, status, cellLocation)
      },
    )
  }

  @Test
  fun `204 no content - delete csip record`() {
    val record = dataSetup(generateCsipRecord()) {
      val referral = requireNotNull(it.withReferral().referral)
        .withContributoryFactor()
        .withContributoryFactor()
        .withInvestigation()
      requireNotNull(referral.investigation)
        .withInterview(interviewDate = LocalDate.now().minusDays(2))
        .withInterview(interviewDate = LocalDate.now().minusDays(1))
        .withInterview()
      val plan = requireNotNull(it.withPlan().plan)
        .withNeed()
        .withReview()
        .withReview()
      plan.reviews().first().withAttendee()
      it
    }

    val factorUuids = record.referral!!.contributoryFactors().map { it.id }.toSet()
    assertThat(factorUuids).hasSize(2)

    val interviewIds = record.referral!!.investigation!!.interviews().map { it.id }.toSet()
    assertThat(interviewIds).hasSize(3)

    val needsIds = record.plan!!.identifiedNeeds().map { it.id }.toSet()
    assertThat(needsIds).hasSize(1)

    val reviewIds = record.plan!!.reviews().map { it.id }.toSet()
    assertThat(reviewIds).hasSize(2)

    val attendeeIds = record.plan!!.reviews().flatMap { r -> r.attendees().map { it.id } }
    assertThat(attendeeIds).hasSize(1)

    val legacyActioned = DefaultLegacyActioned(LocalDateTime.now(), "actionedBy", "LEI")
    deleteCsip(record.id, legacyActioned)

    val affectedComponents =
      setOf(
        RECORD, REFERRAL, CONTRIBUTORY_FACTOR, INVESTIGATION, INTERVIEW, PLAN, IDENTIFIED_NEED, REVIEW, ATTENDEE,
      )
    verifyDoesNotExist(csipRecordRepository.findById(record.id)) { IllegalStateException("CSIP record not deleted") }
    verifyDoesNotExist(personSummaryRepository.findByIdOrNull(record.prisonNumber)) {
      IllegalStateException("Person Summary not deleted")
    }
    verifyAudit(
      record,
      RevisionType.DEL,
      affectedComponents,
      nomisContext().copy(
        requestAt = legacyActioned.actionedAt,
        username = legacyActioned.actionedBy,
        userDisplayName = legacyActioned.actionedBy,
        activeCaseLoadId = legacyActioned.activeCaseloadId,
      ),
    )
  }

  @Test
  fun `204 no content - delete csip record without actioned by information`() {
    val record = dataSetup(generateCsipRecord()) { it.withReferral() }

    val legacyActioned = DefaultLegacyActioned(LocalDateTime.now(), null, null)
    deleteCsip(record.id, legacyActioned)

    val affectedComponents = setOf(RECORD, REFERRAL)
    verifyDoesNotExist(csipRecordRepository.findById(record.id)) { IllegalStateException("CSIP record not deleted") }
    verifyDoesNotExist(personSummaryRepository.findByIdOrNull(record.prisonNumber)) {
      IllegalStateException("Person Summary not deleted")
    }
    verifyAudit(
      record,
      RevisionType.DEL,
      affectedComponents,
      nomisContext().copy(
        requestAt = legacyActioned.actionedAt,
        username = SYSTEM_USER_NAME,
        userDisplayName = SYSTEM_DISPLAY_NAME,
      ),
    )
  }

  @Test
  fun `204 no content - delete csip record without deleting person summary`() {
    val personSummary = prisoner().toPersonSummary()
    dataSetup(generateCsipRecord(personSummary)) { it.withCompletedReferral() }
    val toDelete = dataSetup(generateCsipRecord(personSummary)) { it.withReferral() }

    val legacyActioned = DefaultLegacyActioned(LocalDateTime.now(), null, null)
    deleteCsip(toDelete.id, legacyActioned)

    verifyDoesNotExist(csipRecordRepository.findById(toDelete.id)) { IllegalStateException("CSIP record not deleted") }
    assertThat(personSummaryRepository.findByIdOrNull(toDelete.prisonNumber)).isNotNull()
  }

  @Test
  fun `200 ok - returns an empty list when no csips exist`() {
    val res = getCsipRecords(prisonNumber())
    assertThat(res).isEmpty()
  }

  @Test
  fun `200 ok - returns all csip records for a prison number`() {
    val prisoner = prisoner().toPersonSummary()
    val csip1 = dataSetup(generateCsipRecord(prisoner).withCompletedReferral().withPlan()) {
      val referral = requireNotNull(it.referral).withContributoryFactor()
        .withSaferCustodyScreeningOutcome().withInvestigation().withDecisionAndActions()
      requireNotNull(referral.investigation).withInterview()
      requireNotNull(it.plan).withNeed("First need").withNeed("Second need").withReview()
      it
    }
    val csip2 = dataSetup(generateCsipRecord(prisoner).withCompletedReferral().withPlan()) {
      requireNotNull(it.referral).withContributoryFactor().withContributoryFactor()
        .withSaferCustodyScreeningOutcome().withDecisionAndActions()
      requireNotNull(it.plan).withNeed()
      it
    }

    val res = getCsipRecords(prisoner.prisonNumber)
    assertThat(res).hasSize(2)
    res.first { it.recordUuid == csip1.id }.verifyAgainst(csip1)
    res.first { it.recordUuid == csip2.id }.verifyAgainst(csip2)
  }

  private fun syncCsipResponseSpec(
    request: SyncCsipRequest,
  ) = webTestClient.put().uri(URL)
    .bodyValue(request)
    .headers(setAuthorisation(isUserToken = false, roles = listOf(ROLE_NOMIS))).exchange()

  private fun deleteCsip(uuid: UUID, legacyActioned: LegacyActioned) = webTestClient.method(HttpMethod.DELETE)
    .uri("$URL/$uuid")
    .bodyValue(legacyActioned)
    .headers(setAuthorisation(isUserToken = false, roles = listOf(ROLE_NOMIS)))
    .exchange().expectStatus().isNoContent

  private fun getCsipRecords(prisonNumber: String): List<CsipRecord> = webTestClient.get().uri("$URL/$prisonNumber")
    .headers(setAuthorisation(isUserToken = false, roles = listOf(ROLE_NOMIS))).exchange()
    .expectStatus().isOk.expectBodyList<CsipRecord>().returnResult().responseBody!!

  private fun syncCsipRecord(request: SyncCsipRequest) = syncCsipResponseSpec(request).successResponse<SyncResponse>()

  companion object {
    private const val URL = "/sync/csip-records"
    private const val NON_EXISTENT = "NON_EXISTENT"
    private const val INVALID = "is invalid"

    @JvmStatic
    fun referenceDataValidation() = listOf(
      Arguments.of(
        syncCsipRequest(referral = syncReferralRequest(incidentTypeCode = NON_EXISTENT)),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, { it.referral!!.incidentTypeCode }, INVALID),
      ),
      Arguments.of(
        syncCsipRequest(referral = syncReferralRequest(incidentLocationCode = NON_EXISTENT)),
        InvalidRd(
          ReferenceDataType.INCIDENT_LOCATION,
          { it.referral!!.incidentLocationCode },
          INVALID,
        ),
      ),
      Arguments.of(
        syncCsipRequest(referral = syncReferralRequest(refererAreaCode = NON_EXISTENT)),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, { it.referral!!.refererAreaCode }, INVALID),
      ),
      Arguments.of(
        syncCsipRequest(referral = syncReferralRequest(incidentInvolvementCode = NON_EXISTENT)),
        InvalidRd(
          ReferenceDataType.INCIDENT_INVOLVEMENT,
          { it.referral!!.incidentInvolvementCode!! },
          INVALID,
        ),
      ),
      Arguments.of(
        syncCsipRequest(
          referral = syncReferralRequest(
            contributoryFactors = listOf(syncContributoryFactorRequest(typeCode = NON_EXISTENT)),
          ),
        ),
        InvalidRd(
          ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
          { it.referral!!.contributoryFactors.first().factorTypeCode },
          INVALID,
        ),
      ),
      Arguments.of(
        syncCsipRequest(
          referral = syncReferralRequest(
            saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(
              outcomeCode = NON_EXISTENT,
            ),
          ),
        ),
        InvalidRd(
          ReferenceDataType.SCREENING_OUTCOME_TYPE,
          { it.referral!!.saferCustodyScreeningOutcome!!.outcomeTypeCode },
          INVALID,
        ),
      ),
      Arguments.of(
        syncCsipRequest(referral = syncReferralRequest(decisionAndActions = syncDecisionRequest(outcomeCode = NON_EXISTENT))),
        InvalidRd(
          ReferenceDataType.DECISION_OUTCOME_TYPE,
          { it.referral!!.decisionAndActions!!.outcomeTypeCode!! },
          INVALID,
        ),
      ),
    )

    data class InvalidRd(
      val type: ReferenceDataType,
      val code: (SyncCsipRequest) -> String,
      val message: String,
    )
  }
}
