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
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.IdentifiedNeedRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getIdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.util.UUID

class UpdateIdentifiedNeedIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var identifiedNeedRepository: IdentifiedNeedRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = updateIdentifiedNeedResponseSpec(
      UUID.randomUUID(),
      identifiedNeedRequest(),
      role = role,
    ).errorResponse(HttpStatus.FORBIDDEN)

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
    val response = updateIdentifiedNeedResponseSpec(
      UUID.randomUUID(),
      identifiedNeedRequest(),
      username = "UNKNOWN",
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: User details for supplied username not found")
      assertThat(developerMessage).isEqualTo("User details for supplied username not found")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `400 bad request - invalid request values`() {
    val record = dataSetup(generateCsipRecord()) { csip ->
      csip.withPlan().also { it.plan!!.withNeed() }
    }
    val identifiedNeedUuid = requireNotNull(record.plan?.identifiedNeeds()?.firstOrNull()).id

    val response = updateIdentifiedNeedResponseSpec(
      identifiedNeedUuid,
      UpdateIdentifiedNeedRequest(
        identifiedNeed = "n".repeat(1001),
        responsiblePerson = "n".repeat(101),
        createdDate = LocalDate.of(2024, 8, 1),
        targetDate = LocalDate.of(2047, 12, 25),
        closedDate = LocalDate.of(2024, 12, 31),
        intervention = "n".repeat(4001),
        progression = "n".repeat(4001),
      ),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo(
        """Validation failures: 
          |Identified Need must be <= 1000 characters
          |Intervention must be <= 4000 characters
          |Progression must be <= 4000 characters
          |Responsible person name must be <= 100 characters
          |
        """.trimMargin(),
      )
      assertThat(developerMessage).isEqualTo(
        """400 BAD_REQUEST Validation failures: 
          |Identified Need must be <= 1000 characters
          |Intervention must be <= 4000 characters
          |Progression must be <= 4000 characters
          |Responsible person name must be <= 100 characters
          |
        """.trimMargin(),
      )
    }
  }

  @Test
  fun `404 not found - Identified need not found`() {
    val identifiedNeedUuid = UUID.randomUUID()
    val response =
      updateIdentifiedNeedResponseSpec(identifiedNeedUuid, identifiedNeedRequest()).errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Identified Need not found")
      assertThat(developerMessage).isEqualTo("Identified Need not found with identifier $identifiedNeedUuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - no changes made to identified need`() {
    val request = identifiedNeedRequest()
    val record = dataSetup(generateCsipRecord()) {
      it.withPlan().plan!!.withNeed(
        identifiedNeed = request.identifiedNeed,
        responsiblePerson = request.responsiblePerson,
        createdDate = request.createdDate,
        targetDate = request.targetDate,
        closedDate = request.closedDate,
        intervention = request.intervention,
        progression = request.progression,
      )
      it
    }

    val identifiedNeedUuid = requireNotNull(record.plan?.identifiedNeeds()?.firstOrNull()).id

    val response = updateIdentifiedNeed(identifiedNeedUuid, request, status = HttpStatus.OK)
    response.verifyAgainst(request)
    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - update identified need`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withPlan().plan!!.withNeed(
        identifiedNeed = "oldIdentifiedNeed",
        responsiblePerson = "oldResponsiblePerson",
        createdDate = LocalDate.of(1999, 8, 1),
        targetDate = LocalDate.of(1999, 12, 25),
        closedDate = null,
        intervention = "intervention",
        progression = null,
      )
      it
    }

    val identifiedNeedUuid = requireNotNull(record.plan?.identifiedNeeds()?.firstOrNull()).id
    val request = identifiedNeedRequest()

    val response = updateIdentifiedNeed(identifiedNeedUuid, request, status = HttpStatus.OK)
    response.verifyAgainst(request)

    val identifiedNeed = getIdentifiedNeed(identifiedNeedUuid)
    verifyAudit(identifiedNeed, RevisionType.MOD, setOf(CsipComponent.IDENTIFIED_NEED))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  private fun getIdentifiedNeed(identifiedNeedUuid: UUID) =
    identifiedNeedRepository.getIdentifiedNeed(identifiedNeedUuid)

  private fun IdentifiedNeed.verifyAgainst(request: UpdateIdentifiedNeedRequest) {
    assertThat(identifiedNeed).isEqualTo(request.identifiedNeed)
    assertThat(responsiblePerson).isEqualTo(request.responsiblePerson)
    assertThat(createdDate).isEqualTo(request.createdDate)
    assertThat(targetDate).isEqualTo(request.targetDate)
    assertThat(closedDate).isEqualTo(request.closedDate)
    assertThat(intervention).isEqualTo(request.intervention)
    assertThat(progression).isEqualTo(request.progression)
  }

  private fun identifiedNeedRequest() = UpdateIdentifiedNeedRequest(
    identifiedNeed = "identifiedNeed",
    responsiblePerson = "responsiblePerson",
    createdDate = LocalDate.of(2024, 8, 1),
    targetDate = LocalDate.of(2047, 12, 25),
    closedDate = LocalDate.of(2024, 12, 31),
    intervention = "intervention",
    progression = "progression",
  )

  private fun urlToTest(identifiedNeedUuid: UUID) = "/csip-records/plan/identified-needs/$identifiedNeedUuid"

  private fun updateIdentifiedNeedResponseSpec(
    recordUuid: UUID,
    request: UpdateIdentifiedNeedRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role))).exchange()

  private fun updateIdentifiedNeed(
    recordUuid: UUID,
    request: UpdateIdentifiedNeedRequest,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
    status: HttpStatus,
  ) = updateIdentifiedNeedResponseSpec(recordUuid, request, username, role).successResponse<IdentifiedNeed>(status)
}
