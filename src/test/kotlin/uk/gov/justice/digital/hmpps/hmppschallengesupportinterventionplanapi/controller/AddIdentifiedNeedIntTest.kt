package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.IdentifiedNeedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.getIdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createIdentifiedNeedRequest
import java.util.UUID
import java.util.UUID.randomUUID

class AddIdentifiedNeedIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var identifiedNeedRepository: IdentifiedNeedRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(randomUUID())).exchange().expectStatus().isUnauthorized
  }

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
  fun `201 created - identified need added DPS`() {
    val record = dataSetup(generateCsipRecord()) { it.withPlan() }

    val request = createIdentifiedNeedRequest()
    val response = addIdentifiedNeed(record.id, request)

    val need = getIdentifiedNeed(response.identifiedNeedUuid)
    need.verifyAgainst(request)
    verifyAudit(need, RevisionType.ADD, setOf(CsipComponent.IDENTIFIED_NEED))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  private fun urlToTest(csipRecordUuid: UUID) = "/csip-records/$csipRecordUuid/plan/identified-needs"

  private fun addIdentifiedNeedResponseSpec(
    csipUuid: UUID,
    request: CreateIdentifiedNeedRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post()
    .uri(urlToTest(csipUuid))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun addIdentifiedNeed(
    csipUuid: UUID,
    request: CreateIdentifiedNeedRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.IdentifiedNeed = addIdentifiedNeedResponseSpec(csipUuid, request, username, role).successResponse(CREATED)

  private fun IdentifiedNeed.verifyAgainst(request: CreateIdentifiedNeedRequest) {
    assertThat(identifiedNeed).isEqualTo(request.identifiedNeed)
    assertThat(responsiblePerson).isEqualTo(request.responsiblePerson)
    assertThat(createdDate).isEqualTo(request.createdDate)
    assertThat(targetDate).isEqualTo(request.targetDate)
    assertThat(closedDate).isEqualTo(request.closedDate)
    assertThat(intervention).isEqualTo(request.intervention)
    assertThat(progression).isEqualTo(request.progression)
  }

  private fun getIdentifiedNeed(uuid: UUID): IdentifiedNeed = identifiedNeedRepository.getIdentifiedNeed(uuid)
}
