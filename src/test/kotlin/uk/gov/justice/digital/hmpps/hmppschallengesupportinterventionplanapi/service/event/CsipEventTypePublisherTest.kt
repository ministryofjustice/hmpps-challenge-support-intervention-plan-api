package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.event

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.toZoneDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.DomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.HmppsDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.CompletableFuture.completedFuture

class CsipEventTypePublisherTest {
  private val hmppsQueueService = mock<HmppsQueueService>()
  private val domainEventsTopic = mock<HmppsTopic>()
  private val domainEventsSnsClient = mock<SnsAsyncClient>()
  private val publisherResponse = mock<PublishResponse>()
  private val objectMapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()

  private val domainEventsTopicArn = "arn:aws:sns:eu-west-2:000000000000:${UUID.randomUUID()}"
  private val baseUrl = "http://localhost:8080"

  @Test
  fun `throws IllegalStateException when topic not found`() {
    whenever(hmppsQueueService.findByTopicId("hmppseventtopic")).thenReturn(null)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper)
    val exception = assertThrows<IllegalStateException> { domainEventPublisher.publish(mock<DomainEvent>()) }
    assertThat(exception.message).isEqualTo("hmppseventtopic not found")
  }

  @Test
  fun `publish alert event`() {
    whenever(hmppsQueueService.findByTopicId("hmppseventtopic")).thenReturn(domainEventsTopic)
    whenever(domainEventsTopic.snsClient).thenReturn(domainEventsSnsClient)
    whenever(domainEventsTopic.arn).thenReturn(domainEventsTopicArn)
    whenever(domainEventsSnsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(publisherResponse))

    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper)
    val recordUuid = UUID.randomUUID()
    val occurredAt = LocalDateTime.now()
    val domainEvent = HmppsDomainEvent(
      eventType = CSIP_CREATED.eventType,
      additionalInformation = CsipInformation(
        recordUuid = recordUuid,
        source = NOMIS,
        affectedComponents = setOf(CsipComponent.RECORD),
      ),
      description = CSIP_CREATED.description,
      occurredAt = occurredAt.toZoneDateTime(),
      detailUrl = "$baseUrl/csip-records/$recordUuid",
      personReference = PersonReference.withPrisonNumber(PRISON_NUMBER),
    )

    domainEventPublisher.publish(domainEvent)

    verify(domainEventsSnsClient).publish(
      PublishRequest.builder()
        .topicArn(domainEventsTopic.arn)
        .message(objectMapper.writeValueAsString(domainEvent))
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String").stringValue(domainEvent.eventType)
              .build(),
          ),
        )
        .build(),
    )
  }

  @Test
  fun `publish alert event - failure`() {
    whenever(hmppsQueueService.findByTopicId("hmppseventtopic")).thenReturn(domainEventsTopic)
    whenever(domainEventsTopic.snsClient).thenReturn(domainEventsSnsClient)
    whenever(domainEventsTopic.arn).thenReturn(domainEventsTopicArn)
    val domainEventPublisher = DomainEventPublisher(hmppsQueueService, objectMapper)
    val domainEvent = mock<HmppsDomainEvent<CsipInformation>>()
    whenever(domainEvent.eventType).thenReturn("some.event.type")

    val exceptionMessage = "Failed to publish domain event using library"
    whenever(domainEventsSnsClient.publish(any<PublishRequest>())).thenThrow(RuntimeException(exceptionMessage))

    val ex = assertThrows<RuntimeException> { domainEventPublisher.publish(domainEvent) }
    assertThat(ex.message).isEqualTo(exceptionMessage)
  }
}
