package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.context.jdbc.Sql
import org.springframework.test.context.jdbc.SqlMergeMode
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.USERNAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.AuditEventRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBaseInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBasicDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.Notification
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_DELETED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.DECISION_SIGNER_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INTERVIEWEE_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.ManageUsersExtension
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PrisonerSearchExtension
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PrisonerSearchExtension.Companion.prisonerSearch
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.withReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.IdGenerator
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.set
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.setByName
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDate
import java.util.UUID

@SqlMergeMode(SqlMergeMode.MergeMode.MERGE)
@Sql("classpath:test_data/reset-database.sql")
@ExtendWith(HmppsAuthApiExtension::class, ManageUsersExtension::class, PrisonerSearchExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @SpyBean
  lateinit var hmppsQueueService: HmppsQueueService

  @Autowired
  lateinit var csipRecordRepository: CsipRecordRepository

  @Autowired
  lateinit var referenceDataRepository: ReferenceDataRepository

  @Autowired
  lateinit var auditEventRepository: AuditEventRepository

  internal val hmppsEventsQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventtestqueue")
      ?: throw MissingQueueException("hmppseventtestqueue queue not found")
  }

  internal fun HmppsQueue.countAllMessagesOnQueue() =
    sqsClient.countAllMessagesOnQueue(queueUrl).get()

  internal fun HmppsQueue.receiveMessageOnQueue() =
    sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).get().messages().single()

  internal fun HmppsQueue.receiveCsipDomainEventOnQueue() =
    receiveMessageOnQueue()
      .let { objectMapper.readValue<Notification>(it.body()) }
      .let { objectMapper.readValue<CsipDomainEvent>(it.message) }

  fun HmppsQueue.receiveDomainEventsOnQueue(maxMessages: Int = 10): List<Any> =
    sqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(maxMessages).build(),
    ).get().messages()
      .map { objectMapper.readValue<Notification>(it.body()) }
      .map {
        when (it.eventType) {
          CSIP_UPDATED.eventType, CSIP_CREATED.eventType, CSIP_DELETED.eventType ->
            objectMapper.readValue<CsipDomainEvent>(it.message)

          else -> objectMapper.readValue<CsipBasicDomainEvent>(it.message)
        }
      }

  internal fun verifyDomainEvents(
    prisonNumber: String,
    recordUuid: UUID,
    affectedComponents: Set<AffectedComponent>,
    eventTypes: Set<DomainEventType>,
    entityIds: Set<UUID> = setOf(),
    expectedCount: Int = 1,
    source: Source = DPS,
  ) {
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == expectedCount }
    val allEvents = hmppsEventsQueue.receiveDomainEventsOnQueue(expectedCount)
    eventTypes.forEach { eventType ->
      val events = when (eventType) {
        CSIP_DELETED, CSIP_UPDATED, CSIP_CREATED -> allEvents.filterIsInstance<CsipDomainEvent>()
        else -> {
          val basicEvents = allEvents.filterIsInstance<CsipBasicDomainEvent>()
          assertThat(basicEvents.map { it.additionalInformation.entityUuid })
            .containsExactlyInAnyOrderElementsOf(entityIds)
          basicEvents
        }
      }
      events.forEach { event ->
        with(event) {
          val domainEventType = requireNotNull(DomainEventType.entries.find { it.eventType == this.eventType })
          assertThat(domainEventType).isIn(eventTypes)
          with(additionalInformation as CsipBaseInformation) {
            if (this is CsipAdditionalInformation) {
              assertThat(this.affectedComponents).containsExactlyInAnyOrderElementsOf(affectedComponents)
            }
            assertThat(this.recordUuid).isEqualTo(recordUuid)
            assertThat(this.source).isEqualTo(source)
          }
          assertThat(description).isEqualTo(domainEventType.description)
          assertThat(detailUrl).isEqualTo("http://localhost:8080/csip-records/$recordUuid")
          assertThat(personReference).isEqualTo(PersonReference.withPrisonNumber(prisonNumber))
        }
      }
    }
  }

  internal fun verifyAudit(
    record: CsipRecord,
    action: AuditEventAction,
    affectedComponents: Set<AffectedComponent>,
    description: String,
    source: Source = DPS,
    username: String = TEST_USER,
    userDisplayName: String = TEST_USER_NAME,
  ) {
    val auditEvent: AuditEvent = auditEventRepository.findAll().single {
      it.csipRecordId == record.id && it.action == action
    }
    assertThat(auditEvent.description).isEqualTo(description)
    assertThat(auditEvent.source).isEqualTo(source)
    assertThat(auditEvent.actionedBy).isEqualTo(username)
    assertThat(auditEvent.actionedByCapturedName).isEqualTo(userDisplayName)
    assertThat(auditEvent.affectedComponents).containsExactlyInAnyOrderElementsOf(affectedComponents)
  }

  fun givenRandom(type: ReferenceDataType) =
    referenceDataRepository.findByDomain(type).filter { it.isActive() }.random()

  fun givenReferenceData(type: ReferenceDataType, code: String) =
    requireNotNull(referenceDataRepository.findByDomainAndCode(type, code))

  fun givenValidPrisonNumber(prisonNumber: String): String {
    prisonerSearch.stubGetPrisoner(prisonNumber)
    return prisonNumber
  }

  fun givenCsipRecord(csipRecord: CsipRecord): CsipRecord = csipRecordRepository.save(csipRecord)
  fun givenCsipRecordWithReferral(csipRecord: CsipRecord, complete: Boolean = false): CsipRecord {
    val record = csipRecord.withReferral(
      incidentType = { givenRandom(INCIDENT_TYPE) },
      incidentLocation = { givenRandom(INCIDENT_LOCATION) },
      refererAreaOfWork = { givenRandom(AREA_OF_WORK) },
      referralComplete = complete,
      referralCompletedBy = if (complete) "referralCompletedBy" else null,
      referralCompletedByDisplayName = if (complete) "referralCompletedByDisplayName" else null,
      referralCompletedDate = if (complete) LocalDate.now().minusDays(1) else null,
    )
    return csipRecordRepository.save(record)
  }

  fun CsipRecord.withPlan(
    caseManager: String = "Case Manager",
    reasonForPlan: String = "Reason for this plan",
    firstCaseReviewDate: LocalDate = LocalDate.now().plusWeeks(6),
  ) = apply {
    this.set(this::plan, Plan(this, caseManager, reasonForPlan, firstCaseReviewDate, id))
    csipRecordRepository.save(this)
  }

  fun Plan.withNeed(
    identifiedNeed: String = "An identified need",
    needIdentifiedBy: String = "I Dent",
    createdDate: LocalDate = LocalDate.now(),
    targetDate: LocalDate = LocalDate.now().plusWeeks(8),
    closedDate: LocalDate? = null,
    intervention: String = "intervention description",
    progression: String? = null,
  ) = apply {
    val need = IdentifiedNeed(
      this,
      identifiedNeed,
      needIdentifiedBy,
      createdDate,
      targetDate,
      closedDate,
      intervention,
      progression,
    )
    this.setByName("identifiedNeeds", identifiedNeeds() + need)
    csipRecordRepository.save(this.csipRecord)
  }

  fun Plan.withReview(
    reviewDate: LocalDate? = LocalDate.now(),
    recordedBy: String = "recordedBy",
    recordedByDisplayName: String = "recordedByDisplayName",
    nextReviewDate: LocalDate? = LocalDate.now().plusWeeks(4),
    csipClosedDate: LocalDate? = null,
    summary: String? = "A brief summary of the review",
    actions: Set<ReviewAction> = setOf(),
    attendees: Collection<CreateAttendeeRequest>? = null,
  ) = apply {
    val need = Review(
      this,
      (reviews().maxOfOrNull(Review::reviewSequence) ?: 0) + 1,
      reviewDate, recordedBy, recordedByDisplayName, nextReviewDate, csipClosedDate, summary, actions,
    )
    this.setByName("reviews", reviews() + need)
    csipRecordRepository.save(this.csipRecord)
  }

  fun Review.withAttendee(
    name: String? = "name",
    role: String? = "role",
    attended: Boolean? = true,
    contribution: String? = "a small contribution",
  ) = apply {
    val attendee = Attendee(this, name, role, attended, contribution)
    this.setByName("attendees", attendees() + attendee)
    csipRecordRepository.save(plan.csipRecord)
  }

  fun Referral.withDecisionAndActions(
    outcome: ReferenceData = givenRandom(OUTCOME_TYPE),
    signedOffBy: ReferenceData = givenRandom(DECISION_SIGNER_ROLE),
    conclusion: String? = "a comprehensive conclusion",
    recordedBy: String = "recordedBy",
    recordedByDisplayName: String? = "recordedByDisplayName",
    date: LocalDate = LocalDate.now(),
    nextSteps: String? = "some next steps",
    actions: Set<DecisionAction> = setOf(),
    actionOther: String? = null,
    id: Long = 0,
  ): Referral = DecisionAndActions(this, outcome, id)
    .upsert(
      UpsertDecisionAndActionsRequest(
        conclusion,
        outcome.code,
        signedOffBy.code,
        recordedBy,
        recordedByDisplayName,
        date,
        nextSteps, actionOther, actions,
      ),
      outcome,
      signedOffBy,
    ).let {
      this.set(::decisionAndActions, it)
      csipRecordRepository.save(csipRecord)
      this
    }

  fun Referral.withContributoryFactor(
    type: ReferenceData = givenRandom(CONTRIBUTORY_FACTOR_TYPE),
    comment: String? = "A comment about the factor",
    uuid: UUID = UUID.randomUUID(),
    id: Long = IdGenerator.newId(),
  ): Referral = ContributoryFactor(this, type, comment, uuid, id).let {
    this.setByName("contributoryFactors", contributoryFactors() + it)
    csipRecordRepository.save(csipRecord)
    this
  }

  fun Referral.withInvestigation(
    staffInvolved: String? = "staffInvolved",
    evidenceSecured: String? = "evidenceSecured",
    occurrenceReason: String? = "occurrenceReason",
    personsUsualBehaviour: String? = "personsUsualBehaviour",
    personsTrigger: String? = "personsTrigger",
    protectiveFactors: String? = "protectiveFactors",
  ): Referral = Investigation(
    this,
    id,
  ).upsert(
    UpsertInvestigationRequest(
      staffInvolved,
      evidenceSecured,
      occurrenceReason,
      personsUsualBehaviour,
      personsTrigger,
      protectiveFactors,
    ),
  ).let {
    this.set(::investigation, it)
    csipRecordRepository.save(csipRecord)
    this
  }

  fun Investigation.withInterview(
    interviewee: String = "interviewee",
    interviewDate: LocalDate = LocalDate.now(),
    intervieweeRole: ReferenceData = givenRandom(INTERVIEWEE_ROLE),
    interviewText: String? = "interviewText",
    interviewUuid: UUID = UUID.randomUUID(),
    id: Long = IdGenerator.newId(),
  ): Investigation =
    Interview(this, interviewee, interviewDate, intervieweeRole, interviewText, interviewUuid, id).let {
      this.setByName("interviews", interviews() + it)
      csipRecordRepository.save(referral.csipRecord)
      this
    }

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }

      System.setProperty("aws.region", "eu-west-2")

      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }

  @BeforeEach
  fun `clear queues`() {
    hmppsEventsQueue.sqsClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(hmppsEventsQueue.queueUrl).build()).get()
  }

  internal fun setAuthorisation(
    user: String? = null,
    client: String = CLIENT_ID,
    roles: List<String> = listOf(),
    isUserToken: Boolean = true,
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, client, roles, isUserToken = isUserToken)

  internal fun setCsipRequestContext(
    source: Source? = null,
    username: String? = TEST_USER,
  ): (HttpHeaders) -> Unit = {
    it.set(SOURCE, source?.name)
    it.set(USERNAME, username)
  }

  internal fun WebTestClient.ResponseSpec.errorResponse(status: HttpStatus) =
    expectStatus().isEqualTo(status)
      .expectBody<ErrorResponse>()
      .returnResult().responseBody!!

  internal final inline fun <reified T> WebTestClient.ResponseSpec.successResponse(status: HttpStatus = HttpStatus.OK): T =
    expectStatus().isEqualTo(status)
      .expectBody(T::class.java)
      .returnResult().responseBody!!
}
