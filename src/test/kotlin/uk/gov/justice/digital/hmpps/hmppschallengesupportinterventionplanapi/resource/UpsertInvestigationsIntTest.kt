package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.history.RevisionMetadata.RevisionType.UPDATE
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.Duration.ofSeconds
import java.util.UUID

class UpsertInvestigationsIntTest : IntegrationTestBase() {

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
    val response = upsertInvestigationResponseSpec(UUID.randomUUID(), investigationRequest(), role = role)
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
      .bodyValue(investigationRequest()).headers(setAuthorisation()).headers { it.set(SOURCE, "INVALID") }
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
    val request = investigationRequest()

    val response = upsertInvestigationResponseSpec(recordUuid, request, username = null)
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
    val request = investigationRequest()

    val response = upsertInvestigationResponseSpec(recordUuid, request, username = "UNKNOWN")
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
  fun `400 bad request - CSIP record missing a referral`() {
    val prisonNumber = givenValidPrisonNumber("I2234MR")
    val csipRecord = givenCsipRecord(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid

    val response = upsertInvestigationResponseSpec(recordUuid, investigationRequest())
      .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Invalid request: CSIP Record is missing a referral.")
      assertThat(developerMessage).isEqualTo("CSIP Record is missing a referral.")
      assertThat(moreInfo).isEqualTo(recordUuid.toString())
    }
  }

  @Test
  fun `404 not found - CSIP record not found`() {
    val recordUuid = UUID.randomUUID()
    val response = upsertInvestigationResponseSpec(recordUuid, investigationRequest())
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
    val prisonNumber = givenValidPrisonNumber("I1234DS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = investigationRequest()

    val response = upsertInvestigation(recordUuid, request, status = HttpStatus.CREATED)

    response.verifyAgainst(request)
    verifyAudit(
      csipRecord,
      UPDATE,
      setOf(AffectedComponent.Investigation, AffectedComponent.Referral, AffectedComponent.Record),
    )

    verifyDomainEvents(
      prisonNumber,
      recordUuid,
      setOf(AffectedComponent.Investigation),
      setOf(DomainEventType.CSIP_UPDATED),
    )
  }

  @Test
  fun `201 created - create investigation via NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("I1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = investigationRequest()

    val response = upsertInvestigation(
      recordUuid,
      request,
      source = Source.NOMIS,
      username = NOMIS_SYS_USER,
      role = ROLE_NOMIS,
      status = HttpStatus.CREATED,
    )

    response.verifyAgainst(request)
    verifyAudit(
      csipRecord,
      UPDATE,
      setOf(AffectedComponent.Investigation, AffectedComponent.Referral, AffectedComponent.Record),
      nomisContext(),
    )

    verifyDomainEvents(
      prisonNumber,
      recordUuid,
      setOf(AffectedComponent.Investigation),
      setOf(DomainEventType.CSIP_UPDATED),
      source = Source.NOMIS,
    )
  }

  @Test
  fun `200 ok - no changes made to investigation`() {
    val prisonNumber = givenValidPrisonNumber("I1234NC")
    val csipRecord = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
      requireNotNull(csip.referral).withInvestigation()
      csip
    }!!

    requireNotNull(csipRecord.referral?.investigation)
    val request = investigationRequest()

    val response = upsertInvestigation(csipRecord.recordUuid, request, status = HttpStatus.OK)
    response.verifyAgainst(request)
    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - update investigation`() {
    val prisonNumber = givenValidPrisonNumber("I1234UI")
    val csipRecord = transactionTemplate.execute {
      val csip = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
      requireNotNull(csip.referral).withInvestigation(
        "oldStaffInvolved",
        "oldEvidenceSecured",
        "oldOccurrenceReason",
        "oldPersonsUsualBehaviour",
        "oldPersonsTrigger",
        "oldProtectiveFactors",
      )
      csip
    }!!

    requireNotNull(csipRecord.referral?.investigation)
    val request = investigationRequest()

    val response = upsertInvestigation(csipRecord.recordUuid, request, status = HttpStatus.OK)
    response.verifyAgainst(request)
    verifyAudit(
      csipRecord,
      UPDATE,
      setOf(AffectedComponent.Investigation, AffectedComponent.Referral, AffectedComponent.Record),
    )

    verifyDomainEvents(
      prisonNumber,
      csipRecord.recordUuid,
      setOf(AffectedComponent.Investigation),
      setOf(DomainEventType.CSIP_UPDATED),
    )
  }

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

  private fun upsertInvestigationResponseSpec(
    recordUuid: UUID,
    request: UpsertInvestigationRequest,
    source: Source = Source.DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.put().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username)).exchange()

  private fun upsertInvestigation(
    recordUuid: UUID,
    request: UpsertInvestigationRequest,
    source: Source = Source.DPS,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
    status: HttpStatus,
  ) = upsertInvestigationResponseSpec(recordUuid, request, source, username, role)
    .successResponse<Investigation>(status)
}
