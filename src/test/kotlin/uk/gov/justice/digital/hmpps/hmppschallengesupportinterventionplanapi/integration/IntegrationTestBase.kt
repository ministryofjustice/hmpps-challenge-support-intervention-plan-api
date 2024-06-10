package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.databind.annotation.JsonNaming
import com.fasterxml.jackson.module.kotlin.readValue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.USERNAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.AdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.ContributoryFactorDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.DomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.InterviewDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.ManageUsersExtension
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PrisonerSearchExtension
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countAllMessagesOnQueue

@ExtendWith(HmppsAuthApiExtension::class, ManageUsersExtension::class, PrisonerSearchExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
abstract class IntegrationTestBase {

  @Suppress("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  lateinit var webTestClient: WebTestClient

  @Autowired
  lateinit var jwtAuthHelper: JwtAuthHelper

  @Autowired
  lateinit var objectMapper: ObjectMapper

  @SpyBean
  lateinit var hmppsQueueService: HmppsQueueService

  internal val hmppsEventsQueue by lazy { hmppsQueueService.findByQueueId("hmppseventtestqueue") ?: throw MissingQueueException("hmppseventtestqueue queue not found") }

  internal val hmppsEventTopic by lazy { hmppsQueueService.findByTopicId("hmppseventtopic") ?: throw MissingQueueException("HmppsTopic hmpps event topic not found") }

  internal fun HmppsQueue.countAllMessagesOnQueue() =
    sqsClient.countAllMessagesOnQueue(queueUrl).get()

  internal fun HmppsQueue.receiveMessageOnQueue() =
    sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).build()).get().messages().single()

  internal fun HmppsQueue.receiveCsipDomainEventOnQueue() =
    receiveMessageOnQueue()
      .let { objectMapper.readValue<MsgBody>(it.body()) }
      .let { objectMapper.readValue<CsipDomainEvent>(it.Message) }

  internal fun HmppsQueue.receiveDomainEventsOnQueue(maxMessages: Int = 10): Collection<DomainEvent<AdditionalInformation>> =
    sqsClient.receiveMessage(ReceiveMessageRequest.builder().queueUrl(queueUrl).maxNumberOfMessages(maxMessages).build()).get().messages()
      .map { objectMapper.readValue<MsgBody>(it.body()) }
      .map {
        if (it.Message.contains("prisoner-csip.csip-record-")) {
          objectMapper.readValue<CsipDomainEvent>(it.Message)
        } else if (it.Message.contains("prisoner-csip.interview-")) {
          objectMapper.readValue<InterviewDomainEvent>(it.Message)
        } else if (it.Message.contains("prisoner-csip.contributory-factor-")) {
          objectMapper.readValue<ContributoryFactorDomainEvent>(it.Message)
        } else {
          throw Exception("unidentified Domain Event")
        }
      }

  @JsonNaming(value = PropertyNamingStrategies.UpperCamelCaseStrategy::class)
  private data class MsgBody(val Message: String)

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
}
