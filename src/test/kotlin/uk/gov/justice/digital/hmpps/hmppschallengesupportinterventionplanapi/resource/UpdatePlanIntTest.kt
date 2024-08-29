package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertPlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.util.UUID

class UpdatePlanIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = updatePlanResponseSpec(UUID.randomUUID(), planRequest(), role = role)
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
  fun `400 bad request - username was not found`() {
    val recordUuid = UUID.randomUUID()
    val request = planRequest()

    val response = updatePlanResponseSpec(recordUuid, request, username = "UNKNOWN")
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
    val response = updatePlanResponseSpec(recordUuid, planRequest())
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
  fun `400 bad request - plan does not exist`() {
    val prisonNumber = givenValidPrisonNumber("P1234DS")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it }
    val request = planRequest()

    val response = updatePlanResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing a plan.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing a plan.")
      assertThat(moreInfo).isEqualTo(record.id)
    }
  }

  @Test
  fun `200 ok - no changes made to plan`() {
    val prisonNumber = givenValidPrisonNumber("P1234NC")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral().withPlan() }

    val request = planRequest()

    updatePlan(record.id, request, status = HttpStatus.OK)
    val plan = csipRecordRepository.getCsipRecord(record.id).plan
    requireNotNull(plan).verifyAgainst(request)

    verifyAudit(
      plan,
      RevisionType.ADD,
      setOf(CsipComponent.PLAN, CsipComponent.REFERRAL, CsipComponent.RECORD),
      nomisContext().copy(source = Source.DPS),
    )

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - update plan`() {
    val prisonNumber = givenValidPrisonNumber("P1234UP")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral().withPlan() }

    val request = planRequest(
      "A new case manager",
      "Some other reason",
    )

    updatePlan(record.id, request, status = HttpStatus.OK)

    val plan = csipRecordRepository.getCsipRecord(record.id).plan
    requireNotNull(plan).verifyAgainst(request)

    verifyAudit(
      plan,
      RevisionType.MOD,
      setOf(CsipComponent.PLAN),
    )

    verifyDomainEvents(
      prisonNumber,
      record.id,
      setOf(CsipComponent.PLAN),
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

  private fun updatePlanResponseSpec(
    recordUuid: UUID,
    request: UpsertPlanRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role))).exchange()

  private fun updatePlan(
    recordUuid: UUID,
    request: UpsertPlanRequest,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
    status: HttpStatus,
  ) = updatePlanResponseSpec(recordUuid, request, username, role)
    .successResponse<uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Plan>(status)
}
