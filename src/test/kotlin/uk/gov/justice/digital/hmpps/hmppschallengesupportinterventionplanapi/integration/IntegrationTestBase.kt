package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.hibernate.envers.AuditReaderFactory
import org.hibernate.envers.RevisionType
import org.hibernate.envers.query.AuditEntity
import org.junit.jupiter.api.Assertions.assertTrue
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
import org.springframework.transaction.support.TransactionTemplate
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.EventProperties
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Auditable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.audit.AuditRevision
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBaseInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipChildInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.Notification
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_DELETED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.DECISION_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.DECISION_SIGNER_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INTERVIEWEE_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.SCREENING_OUTCOME_TYPE
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.saveAndRefresh
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.event.EntityEventService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.getByName
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.set
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.setByName
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.testUserContext
import uk.gov.justice.hmpps.kotlin.common.ErrorResponse
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue
import java.time.LocalDate
import java.time.LocalTime
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
  lateinit var entityEventService: EntityEventService

  @Autowired
  lateinit var csipRecordRepository: CsipRecordRepository

  @Autowired
  lateinit var referenceDataRepository: ReferenceDataRepository

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @Autowired
  lateinit var entityManager: EntityManager

  internal val hmppsEventsQueue by lazy {
    hmppsQueueService.findByQueueId("hmppseventtestqueue")
      ?: throw MissingQueueException("hmppseventtestqueue queue not found")
  }

  internal fun HmppsQueue.countAllMessagesOnQueue() =
    sqsClient.countAllMessagesOnQueue(queueUrl).get()

  fun HmppsQueue.receiveDomainEventsOnQueue(maxMessages: Int = 10): List<HmppsDomainEvent<*>> =
    sqsClient.receiveMessage(
      ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(maxMessages).build(),
    ).get().messages()
      .map { objectMapper.readValue<Notification>(it.body()) }
      .map {
        when (it.eventType) {
          CSIP_UPDATED.eventType, CSIP_CREATED.eventType, CSIP_DELETED.eventType ->
            objectMapper.readValue<HmppsDomainEvent<CsipInformation>>(it.message)

          else -> objectMapper.readValue<HmppsDomainEvent<CsipChildInformation>>(it.message)
        }
      }

  fun switchEventPublish(publish: Boolean) {
    val current = entityEventService.getByName<EventProperties>("eventProperties")
    entityEventService.setByName("eventProperties", current.copy(publish = publish))
  }

  fun <T> dataSetup(csipRecord: CsipRecord, code: (CsipRecord) -> T): T {
    switchEventPublish(false)
    val t = transactionTemplate.execute {
      val res = code(csipRecord)
      csipRecordRepository.saveAndRefresh(csipRecord)
      res
    }!!
    switchEventPublish(true)
    return t
  }

  internal fun verifyDomainEvents(
    prisonNumber: String,
    recordUuid: UUID,
    affectedComponents: Set<CsipComponent>,
    eventTypes: Set<DomainEventType>,
    entityIds: Set<UUID> = setOf(),
    expectedCount: Int = 1,
    source: Source = DPS,
  ) {
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == expectedCount }
    val allEvents = hmppsEventsQueue.receiveDomainEventsOnQueue(expectedCount)
    val basicEvents = allEvents.map { it.additionalInformation }.filterIsInstance<CsipChildInformation>()
    assertThat(basicEvents.map { it.entityUuid }).containsExactlyInAnyOrderElementsOf(entityIds)
    allEvents.forEach { event ->
      with(event) {
        val domainEventType = requireNotNull(DomainEventType.entries.find { it.eventType == event.eventType })
        assertThat(domainEventType).isIn(eventTypes)
        with(additionalInformation as CsipBaseInformation) {
          if (this is CsipInformation) {
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

  internal fun verifyAudit(
    entity: Any,
    revisionType: RevisionType,
    affectedComponents: Set<CsipComponent>,
    context: CsipRequestContext = testUserContext(),
    validateEntityWithContext: Boolean = true,
  ) = transactionTemplate.execute {
    val auditReader = AuditReaderFactory.get(entityManager)
    assertTrue(auditReader.isEntityClassAudited(entity::class.java))

    val revisionNumber = auditReader.getRevisions(entity::class.java, entity.getByName("id"))
      .filterIsInstance<Long>().max()

    val entityRevision: Array<*> = auditReader.createQuery()
      .forRevisionsOfEntity(entity::class.java, false, true)
      .add(AuditEntity.revisionNumber().eq(revisionNumber))
      .resultList.first() as Array<*>
    assertThat(entityRevision[2]).isEqualTo(revisionType)

    val auditRevision = entityRevision[1] as AuditRevision
    with(auditRevision) {
      assertThat(source).isEqualTo(context.source)
      assertThat(username).isEqualTo(context.username)
      assertThat(userDisplayName).isEqualTo(context.userDisplayName)
      assertThat(caseloadId).isEqualTo(context.activeCaseLoadId)
      assertThat(this.affectedComponents).containsExactlyInAnyOrderElementsOf(affectedComponents)
    }

    if (validateEntityWithContext) {
      val audited = entityRevision[0] as Auditable
      with(audited) {
        if (revisionType == RevisionType.ADD) {
          assertThat(createdBy).isEqualTo(context.username)
          assertThat(createdByDisplayName).isEqualTo(context.userDisplayName)
        }
        if (revisionType == RevisionType.MOD) {
          assertThat(lastModifiedBy).isEqualTo(context.username)
          assertThat(lastModifiedByDisplayName).isEqualTo(context.userDisplayName)
        }
      }
    }
  }

  fun givenRandom(type: ReferenceDataType) =
    referenceDataRepository.findByKeyDomain(type).filter { it.isActive() }.random()

  fun givenReferenceData(type: ReferenceDataType, code: String) =
    requireNotNull(referenceDataRepository.findByKey(ReferenceDataKey(type, code)))

  fun givenValidPrisonNumber(prisonNumber: String): String {
    prisonerSearch.stubGetPrisoner(prisonNumber)
    return prisonNumber
  }

  fun givenCsipRecord(csipRecord: CsipRecord): CsipRecord = csipRecordRepository.save(csipRecord)

  fun CsipRecord.withCompletedReferral(
    referralComplete: Boolean = true,
    referralCompletedBy: String = "referralCompletedBy",
    referralCompletedByDisplayName: String = "referralCompletedByDisplayName",
    referralCompletedDate: LocalDate = LocalDate.now().minusDays(1),
  ) = withReferral(
    referralComplete = referralComplete,
    referralCompletedBy = referralCompletedBy,
    referralCompletedByDisplayName = referralCompletedByDisplayName,
    referralCompletedDate = referralCompletedDate,
  )

  fun CsipRecord.withReferral(
    incidentType: () -> ReferenceData = { givenRandom(INCIDENT_TYPE) },
    incidentLocation: () -> ReferenceData = { givenRandom(INCIDENT_LOCATION) },
    refererAreaOfWork: () -> ReferenceData = { givenRandom(AREA_OF_WORK) },
    incidentInvolvement: () -> ReferenceData? = { null },
    incidentDate: LocalDate = LocalDate.now(),
    incidentTime: LocalTime? = null,
    referredBy: String = "referredBy",
    referralDate: LocalDate = LocalDate.now(),
    proactiveReferral: Boolean? = null,
    staffAssaulted: Boolean? = null,
    assaultedStaffName: String? = null,
    descriptionOfConcern: String? = "descriptionOfConcern",
    knownReasons: String? = "knownReasons",
    otherInformation: String? = "otherInformation",
    saferCustodyTeamInformed: OptionalYesNoAnswer = DO_NOT_KNOW,
    referralComplete: Boolean? = null,
    referralCompletedBy: String? = null,
    referralCompletedByDisplayName: String? = null,
    referralCompletedDate: LocalDate? = null,
  ): CsipRecord = apply {
    set(
      ::referral,
      Referral(
        this,
        referralDate,
        incidentDate,
        incidentTime,
        referredBy,
        proactiveReferral,
        staffAssaulted,
        assaultedStaffName,
        descriptionOfConcern,
        knownReasons,
        otherInformation,
        saferCustodyTeamInformed,
        referralComplete,
        referralCompletedBy,
        referralCompletedByDisplayName,
        referralCompletedDate,
        incidentType(),
        incidentLocation(),
        refererAreaOfWork(),
        incidentInvolvement(),
      ),
    )
  }

  fun CsipRecord.withPlan(
    caseManager: String = "Case Manager",
    reasonForPlan: String = "Reason for this plan",
    firstCaseReviewDate: LocalDate = LocalDate.now().plusWeeks(6),
  ) = apply {
    set(::plan, Plan(this, caseManager, reasonForPlan, firstCaseReviewDate))
  }

  fun Plan.withNeed(
    identifiedNeed: String = "An identified need",
    responsiblePerson: String = "I Dent",
    createdDate: LocalDate = LocalDate.now(),
    targetDate: LocalDate = LocalDate.now().plusWeeks(8),
    closedDate: LocalDate? = null,
    intervention: String = "intervention description",
    progression: String? = null,
    legacyId: Long? = null,
  ) = apply {
    val need = IdentifiedNeed(
      this,
      identifiedNeed,
      responsiblePerson,
      createdDate,
      targetDate,
      closedDate,
      intervention,
      progression,
      legacyId,
    )
    getByName<MutableList<IdentifiedNeed>>("identifiedNeeds") += need
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
    legacyId: Long? = null,
  ) = apply {
    val review = Review(
      this,
      (reviews().maxOfOrNull(Review::reviewSequence) ?: 0) + 1,
      reviewDate, recordedBy, recordedByDisplayName, nextReviewDate, csipClosedDate, summary, actions, legacyId,
    )
    getByName<MutableList<Review>>("reviews") += review
  }

  fun Review.withAttendee(
    name: String? = "name",
    role: String? = "role",
    attended: Boolean? = true,
    contribution: String? = "a small contribution",
    legacyId: Long? = null,
  ) = apply {
    val attendee = Attendee(this, name, role, attended, contribution, legacyId)
    getByName<MutableList<Attendee>>("attendees") += attendee
  }

  fun Referral.withSaferCustodyScreeningOutcome(
    outcome: ReferenceData = givenRandom(SCREENING_OUTCOME_TYPE),
    recordedBy: String = "recordedBy",
    recordedByDisplayName: String = "recordedByDisplayName",
    date: LocalDate = LocalDate.now(),
    reasonForDecision: String = "A reason for the decision",
  ) = apply {
    this.set(
      ::saferCustodyScreeningOutcome,
      SaferCustodyScreeningOutcome(this, outcome, date, recordedBy, recordedByDisplayName, reasonForDecision),
    )
  }

  fun Referral.withDecisionAndActions(
    outcome: ReferenceData = givenRandom(DECISION_OUTCOME_TYPE),
    signedOffBy: ReferenceData = givenRandom(DECISION_SIGNER_ROLE),
    conclusion: String? = "a comprehensive conclusion",
    recordedBy: String = "recordedBy",
    recordedByDisplayName: String? = "recordedByDisplayName",
    date: LocalDate = LocalDate.now(),
    nextSteps: String? = "some next steps",
    actions: Set<DecisionAction> = setOf(),
    actionOther: String? = null,
  ): Referral = apply {
    val decision = DecisionAndActions(this, outcome, signedOffBy)
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
      )
    set(::decisionAndActions, decision)
  }

  fun Referral.withContributoryFactor(
    type: ReferenceData = givenRandom(CONTRIBUTORY_FACTOR_TYPE),
    comment: String? = "A comment about the factor",
    legacyId: Long? = null,
  ): Referral = apply {
    val factor = ContributoryFactor(this, type, comment, legacyId)
    getByName<MutableList<ContributoryFactor>>("contributoryFactors") += factor
  }

  fun Referral.withInvestigation(
    staffInvolved: String? = "staffInvolved",
    evidenceSecured: String? = "evidenceSecured",
    occurrenceReason: String? = "occurrenceReason",
    personsUsualBehaviour: String? = "personsUsualBehaviour",
    personsTrigger: String? = "personsTrigger",
    protectiveFactors: String? = "protectiveFactors",
  ): Referral = apply {
    val investigation = Investigation(this).update(
      UpsertInvestigationRequest(
        staffInvolved,
        evidenceSecured,
        occurrenceReason,
        personsUsualBehaviour,
        personsTrigger,
        protectiveFactors,
      ),
    )
    set(::investigation, investigation)
  }

  fun Investigation.withInterview(
    interviewee: String = "interviewee",
    interviewDate: LocalDate = LocalDate.now(),
    intervieweeRole: ReferenceData = givenRandom(INTERVIEWEE_ROLE),
    interviewText: String? = "interviewText",
    legacyId: Long? = null,
  ): Investigation = apply {
    val interview = Interview(this, interviewee, interviewDate, intervieweeRole, interviewText, legacyId)
    getByName<MutableList<Interview>>("interviews") += interview
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
    user: String? = TEST_USER,
    client: String = CLIENT_ID,
    roles: List<String> = listOf(ROLE_CSIP_UI),
    isUserToken: Boolean = false,
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisation(user, client, roles, isUserToken = isUserToken)

  internal fun WebTestClient.ResponseSpec.errorResponse(status: HttpStatus) =
    expectStatus().isEqualTo(status)
      .expectBody<ErrorResponse>()
      .returnResult().responseBody!!

  internal final inline fun <reified T> WebTestClient.ResponseSpec.successResponse(status: HttpStatus = HttpStatus.OK): T =
    expectStatus().isEqualTo(status)
      .expectBody(T::class.java)
      .returnResult().responseBody!!
}
