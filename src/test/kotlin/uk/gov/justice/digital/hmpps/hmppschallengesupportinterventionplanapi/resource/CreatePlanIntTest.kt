package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreatePlanRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createIdentifiedNeedRequest
import java.time.LocalDate
import java.util.UUID

class CreatePlanIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE", ROLE_NOMIS])
  fun `403 forbidden - no required role`(role: String?) {
    val response = createPlanResponseSpec(UUID.randomUUID(), createPlanRequest(), role = role)
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
      .bodyValue(createPlanRequest()).headers(setAuthorisation()).headers { it.set(SOURCE, "INVALID") }
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
    val record = givenCsipRecord(generateCsipRecord(PRISON_NUMBER)).withReferral()
    val request = createPlanRequest()

    val response = createPlanResponseSpec(record.id, request, username = null)
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
    val request = createPlanRequest()

    val response = createPlanResponseSpec(recordUuid, request, username = "UNKNOWN")
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
    val response = createPlanResponseSpec(recordUuid, createPlanRequest())
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
  fun `409 conflict - Plan already exists`() {
    val prisonNumber = givenValidPrisonNumber("P1234AE")
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withCompletedReferral().withPlan()
    }

    val response = createPlanResponseSpec(record.id, createPlanRequest())
      .errorResponse(HttpStatus.CONFLICT)

    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: CSIP record already has a plan")
      assertThat(developerMessage).isEqualTo("CSIP record already has a plan")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - create plan via DPS UI`() {
    val prisonNumber = givenValidPrisonNumber("P1234DS")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it }
    val recordUuid = record.id
    val request = createPlanRequest()

    createPlan(recordUuid, request)

    val plan = getPlan(record.id)
    plan.verifyAgainst(request)

    verifyAudit(plan, RevisionType.ADD, setOf(CsipComponent.PLAN))
    verifyDomainEvents(
      record.prisonNumber,
      recordUuid,
      setOf(CsipComponent.PLAN),
      setOf(DomainEventType.CSIP_UPDATED),
    )
  }

  @Test
  fun `201 created - create plan with identified needs via DPS UI`() {
    val prisonNumber = givenValidPrisonNumber("P1234WN")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it }

    val request = createPlanRequest(identifiedNeeds = listOf(createIdentifiedNeedRequest()))
    val response = createPlan(record.id, request)

    val needsIds = response.identifiedNeeds.map { it.identifiedNeedUuid }

    val plan = getPlan(record.id)
    plan.verifyAgainst(request)

    verifyAudit(
      plan,
      RevisionType.ADD,
      setOf(CsipComponent.PLAN, CsipComponent.IDENTIFIED_NEED),
    )

    verifyDomainEvents(
      record.prisonNumber,
      record.id,
      setOf(CsipComponent.PLAN, CsipComponent.IDENTIFIED_NEED),
      setOf(DomainEventType.CSIP_UPDATED, DomainEventType.IDENTIFIED_NEED_CREATED),
      needsIds.toSet(),
      2,
    )
  }

  private fun Plan.verifyAgainst(request: CreatePlanRequest) {
    assertThat(caseManager).isEqualTo(request.caseManager)
    assertThat(reasonForPlan).isEqualTo(request.reasonForPlan)
    assertThat(firstCaseReviewDate).isEqualTo(request.firstCaseReviewDate)
    assertThat(identifiedNeeds().size).isEqualTo(request.identifiedNeeds.size)
  }

  private fun createPlanRequest(
    caseManager: String = "Case Manager",
    reasonForPlan: String = "Reason for this plan",
    firstCaseReviewDate: LocalDate = LocalDate.now().plusWeeks(6),
    identifiedNeeds: List<CreateIdentifiedNeedRequest> = listOf(),
  ) = CreatePlanRequest(caseManager, reasonForPlan, firstCaseReviewDate, identifiedNeeds)

  private fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/plan"

  private fun createPlanResponseSpec(
    recordUuid: UUID,
    request: CreatePlanRequest,
    source: Source = Source.DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username)).exchange()

  private fun createPlan(
    recordUuid: UUID,
    request: CreatePlanRequest,
    source: Source = Source.DPS,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
  ) = createPlanResponseSpec(recordUuid, request, source, username, role)
    .successResponse<uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Plan>(HttpStatus.CREATED)

  private fun getPlan(recordUuid: UUID) = transactionTemplate.execute {
    val plan = csipRecordRepository.findById(recordUuid)!!.plan
    plan!!.identifiedNeeds()
    plan
  }!!
}