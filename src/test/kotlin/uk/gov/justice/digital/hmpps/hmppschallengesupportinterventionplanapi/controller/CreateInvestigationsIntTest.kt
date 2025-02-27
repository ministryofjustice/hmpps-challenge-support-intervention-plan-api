package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.INTERVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ValidInvestigationDetail.Companion.WITH_INTERVIEW_MESSAGE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createInterviewRequest
import java.util.UUID

class CreateInvestigationsIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE", ROLE_NOMIS])
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
    val record = givenCsipRecord(generateCsipRecord())

    val response = createInvestigationResponseSpec(record.id, createInvestigationRequest())
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
  fun `400 bad request - attempt to create investigation without interviews or fields populated`() {
    val recordUuid = UUID.randomUUID()
    val response = createInvestigationResponseSpec(
      recordUuid,
      createInvestigationRequest(null, null, null, null, null, null),
    ).errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(userMessage).isEqualTo("Validation failure: $WITH_INTERVIEW_MESSAGE")
      assertThat(developerMessage).isEqualTo("400 BAD_REQUEST Validation failure: $WITH_INTERVIEW_MESSAGE")
    }
  }

  @Test
  fun `409 conflict - Investigation already exists`() {
    val record = dataSetup(generateCsipRecord()) {
      it.withReferral()
      requireNotNull(it.referral).withInvestigation()
      it
    }

    val response = createInvestigationResponseSpec(record.id, createInvestigationRequest())
      .errorResponse(HttpStatus.CONFLICT)

    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: Referral already has an investigation")
      assertThat(developerMessage).isEqualTo("Referral already has an investigation")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - create investigation with interviews via DPS UI`() {
    val record = dataSetup(generateCsipRecord()) { it.withReferral() }
    val request = createInvestigationRequest(interviews = listOf(createInterviewRequest(), createInterviewRequest()))

    val response = createInvestigation(record.id, request)
    response.verifyAgainst(request)
    assertThat(response.recordedBy).isEqualTo(TEST_USER)
    assertThat(response.recordedByDisplayName).isEqualTo(TEST_USER_NAME)

    val investigation = getInvestigation(record.id)
    verifyAudit(investigation, RevisionType.ADD, setOf(CsipComponent.INVESTIGATION, INTERVIEW))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  @Test
  fun `201 created - create investigation with recorded by details`() {
    val username = "J1264BC"
    val displayName = "John Smith"
    val record = dataSetup(generateCsipRecord()) { it.withReferral() }
    val request = createInvestigationRequest(recordedBy = username, recordedByDisplayName = displayName)

    val response = createInvestigation(record.id, request)
    response.verifyAgainst(request)
    assertThat(response.recordedBy).isEqualTo(request.recordedBy)
    assertThat(response.recordedByDisplayName).isEqualTo(request.recordedByDisplayName)

    val investigation = getInvestigation(record.id)
    assertThat(investigation.recordedBy).isEqualTo(request.recordedBy)
    assertThat(investigation.recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
    verifyAudit(investigation, RevisionType.ADD, setOf(CsipComponent.INVESTIGATION))
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_UPDATED)
  }

  private fun Investigation.verifyAgainst(request: CreateInvestigationRequest) {
    assertThat(staffInvolved).isEqualTo(request.staffInvolved)
    assertThat(evidenceSecured).isEqualTo(request.evidenceSecured)
    assertThat(occurrenceReason).isEqualTo(request.occurrenceReason)
    assertThat(personsUsualBehaviour).isEqualTo(request.personsUsualBehaviour)
    assertThat(personsTrigger).isEqualTo(request.personsTrigger)
    assertThat(protectiveFactors).isEqualTo(request.protectiveFactors)
    assertThat(interviews.size).isEqualTo(request.interviews.size)
  }

  private fun createInvestigationRequest(
    staffInvolved: String? = "staffInvolved",
    evidenceSecured: String? = "evidenceSecured",
    occurrenceReason: String? = "occurrenceReason",
    personsUsualBehaviour: String? = "personsUsualBehaviour",
    personsTrigger: String? = "personsTrigger",
    protectiveFactors: String? = "protectiveFactors",
    recordedBy: String? = null,
    recordedByDisplayName: String? = null,
    interviews: List<CreateInterviewRequest> = listOf(),
  ) = CreateInvestigationRequest(
    staffInvolved,
    evidenceSecured,
    occurrenceReason,
    personsUsualBehaviour,
    personsTrigger,
    protectiveFactors,
    recordedBy,
    recordedByDisplayName,
    interviews,
  )

  private fun urlToTest(recordUuid: UUID) = "/csip-records/$recordUuid/referral/investigation"

  private fun createInvestigationResponseSpec(
    recordUuid: UUID,
    request: CreateInvestigationRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post().uri(urlToTest(recordUuid)).bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role))).exchange()

  private fun createInvestigation(
    recordUuid: UUID,
    request: CreateInvestigationRequest,
    role: String? = ROLE_CSIP_UI,
    username: String = TEST_USER,
  ) = createInvestigationResponseSpec(recordUuid, request, username, role)
    .successResponse<Investigation>(HttpStatus.CREATED)

  private fun getInvestigation(recordUuid: UUID) = csipRecordRepository.getCsipRecord(recordUuid).referral!!.investigation!!
}
