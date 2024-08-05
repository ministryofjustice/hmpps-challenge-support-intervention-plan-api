package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.transaction.support.TransactionTemplate
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createInterviewRequest
import java.util.UUID

class CreateInvestigationsIntTest : IntegrationTestBase() {

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
    val response = createInvestigationResponseSpec(UUID.randomUUID(), createInvestigationRequest(), role = role)
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
      .bodyValue(createInvestigationRequest()).headers(setAuthorisation()).headers { it.set(SOURCE, "INVALID") }
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
    val request = createInvestigationRequest()

    val response = createInvestigationResponseSpec(recordUuid, request, username = null)
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
    val request = createInvestigationRequest()

    val response = createInvestigationResponseSpec(recordUuid, request, username = "UNKNOWN")
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

    val response = createInvestigationResponseSpec(recordUuid, createInvestigationRequest())
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
    val response = createInvestigationResponseSpec(recordUuid, createInvestigationRequest())
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
    val request = createInvestigationRequest()

    val response = createInvestigation(recordUuid, request)

    val affectedComponents = setOf(AffectedComponent.Investigation)

    response.verifyAgainst(request)
    verifyAudit(
      csipRecord,
      AuditEventAction.CREATED,
      affectedComponents,
      "Investigation added to referral",
    )

    verifyDomainEvents(
      prisonNumber,
      recordUuid,
      affectedComponents,
      setOf(DomainEventType.CSIP_UPDATED),
    )
  }

  @Test
  fun `201 created - create investigation via NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("I1234NS")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createInvestigationRequest()

    val response = createInvestigation(
      recordUuid,
      request,
      source = Source.NOMIS,
      username = NOMIS_SYS_USER,
    )

    response.verifyAgainst(request)
    verifyAudit(
      csipRecord,
      AuditEventAction.CREATED,
      setOf(AffectedComponent.Investigation),
      "Investigation added to referral",
      Source.NOMIS,
      NOMIS_SYS_USER,
      NOMIS_SYS_USER_DISPLAY_NAME,
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
  fun `201 created - create investigation with interviews via DPS UI`() {
    val prisonNumber = givenValidPrisonNumber("I1234WI")
    val csipRecord = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    val recordUuid = csipRecord.recordUuid
    val request = createInvestigationRequest(interviews = listOf(createInterviewRequest(), createInterviewRequest()))

    val response = createInvestigation(recordUuid, request)
    val interviewUuids = response.interviews.map { it.interviewUuid }

    val affectedComponents = setOf(AffectedComponent.Investigation, AffectedComponent.Interview)

    response.verifyAgainst(request)
    verifyAudit(
      csipRecord,
      AuditEventAction.CREATED,
      affectedComponents,
      "Investigation added to referral",
    )

    verifyDomainEvents(
      prisonNumber,
      recordUuid,
      affectedComponents,
      setOf(DomainEventType.CSIP_UPDATED),
      interviewUuids.toSet(),
      3,
    )
  }

  private fun Investigation.verifyAgainst(request: CreateInvestigationRequest) {
    assertThat(staffInvolved).isEqualTo(request.staffInvolved)
    assertThat(evidenceSecured).isEqualTo(request.evidenceSecured)
    assertThat(occurrenceReason).isEqualTo(request.occurrenceReason)
    assertThat(personsUsualBehaviour).isEqualTo(request.personsUsualBehaviour)
    assertThat(personsTrigger).isEqualTo(request.personsTrigger)
    assertThat(protectiveFactors).isEqualTo(request.protectiveFactors)
  }

  private fun createInvestigationRequest(interviews: List<CreateInterviewRequest> = listOf()) =
    CreateInvestigationRequest(
      staffInvolved = "staffInvolved",
      evidenceSecured = "evidenceSecured",
      occurrenceReason = "occurrenceReason",
      personsUsualBehaviour = "personsUsualBehaviour",
      personsTrigger = "personsTrigger",
      protectiveFactors = "protectiveFactors",
      interviews,
    )

  private fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/referral/investigation"

  private fun createInvestigationResponseSpec(
    recordUuid: UUID,
    request: CreateInvestigationRequest,
    source: Source = Source.DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username)).exchange()

  private fun createInvestigation(
    recordUuid: UUID,
    request: CreateInvestigationRequest,
    source: Source = Source.DPS,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
  ) = createInvestigationResponseSpec(recordUuid, request, source, username, role)
    .successResponse<Investigation>(HttpStatus.CREATED)
}
