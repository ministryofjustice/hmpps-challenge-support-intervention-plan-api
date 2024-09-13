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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.AttendeeRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ContributoryFactorRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.IdentifiedNeedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.InterviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReviewRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.NomisIdGenerator.prisonNumber
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.badSyncRequest
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.SyncRequestGenerator.withModifiedDetail
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class SyncCsipRequestRecordIntTest : IntegrationTestBase() {

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
    webTestClient.put().uri(urlToTest()).exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `403 forbidden - wrong role`() {
    val response = webTestClient.put().uri(urlToTest())
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
    val response = webTestClient.put().uri(urlToTest())
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
        |Case manager must be <= 100 characters
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
        |Next step must be <= 4000 characters
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
  fun `200 success - save a new csip record with children`() {
    val request = syncCsipRequest(
      logCode = LOG_CODE,
      prisonCodeWhenRecorded = "MDI",
      referral = syncReferralRequest(
        contributoryFactors = listOf(syncContributoryFactorRequest(), syncContributoryFactorRequest(typeCode = "BAS")),
        saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(),
        investigation = syncInvestigationRequest(interviews = listOf(syncInterviewRequest())),
        decisionAndActions = syncDecisionRequest(),
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
        userDisplayName = request.actionedByDisplayName,
        activeCaseLoadId = request.activeCaseloadId,
      ),
      validateEntityWithContext = false,
    )

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 success - can update an existing csip record`() {
    val initial = dataSetup(generateCsipRecord(prisonNumber())) {
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
      logCode = LOG_CODE,
      referral = syncReferralRequest(
        incidentTime = LocalTime.now(),
        contributoryFactors = listOf(
          syncContributoryFactorRequest(
            uuid = initial.referral!!.contributoryFactors().first().id,
          ).withModifiedDetail(),
          syncContributoryFactorRequest(typeCode = "BAS"),
        ),
        saferCustodyScreeningOutcome = syncScreeningOutcomeRequest(),
        investigation = syncInvestigationRequest(
          interviews = listOf(
            syncInterviewRequest(uuid = initial.referral!!.investigation!!.interviews().first().id),
            syncInterviewRequest(),
          ),
        ),
        decisionAndActions = syncDecisionRequest(),
      ),
      plan = syncPlanRequest(
        identifiedNeeds = listOf(
          syncNeedRequest(uuid = initial.plan!!.identifiedNeeds().first().id).withModifiedDetail(),
          syncNeedRequest("Another need"),
        ),
        reviews = listOf(
          syncReviewRequest(
            uuid = initial.plan!!.reviews().first().id,
            actions = setOf(ReviewAction.CASE_NOTE),
            attendees = listOf(
              syncAttendeeRequest(uuid = initial.plan!!.reviews().first().attendees().first().id).withModifiedDetail(),
              syncAttendeeRequest("Another attendee"),
            ),
          ).withModifiedDetail(),
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

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `204 no content - delete csip record`() {
    val record = dataSetup(generateCsipRecord(prisonNumber())) {
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

    val legacyActioned = DefaultLegacyActioned(LocalDateTime.now(), "actionedBy", "actionedByDisplayName", "LEI")
    deleteCsip(record.id, legacyActioned)

    val affectedComponents =
      setOf(
        RECORD, REFERRAL, CONTRIBUTORY_FACTOR, INVESTIGATION, INTERVIEW, PLAN, IDENTIFIED_NEED, REVIEW, ATTENDEE,
      )
    verifyDoesNotExist(csipRecordRepository.findById(record.id)) { IllegalStateException("CSIP record not deleted") }
    verifyAudit(
      record,
      RevisionType.DEL,
      affectedComponents,
      nomisContext().copy(
        requestAt = legacyActioned.actionedAt,
        username = legacyActioned.actionedBy,
        userDisplayName = legacyActioned.actionedByDisplayName,
        activeCaseLoadId = legacyActioned.activeCaseloadId,
      ),
    )
  }

  private fun urlToTest() = "/sync/csip-records"

  private fun syncCsipResponseSpec(
    request: SyncCsipRequest,
  ) = webTestClient.put().uri(urlToTest())
    .bodyValue(request)
    .headers(setAuthorisation(isUserToken = false, roles = listOf(ROLE_NOMIS))).exchange()

  private fun deleteCsip(uuid: UUID, legacyActioned: LegacyActioned) = webTestClient.method(HttpMethod.DELETE)
    .uri("${urlToTest()}/$uuid")
    .bodyValue(legacyActioned)
    .headers(setAuthorisation(isUserToken = false, roles = listOf(ROLE_NOMIS)))
    .exchange().expectStatus().isNoContent

  private fun syncCsipRecord(request: SyncCsipRequest) = syncCsipResponseSpec(request).successResponse<SyncResponse>()

  companion object {
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
          { it.referral!!.decisionAndActions!!.outcomeTypeCode },
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
