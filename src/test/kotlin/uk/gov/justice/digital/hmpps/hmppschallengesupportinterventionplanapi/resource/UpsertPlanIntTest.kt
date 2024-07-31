package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertPlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.util.UUID

class UpsertPlanIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = upsertPlanResponseSpec(UUID.randomUUID(), planRequest(), role = role)
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
    val response = webTestClient.put().uri(urlToTest(UUID.randomUUID()))
      .bodyValue(planRequest()).headers(setAuthorisation()).headers { it.set(SOURCE, "INVALID") }
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
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(PRISON_NUMBER))
    val recordUuid = csipRecord.recordUuid
    val request = planRequest()

    val response = upsertPlanResponseSpec(recordUuid, request, username = null)
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
  fun `400 bad request - username was not found`() {
    val recordUuid = UUID.randomUUID()
    val request = planRequest()

    val response = upsertPlanResponseSpec(recordUuid, request, username = "UNKNOWN")
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
  fun `404 not found - CSIP record not found`() {
    val recordUuid = UUID.randomUUID()
    val response = upsertPlanResponseSpec(recordUuid, planRequest())
      .errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $recordUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - create investigation via DPS UI`() {
    val prisonNumber = givenValidPrisonNumber("P1234DS")
    val csipRecord = givenCsipRecord(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = planRequest()

    upsertPlan(recordUuid, request, status = HttpStatus.CREATED)

    val plan = csipRecordRepository.getCsipRecord(csipRecord.recordUuid).plan
    requireNotNull(plan).verifyAgainst(request)

    val affectedComponents = setOf(AffectedComponent.Plan)
    verifyAudit(csipRecord, AuditEventAction.UPDATED, affectedComponents, "Plan added to CSIP record")
    verifyDomainEvents(
      prisonNumber,
      recordUuid,
      affectedComponents,
      setOf(DomainEventType.CSIP_UPDATED),
    )
  }

  @Test
  fun `201 created - create investigation via NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("P1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))

    val request = planRequest()

    upsertPlan(
      csipRecord.recordUuid,
      request,
      source = Source.NOMIS,
      username = NOMIS_SYS_USER,
      status = HttpStatus.CREATED,
    )

    val plan = csipRecordRepository.getCsipRecord(csipRecord.recordUuid).plan
    requireNotNull(plan).verifyAgainst(request)

    val affectedComponents = setOf(AffectedComponent.Plan)

    verifyAudit(
      csipRecord,
      AuditEventAction.UPDATED,
      affectedComponents,
      "Plan added to CSIP record",
      Source.NOMIS,
      NOMIS_SYS_USER,
      NOMIS_SYS_USER_DISPLAY_NAME,
    )

    verifyDomainEvents(
      prisonNumber,
      csipRecord.recordUuid,
      affectedComponents,
      setOf(DomainEventType.CSIP_UPDATED),
      source = Source.NOMIS,
    )
  }

  @Test
  fun `200 ok - no changes made to investigation`() {
    val prisonNumber = givenValidPrisonNumber("P1234NC")
    val csipRecord = transactionTemplate.execute {
      givenCsipRecordWithReferral(generateCsipRecord(prisonNumber)).withPlan()
    }!!

    val request = planRequest()

    upsertPlan(csipRecord.recordUuid, request, status = HttpStatus.OK)
    val plan = csipRecordRepository.getCsipRecord(csipRecord.recordUuid).plan
    requireNotNull(plan).verifyAgainst(request)
    assertFalse(auditEventRepository.findAll().any { it.csipRecordId == csipRecord.id })
    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - update investigation`() {
    val prisonNumber = givenValidPrisonNumber("P1234UP")
    val csipRecord = transactionTemplate.execute {
      givenCsipRecordWithReferral(generateCsipRecord(prisonNumber)).withPlan()
    }!!

    val request = planRequest(
      "A new case manager",
      "Some other reason",
    )

    upsertPlan(csipRecord.recordUuid, request, status = HttpStatus.OK)

    val plan = csipRecordRepository.getCsipRecord(csipRecord.recordUuid).plan
    requireNotNull(plan).verifyAgainst(request)

    val affectedComponents = setOf(AffectedComponent.Plan)
    verifyAudit(
      csipRecord,
      AuditEventAction.UPDATED,
      affectedComponents,
      "Updated plan caseManager changed from 'Case Manager' to 'A new case manager', " +
        "reasonForPlan changed from 'Reason for this plan' to 'Some other reason'",
    )

    verifyDomainEvents(
      prisonNumber,
      csipRecord.recordUuid,
      affectedComponents,
      setOf(DomainEventType.CSIP_UPDATED),
    )
  }

  private fun Plan.verifyAgainst(request: UpsertPlanRequest) {
    assertThat(caseManager).isEqualTo(request.caseManager)
    assertThat(reasonForPlan).isEqualTo(request.reasonForPlan)
    assertThat(firstCaseReviewDate).isEqualTo(request.firstCaseReviewDate)
  }

  private fun planRequest(
    caseManager: String = "Case Manager",
    reasonForPlan: String = "Reason for this plan",
    firstCaseReviewDate: LocalDate = LocalDate.now().plusWeeks(6),
  ) = UpsertPlanRequest(caseManager, reasonForPlan, firstCaseReviewDate)

  private fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/plan"

  private fun upsertPlanResponseSpec(
    recordUuid: UUID,
    request: UpsertPlanRequest,
    source: Source = Source.DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.put().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username)).exchange()

  private fun upsertPlan(
    recordUuid: UUID,
    request: UpsertPlanRequest,
    source: Source = Source.DPS,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
    status: HttpStatus,
  ) = upsertPlanResponseSpec(recordUuid, request, source, username, role)
    .successResponse<uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Plan>(status)
}
