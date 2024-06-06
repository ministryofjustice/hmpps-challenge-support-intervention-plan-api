package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SaferCustodyScreeningOutcomesIntTest(
  @Autowired private val csipRecordRepository: CsipRecordRepository,
) : IntegrationTestBase() {
  @Test
  fun `401 unauthorised`() {
    val recordUuid = UUID.randomUUID()
    webTestClient.get().uri("/csip-records/$recordUuid/referral/safer-custody-screening").exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `create safer custody screening outcome`() {
    val recordUuid = createCsipRecord().recordUuid

    val request = CreateSaferCustodyScreeningOutcomeRequest(
      outcomeTypeCode = "CUR",
      date = LocalDate.now(),
      reasonForDecision = "alia",
    )

    val response = webTestClient.post()
      .uri("/csip-records/$recordUuid/referral/safer-custody-screening")
      .bodyValue(request)
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI), user = TEST_USER, isUserToken = true))
      .headers(setCsipRequestContext())
      .exchange()
      .expectStatus().isCreated
      .expectHeader().contentType(MediaType.APPLICATION_JSON)
      .expectBody(SaferCustodyScreeningOutcome::class.java)
      .returnResult().responseBody!!

    with(response) {
      assertThat(reasonForDecision).isEqualTo("alia")
      assertThat(outcome.code).isEqualTo("CUR")
    }

    with(csipRecordRepository.findByRecordUuid(recordUuid)!!.auditEvents().single()) {
      assertThat(action).isEqualTo(AuditEventAction.UPDATED)
      assertThat(isSaferCustodyScreeningOutcomeAffected).isTrue()
    }

    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it!! > 0 }
    val event = hmppsEventsQueue.receiveCsipDomainEventOnQueue()
    assertThat(event).usingRecursiveComparison().isEqualTo(
      CsipDomainEvent(
        DomainEventType.CSIP_UPDATED.eventType,
        CsipAdditionalInformation(
          url = "http://localhost:8080/csip-records/$recordUuid",
          recordUuid = recordUuid,
          prisonNumber = "PRISON01",
          isRecordAffected = false,
          isReferralAffected = false,
          isContributoryFactorAffected = false,
          isSaferCustodyScreeningOutcomeAffected = true,
          isInvestigationAffected = false,
          isInterviewAffected = false,
          isDecisionAndActionsAffected = false,
          isPlanAffected = false,
          isIdentifiedNeedAffected = false,
          isReviewAffected = false,
          isAttendeeAffected = false,
          source = Source.DPS,
          reason = Reason.USER,
        ),
        1,
        DomainEventType.CSIP_UPDATED.description,
        event.occurredAt,
      ),
    )
  }

  private fun createCsipRecord() = csipRecordRepository.saveAndFlush(
    CsipRecord(
      recordUuid = UUID.randomUUID(),
      prisonNumber = "PRISON01",
      prisonCodeWhenRecorded = PRISON_CODE_LEEDS,
      logNumber = "LOG",
      createdAt = LocalDateTime.now(),
      createdBy = "te",
      createdByDisplayName = "Bobbie Shepard",
      lastModifiedAt = null,
      lastModifiedBy = null,
      lastModifiedByDisplayName = null,
    ),
  )
}
