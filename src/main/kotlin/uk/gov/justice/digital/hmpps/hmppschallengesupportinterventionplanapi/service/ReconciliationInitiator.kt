package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.retry.RetryPolicy
import org.springframework.retry.backoff.BackOffPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequest
import software.amazon.awssdk.services.sqs.model.SendMessageBatchRequestEntry
import software.amazon.awssdk.services.sqs.model.SendMessageBatchResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummaryRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.DomainEventsListener.Companion.PERSON_RECONCILIATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.MessageAttributes
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.Notification
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.events.domainevents.PersonReconciliationInformation
import uk.gov.justice.hmpps.sqs.DEFAULT_BACKOFF_POLICY
import uk.gov.justice.hmpps.sqs.DEFAULT_RETRY_POLICY
import uk.gov.justice.hmpps.sqs.HmppsQueue
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.streams.asSequence

@Transactional
@Service
class ReconciliationInitiator(
  private val psr: PersonSummaryRepository,
  private val om: ObjectMapper,
  private val queueService: HmppsQueueService,
) {
  private val eventQueue: HmppsQueue by lazy {
    queueService.findByQueueId("hmppsdomaineventsqueue") ?: throw IllegalStateException("Queue not available")
  }

  fun initiatePersonReconciliation() {
    psr.streamAll()
      .asSequence()
      .chunked(100)
      .map { it.toDomainEvent() }
      .chunked(10)
      .forEach { eventQueue.publishBatch(it) }
  }

  private fun List<PersonSummary>.toDomainEvent(): HmppsDomainEvent<PersonReconciliationInformation> = HmppsDomainEvent(
    occurredAt = ZonedDateTime.now(),
    eventType = PERSON_RECONCILIATION,
    detailUrl = null,
    description = "Internal message to launch person reconciliation",
    additionalInformation = PersonReconciliationInformation(map { it.prisonNumber }.toSet()),
    personReference = null,
  )

  private fun HmppsQueue.publishBatch(
    events: Collection<HmppsDomainEvent<*>>,
    retryPolicy: RetryPolicy = DEFAULT_RETRY_POLICY,
    backOffPolicy: BackOffPolicy = DEFAULT_BACKOFF_POLICY,
  ) {
    val retryTemplate =
      RetryTemplate().apply {
        setRetryPolicy(retryPolicy)
        setBackOffPolicy(backOffPolicy)
      }
    val publishRequest =
      SendMessageBatchRequest
        .builder()
        .queueUrl(queueUrl)
        .entries(
          events.map {
            val notification =
              Notification(om.writeValueAsString(it), attributes = MessageAttributes(it.eventType))
            SendMessageBatchRequestEntry
              .builder()
              .id(UUID.randomUUID().toString())
              .messageBody(om.writeValueAsString(notification))
              .build()
          },
        ).build()
    retryTemplate.execute<SendMessageBatchResponse, RuntimeException> {
      sqsClient.sendMessageBatch(publishRequest).get()
    }
  }
}
