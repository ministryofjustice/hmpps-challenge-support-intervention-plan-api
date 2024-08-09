package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Record
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.IDENTIFIED_NEED_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.util.UUID
import java.util.UUID.randomUUID

class AddIdentifiedNeedIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = addIdentifiedNeedResponseSpec(randomUUID(), createIdentifiedNeedRequest(), role = role)
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
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(randomUUID()))
      .exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `400 bad request - invalid source`() {
    val response = webTestClient.post().uri(urlToTest(randomUUID()))
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
    val response = addIdentifiedNeedResponseSpec(randomUUID(), createIdentifiedNeedRequest(), username = null)
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
    val response = addIdentifiedNeedResponseSpec(randomUUID(), createIdentifiedNeedRequest(), username = USER_NOT_FOUND)
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
  fun `404 not found - csip record not found`() {
    val uuid = randomUUID()
    val response = addIdentifiedNeedResponseSpec(uuid, createIdentifiedNeedRequest())
      .errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `409 conflict - identified need already present`() {
    val prisonNumber = givenValidPrisonNumber("N1234AA")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber)).withPlan()
    requireNotNull(record.plan).withNeed()

    val request = createIdentifiedNeedRequest()
    val response = addIdentifiedNeedResponseSpec(record.recordUuid, request).errorResponse(HttpStatus.CONFLICT)

    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: Identified need already part of plan")
      assertThat(developerMessage).isEqualTo("Identified need already part of plan")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - identified need added DPS`() {
    val prisonNumber = givenValidPrisonNumber("N1234DP")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber)).withPlan()

    val request = createIdentifiedNeedRequest()
    val response = addIdentifiedNeed(record.recordUuid, request)

    val need = getIdentifiedNeed(record.recordUuid, response.identifiedNeedUuid)
    need.verifyAgainst(request)

    verifyAudit(record, UPDATE, setOf(AffectedComponent.IdentifiedNeed, Plan, Record))

    verifyDomainEvents(
      prisonNumber,
      record.recordUuid,
      setOf(AffectedComponent.IdentifiedNeed),
      setOf(IDENTIFIED_NEED_CREATED),
      setOf(response.identifiedNeedUuid),
    )
  }

  @Test
  fun `201 created - identified need added NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("N1234NM")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber)).withPlan()

    val request = createIdentifiedNeedRequest()
    val response = addIdentifiedNeed(record.recordUuid, request, NOMIS, NOMIS_SYS_USER, ROLE_NOMIS)

    val need = getIdentifiedNeed(record.recordUuid, response.identifiedNeedUuid)
    need.verifyAgainst(request)

    verifyAudit(
      record,
      UPDATE,
      setOf(AffectedComponent.IdentifiedNeed, Plan, Record),
      nomisContext(),
    )

    verifyDomainEvents(
      prisonNumber,
      record.recordUuid,
      setOf(AffectedComponent.IdentifiedNeed),
      setOf(IDENTIFIED_NEED_CREATED),
      setOf(response.identifiedNeedUuid),
      source = NOMIS,
    )
  }

  private fun urlToTest(csipRecordUuid: UUID) = "/csip-records/$csipRecordUuid/plan/identified-needs"

  private fun addIdentifiedNeedResponseSpec(
    csipUuid: UUID,
    request: CreateIdentifiedNeedRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post()
    .uri(urlToTest(csipUuid))
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username))
    .exchange()

  private fun addIdentifiedNeed(
    csipUuid: UUID,
    request: CreateIdentifiedNeedRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.IdentifiedNeed =
    addIdentifiedNeedResponseSpec(csipUuid, request, source, username, role).successResponse(CREATED)

  private fun IdentifiedNeed.verifyAgainst(request: CreateIdentifiedNeedRequest) {
    assertThat(identifiedNeed).isEqualTo(request.identifiedNeed)
    assertThat(responsiblePerson).isEqualTo(request.responsiblePerson)
    assertThat(createdDate).isEqualTo(request.createdDate)
    assertThat(targetDate).isEqualTo(request.targetDate)
    assertThat(closedDate).isEqualTo(request.closedDate)
    assertThat(intervention).isEqualTo(request.intervention)
    assertThat(progression).isEqualTo(request.progression)
  }

  private fun getIdentifiedNeed(recordUuid: UUID, identifiedNeedUuid: UUID): IdentifiedNeed =
    transactionTemplate.execute {
      csipRecordRepository.getCsipRecord(recordUuid).plan!!.identifiedNeeds()
        .first { it.identifiedNeedUuid == identifiedNeedUuid }
    }!!
}
