package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.CONTRIBUTORY_FACTOR
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CONTRIBUTORY_FACTOR_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ContributoryFactorRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.UUID
import java.util.UUID.randomUUID

class UpdateContributoryFactorIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var contributoryFactorRepository: ContributoryFactorRepository

  @Test
  fun `401 unauthorised`() {
    webTestClient.patch().uri(urlToTest(randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE", ROLE_NOMIS])
  fun `403 forbidden - no required role`(role: String?) {
    val response = updateContributoryFactorResponseSpec(randomUUID(), updateContributoryFactorRequest(), role = role)
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
    val response = updateContributoryFactorResponseSpec(
      randomUUID(),
      updateContributoryFactorRequest(),
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
  fun `404 not found - contributory factor not found`() {
    val uuid = randomUUID()
    val response = updateContributoryFactorResponseSpec(uuid, updateContributoryFactorRequest())
      .errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: Contributory Factor not found")
      assertThat(developerMessage).isEqualTo("Contributory Factor not found with identifier $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `409 conflict - contributory factor already present`() {
    val prisonNumber = givenValidPrisonNumber("C1234FE")
    val factorUuid = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral()
      requireNotNull(it.referral)
        .withContributoryFactor(
          type = givenReferenceData(CONTRIBUTORY_FACTOR_TYPE, "AFL"),
        )
        .withContributoryFactor(
          type = givenReferenceData(CONTRIBUTORY_FACTOR_TYPE, "BAS"),
        )
        .contributoryFactors().last().id
    }

    val response = updateContributoryFactorResponseSpec(
      factorUuid,
      updateContributoryFactorRequest(
        factorTypeCode = "BAS",
      ),
    )
      .errorResponse(HttpStatus.CONFLICT)

    with(response) {
      assertThat(status).isEqualTo(409)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Conflict failure: Contributory factor already part of referral")
      assertThat(developerMessage).isEqualTo("Contributory factor already part of referral")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - contributory factor updated`() {
    val prisonNumber = givenValidPrisonNumber("F1234NC")
    val factor = dataSetup(generateCsipRecord(prisonNumber).withReferral()) {
      val referral = requireNotNull(it.referral).withContributoryFactor()
      referral.contributoryFactors().first()
    }

    val request = updateContributoryFactorRequest(comment = "An updated comment to replace the original")
    val response = updateContributoryFactor(factor.id, request)

    val saved = getContributoryFactory(response.factorUuid)
    assertThat(saved.comment).isEqualTo(request.comment)
    assertThat(saved.lastModifiedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(saved.lastModifiedBy).isEqualTo(TEST_USER)
    assertThat(saved.lastModifiedByDisplayName).isEqualTo(TEST_USER_NAME)
    verifyAudit(saved, RevisionType.MOD, setOf(CONTRIBUTORY_FACTOR))

    verifyDomainEvents(
      prisonNumber,
      factor.csipRecord().id,
      setOf(CONTRIBUTORY_FACTOR),
      setOf(CONTRIBUTORY_FACTOR_UPDATED),
      setOf(response.factorUuid),
    )
  }

  @Test
  fun `200 ok - contributory factor not updated with no change`() {
    val prisonNumber = givenValidPrisonNumber("F1234UP")
    val factor = dataSetup(generateCsipRecord(prisonNumber).withReferral()) {
      val referral = requireNotNull(it.referral).withContributoryFactor()
      referral.contributoryFactors().first()
    }

    val request =
      updateContributoryFactorRequest(factorTypeCode = factor.contributoryFactorType.code, comment = factor.comment)
    val response = updateContributoryFactor(factor.id, request)

    val saved = getContributoryFactory(response.factorUuid)
    assertThat(saved.lastModifiedAt).isNull()
    assertThat(saved.lastModifiedBy).isNull()
    assertThat(saved.lastModifiedByDisplayName).isNull()
    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(RECORD, REFERRAL, CONTRIBUTORY_FACTOR),
      nomisContext().copy(source = Source.DPS),
    )
  }

  private fun urlToTest(factorId: UUID) = "/csip-records/referral/contributory-factors/$factorId"

  private fun updateContributoryFactorRequest(
    factorTypeCode: String = "BAS",
    comment: String? = "comment about the factor",
  ) =
    UpdateContributoryFactorRequest(factorTypeCode, comment)

  private fun updateContributoryFactorResponseSpec(
    factorId: UUID,
    request: UpdateContributoryFactorRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch()
    .uri(urlToTest(factorId))
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun updateContributoryFactor(
    csipUuid: UUID,
    request: UpdateContributoryFactorRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.ContributoryFactor =
    updateContributoryFactorResponseSpec(csipUuid, request, username, role).successResponse(OK)

  private fun getContributoryFactory(uuid: UUID) = contributoryFactorRepository.getContributoryFactor(uuid)
}
