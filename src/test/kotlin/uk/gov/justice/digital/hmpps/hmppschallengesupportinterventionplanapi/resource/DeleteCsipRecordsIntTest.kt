package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.AuditEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipAdditionalInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBaseInformation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipBasicDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Record
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.LocalDate
import java.util.UUID

class DeleteCsipRecordsIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @Test
  fun `401 unauthorised`() {
    webTestClient.delete().uri("/csip-records/${UUID.randomUUID()}").exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = deleteCsipRecordResponseSpec(UUID.randomUUID(), role = role)
      .errorResponse(HttpStatus.FORBIDDEN)

    with(response) {
      assertThat(status).isEqualTo(403)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Authentication problem. Check token and roles - Access Denied")
      assertThat(developerMessage).isEqualTo("Access Denied")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.delete().uri("/csip-records/${UUID.randomUUID()}")
      .headers(setAuthorisation(roles = listOf(ROLE_CSIP_UI))).headers { it.set(SOURCE, "INVALID") }
      .exchange().errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: No enum constant uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.INVALID")
      assertThat(developerMessage).isEqualTo("No enum constant uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.INVALID")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not supplied`() {
    val response = deleteCsipRecordResponseSpec(UUID.randomUUID(), username = null)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: Could not find non empty username from user_name or username token claims or Username header")
      assertThat(developerMessage).isEqualTo("Could not find non empty username from user_name or username token claims or Username header")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = deleteCsipRecordResponseSpec(UUID.randomUUID(), username = USER_NOT_FOUND)
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - csip record already deleted`() {
    val uuid = UUID.randomUUID()
    deleteCsipRecordResponseSpec(uuid).expectStatus().isOk
  }

  @Test
  fun `204 no content - CSIP record soft deleted by DPS`() {
    val prisonNumber = givenValidPrisonNumber("D1234DS")
    val record = transactionTemplate.execute {
      val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
      val referral = requireNotNull(record.referral)
        .withContributoryFactor()
        .withContributoryFactor()
        .withInvestigation()
      requireNotNull(referral.investigation)
        .withInterview(interviewDate = LocalDate.now().minusDays(2))
        .withInterview(interviewDate = LocalDate.now().minusDays(1))
        .withInterview()
      record
    }!!

    val factorUuids = record.referral!!.contributoryFactors().map { it.contributoryFactorUuid }.toSet()
    assertThat(factorUuids).hasSize(2)

    val interviewIds = record.referral!!.investigation!!.interviews().map { it.interviewUuid }.toSet()
    assertThat(interviewIds).hasSize(3)

    deleteCsipRecordResponseSpec(record.recordUuid).expectStatus().isNoContent

    verifyDelete(record, setOf(Record, Referral, ContributoryFactor, Investigation, Interview))

    verifyDomainEvents(
      prisonNumber,
      record.recordUuid,
      setOf(Record, Referral, ContributoryFactor, Investigation, Interview),
      entityIds = factorUuids + interviewIds,
      expectedCount = 6,
    )
  }

  @Test
  fun `204 no content - CSIP record soft deleted by NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("D1234NS")
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    deleteCsipRecordResponseSpec(record.recordUuid, NOMIS, NOMIS_SYS_USER, ROLE_NOMIS).expectStatus().isNoContent

    verifyDelete(
      record,
      setOf(Record, Referral),
      source = NOMIS,
      username = NOMIS_SYS_USER,
      userDisplayName = NOMIS_SYS_USER_DISPLAY_NAME,
    )

    verifyDomainEvents(prisonNumber, record.recordUuid, setOf(Record, Referral), NOMIS)
  }

  private fun deleteCsipRecordResponseSpec(
    uuid: UUID,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.delete()
    .uri("/csip-records/$uuid")
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username))
    .exchange()

  private fun verifyDelete(
    record: CsipRecord,
    affectedComponents: Set<AffectedComponent>,
    source: Source = DPS,
    username: String = TEST_USER,
    userDisplayName: String = TEST_USER_NAME,
  ) {
    val saved = csipRecordRepository.findByRecordUuid(record.recordUuid)
    assertThat(saved).isNull()
    val auditEvent: AuditEvent = auditEventRepository.findAll().single {
      it.csipRecordId == record.id && it.action == AuditEventAction.DELETED
    }
    assertThat(auditEvent.source).isEqualTo(source)
    assertThat(auditEvent.actionedBy).isEqualTo(username)
    assertThat(auditEvent.actionedByCapturedName).isEqualTo(userDisplayName)
    assertThat(auditEvent.affectedComponents).containsExactlyInAnyOrderElementsOf(affectedComponents)
  }

  private fun verifyDomainEvents(
    prisonNumber: String,
    recordUuid: UUID,
    affectedComponents: Set<AffectedComponent>,
    source: Source = DPS,
    eventTypes: List<DomainEventType> = listOf(DomainEventType.CSIP_DELETED),
    entityIds: Set<UUID> = setOf(),
    expectedCount: Int = 1,
  ) {
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == expectedCount }
    val allEvents = hmppsEventsQueue.receiveDomainEventsOnQueue(expectedCount)
    eventTypes.forEach { eventType ->
      val events = when (eventType) {
        DomainEventType.CSIP_DELETED -> allEvents.filterIsInstance<CsipDomainEvent>()
        else -> {
          val basicEvents = allEvents.filterIsInstance<CsipBasicDomainEvent>()
          assertThat(basicEvents.map { it.additionalInformation.entityUuid })
            .containsExactlyInAnyOrderElementsOf(entityIds)
          basicEvents
        }
      }
      events.forEach { event ->
        with(event) {
          assertThat(this.eventType).isEqualTo(eventType.eventType)
          with(additionalInformation as CsipBaseInformation) {
            if (this is CsipAdditionalInformation) {
              assertThat(this.affectedComponents).containsExactlyInAnyOrderElementsOf(affectedComponents)
            }
            assertThat(this.recordUuid).isEqualTo(recordUuid)
            assertThat(this.source).isEqualTo(source)
          }
          assertThat(description).isEqualTo(eventType.description)
          assertThat(detailUrl).isEqualTo("http://localhost:8080/csip-records/$recordUuid")
          assertThat(personReference).isEqualTo(PersonReference.withPrisonNumber(prisonNumber))
        }
      }
    }
  }
}
