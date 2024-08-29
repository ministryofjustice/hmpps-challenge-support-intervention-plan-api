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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.Duration.ofSeconds
import java.util.UUID

class UpdateInvestigationIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = updateInvestigationResponseSpec(UUID.randomUUID(), investigationRequest(), role = role)
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
    val request = investigationRequest()

    val response = updateInvestigationResponseSpec(recordUuid, request, username = "UNKNOWN")
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
  fun `400 bad request - At least one field should be completed`() {
    val prisonNumber = givenValidPrisonNumber("I2234NF")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber).withCompletedReferral())

    val response = updateInvestigationResponseSpec(
      record.id,
      UpsertInvestigationRequest(null, null, null, null, null, null),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: At least one of staffInvolved, evidenceSecured, occurrenceReason, personsUsualBehaviour, personsTrigger, protectiveFactors must be non null.")
    }
  }

  @Test
  fun `400 bad request - CSIP record missing a referral`() {
    val prisonNumber = givenValidPrisonNumber("I2234MR")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber))

    val response = updateInvestigationResponseSpec(record.id, investigationRequest())
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing a referral.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing a referral.")
      assertThat(moreInfo).isEqualTo(record.id.toString())
    }
  }

  @Test
  fun `404 not found - CSIP record not found`() {
    val recordUuid = UUID.randomUUID()
    val response = updateInvestigationResponseSpec(recordUuid, investigationRequest())
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
  fun `400 bad request - investigation does not exist`() {
    val prisonNumber = givenValidPrisonNumber("I1234DS")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }
    val request = investigationRequest()

    val response = updateInvestigationResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing an investigation.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing an investigation.")
      assertThat(moreInfo).isEqualTo(record.id.toString())
    }
  }

  @Test
  fun `200 ok - no changes made to investigation`() {
    val prisonNumber = givenValidPrisonNumber("I1234NC")
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral()
      requireNotNull(it.referral).withInvestigation()
      it
    }

    requireNotNull(record.referral?.investigation)
    val request = investigationRequest()

    val response = updateInvestigation(record.id, request, status = HttpStatus.OK)
    response.verifyAgainst(request)
    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - update investigation`() {
    val prisonNumber = givenValidPrisonNumber("I1234UI")
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral()
      requireNotNull(it.referral).withInvestigation(
        "oldStaffInvolved",
        "oldEvidenceSecured",
        "oldOccurrenceReason",
        "oldPersonsUsualBehaviour",
        "oldPersonsTrigger",
        "oldProtectiveFactors",
      )
      it
    }

    requireNotNull(record.referral?.investigation)
    val request = investigationRequest()

    val response = updateInvestigation(record.id, request, status = HttpStatus.OK)
    response.verifyAgainst(request)

    val investigation = getInvestigation(record.id)
    verifyAudit(
      investigation,
      RevisionType.MOD,
      setOf(CsipComponent.INVESTIGATION),
    )

    verifyDomainEvents(
      prisonNumber,
      record.id,
      setOf(CsipComponent.INVESTIGATION),
      setOf(DomainEventType.CSIP_UPDATED),
    )
  }

  private fun getInvestigation(recordUuid: UUID) =
    csipRecordRepository.getCsipRecord(recordUuid).referral!!.investigation!!

  private fun Investigation.verifyAgainst(request: UpsertInvestigationRequest) {
    assertThat(staffInvolved).isEqualTo(request.staffInvolved)
    assertThat(evidenceSecured).isEqualTo(request.evidenceSecured)
    assertThat(occurrenceReason).isEqualTo(request.occurrenceReason)
    assertThat(personsUsualBehaviour).isEqualTo(request.personsUsualBehaviour)
    assertThat(personsTrigger).isEqualTo(request.personsTrigger)
    assertThat(protectiveFactors).isEqualTo(request.protectiveFactors)
  }

  private fun investigationRequest() = UpsertInvestigationRequest(
    staffInvolved = "staffInvolved",
    evidenceSecured = "evidenceSecured",
    occurrenceReason = "occurrenceReason",
    personsUsualBehaviour = "personsUsualBehaviour",
    personsTrigger = "personsTrigger",
    protectiveFactors = "protectiveFactors",
  )

  private fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/referral/investigation"

  private fun updateInvestigationResponseSpec(
    recordUuid: UUID,
    request: UpsertInvestigationRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role))).exchange()

  private fun updateInvestigation(
    recordUuid: UUID,
    request: UpsertInvestigationRequest,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
    status: HttpStatus,
  ) = updateInvestigationResponseSpec(recordUuid, request, username, role)
    .successResponse<Investigation>(status)
}
