package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.CONTRIBUTORY_FACTOR
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CONTRIBUTORY_FACTOR_CREATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ContributoryFactorRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.createContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.UUID.randomUUID

class AddContributoryFactorIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var contributoryFactorRepository: ContributoryFactorRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.post().uri(urlToTest(randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = addContributoryFactorResponseSpec(randomUUID(), createContributoryFactorRequest(), role = role)
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
    val response = addContributoryFactorResponseSpec(randomUUID(), createContributoryFactorRequest(), username = null)
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
    val response = addContributoryFactorResponseSpec(
      randomUUID(),
      createContributoryFactorRequest(),
      username = USER_NOT_FOUND,
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
  fun `400 bad request - factor type code too long`() {
    val response =
      addContributoryFactorResponseSpec(randomUUID(), createContributoryFactorRequest(type = "n".repeat(13)))
        .errorResponse(HttpStatus.BAD_REQUEST)

    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(userMessage).isEqualTo("Validation failure: Contributory factor type code must be <= 12 characters")
    }
  }

  @ParameterizedTest
  @MethodSource("referenceDataValidation")
  fun `400 bad request - reference data invalid or inactive`(
    request: CreateContributoryFactorRequest,
    invalid: InvalidRd,
  ) {
    val prisonNumber = givenValidPrisonNumber("R1234VC")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber).withReferral())
    val response = addContributoryFactorResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: ${invalid.type} ${invalid.message}")
      assertThat(developerMessage).isEqualTo("Details => ${invalid.type}:${invalid.code(request)}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `404 not found - csip record not found`() {
    val uuid = randomUUID()
    val response = addContributoryFactorResponseSpec(uuid, createContributoryFactorRequest())
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
  fun `409 conflict - contributory factor already present`() {
    val prisonNumber = givenValidPrisonNumber("C1234FE")
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral()
      requireNotNull(it.referral).withContributoryFactor()
      it
    }

    val response = addContributoryFactorResponseSpec(
      record.id,
      createContributoryFactorRequest(
        type = record.referral!!.contributoryFactors().first().contributoryFactorType.code,
      ),
    ).errorResponse(HttpStatus.CONFLICT)

    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: Contributory factor already part of referral")
      assertThat(developerMessage).isEqualTo("Contributory factor already part of referral")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `201 created - contributory factor added DPS`() {
    val prisonNumber = givenValidPrisonNumber("C1234DP")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }

    val request = createContributoryFactorRequest()
    val response = addContributoryFactor(record.id, request)

    with(response) {
      assertThat(factorType.code).isEqualTo(request.factorTypeCode)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(TEST_USER)
      assertThat(createdByDisplayName).isEqualTo(TEST_USER_NAME)
    }

    val saved = getContributoryFactory(response.factorUuid)
    verifyAudit(saved, RevisionType.ADD, setOf(CONTRIBUTORY_FACTOR))

    verifyDomainEvents(
      prisonNumber,
      record.id,
      setOf(CONTRIBUTORY_FACTOR),
      setOf(CONTRIBUTORY_FACTOR_CREATED),
      setOf(response.factorUuid),
    )
  }

  @Test
  fun `201 created - contributory factor added NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("C1234NM")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }

    val request = createContributoryFactorRequest(comment = "This was done by nomis")
    val response = addContributoryFactor(record.id, request, NOMIS, NOMIS_SYS_USER, ROLE_NOMIS)

    with(response) {
      assertThat(factorType.code).isEqualTo(request.factorTypeCode)
      assertThat(comment).isEqualTo(request.comment)
      assertThat(createdAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(createdBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(createdByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
    }

    val saved = getContributoryFactory(response.factorUuid)
    verifyAudit(saved, RevisionType.ADD, setOf(CONTRIBUTORY_FACTOR), nomisContext())
  }

  private fun urlToTest(csipRecordUuid: UUID) = "/csip-records/$csipRecordUuid/referral/contributory-factors"

  private fun addContributoryFactorResponseSpec(
    csipUuid: UUID,
    request: CreateContributoryFactorRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.post()
    .uri(urlToTest(csipUuid))
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username))
    .exchange()

  private fun addContributoryFactor(
    csipUuid: UUID,
    request: CreateContributoryFactorRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ContributoryFactor =
    addContributoryFactorResponseSpec(csipUuid, request, source, username, role).successResponse(CREATED)

  private fun getContributoryFactory(uuid: UUID) = contributoryFactorRepository.getContributoryFactor(uuid)

  companion object {
    private const val INVALID = "is invalid"
    private const val NOT_ACTIVE = "is not active"

    @JvmStatic
    fun referenceDataValidation() = listOf(
      Arguments.of(
        createContributoryFactorRequest(type = "NONEXISTENT"),
        InvalidRd(CONTRIBUTORY_FACTOR_TYPE, CreateContributoryFactorRequest::factorTypeCode, INVALID),
      ),
      Arguments.of(
        createContributoryFactorRequest(type = "CFT_INACT"),
        InvalidRd(CONTRIBUTORY_FACTOR_TYPE, CreateContributoryFactorRequest::factorTypeCode, NOT_ACTIVE),
      ),
    )

    data class InvalidRd(
      val type: ReferenceDataType,
      val code: (CreateContributoryFactorRequest) -> String,
      val message: String,
    )
  }
}
