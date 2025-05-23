package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

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
import org.springframework.http.HttpStatus.OK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.ContributoryFactorRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.getContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.CONTRIBUTORY_FACTOR
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpdateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.Duration.ofSeconds
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
    val response =
      updateContributoryFactorResponseSpec(randomUUID(), updateContributoryFactorRequest(), role = role).errorResponse(
        HttpStatus.FORBIDDEN,
      )

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
    val response =
      updateContributoryFactorResponseSpec(uuid, updateContributoryFactorRequest()).errorResponse(HttpStatus.NOT_FOUND)

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
    val factorUuid = dataSetup(generateCsipRecord().withReferral()) {
      requireNotNull(it.referral).withContributoryFactor(
        type = givenReferenceData(CONTRIBUTORY_FACTOR_TYPE, "AFL"),
      ).withContributoryFactor(
        type = givenReferenceData(CONTRIBUTORY_FACTOR_TYPE, "BAS"),
      ).contributoryFactors().single { it.contributoryFactorType.code == "AFL" }.id
    }

    val response = updateContributoryFactorResponseSpec(
      factorUuid,
      updateContributoryFactorRequest(
        factorTypeCode = "BAS",
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
  fun `200 ok - contributory factor updated`() {
    val factor = dataSetup(generateCsipRecord().withReferral()) {
      val referral = requireNotNull(it.referral)
        .withContributoryFactor(type = givenReferenceData(CONTRIBUTORY_FACTOR_TYPE, "AFL"))
      referral.contributoryFactors().first()
    }

    val request =
      updateContributoryFactorRequest(factorTypeCode = "BAS", comment = "An updated comment to replace the original")
    val response = updateContributoryFactor(factor.id, request)

    val saved = getContributoryFactory(response.factorUuid)
    assertThat(saved.contributoryFactorType.code).isEqualTo(request.factorTypeCode)
    assertThat(saved.comment).isEqualTo(request.comment)
    verifyAudit(saved, RevisionType.MOD, setOf(CONTRIBUTORY_FACTOR))
    verifyDomainEvents(factor.csipRecord().prisonNumber, factor.csipRecord().id, CSIP_UPDATED)
  }

  @Test
  fun `200 ok - contributory factor not updated with no change`() {
    val factor = dataSetup(generateCsipRecord().withReferral()) {
      val referral = requireNotNull(it.referral).withContributoryFactor()
      referral.contributoryFactors().first()
    }

    val request =
      updateContributoryFactorRequest(factorTypeCode = factor.contributoryFactorType.code, comment = factor.comment)
    val response = updateContributoryFactor(factor.id, request)

    val saved = getContributoryFactory(response.factorUuid)
    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(RECORD, REFERRAL, CONTRIBUTORY_FACTOR),
      nomisContext().copy(source = Source.DPS),
    )
    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsTestQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  private fun urlToTest(factorId: UUID) = "/csip-records/referral/contributory-factors/$factorId"

  private fun updateContributoryFactorRequest(
    factorTypeCode: String = "BAS",
    comment: String? = "comment about the factor",
  ) = UpdateContributoryFactorRequest(factorTypeCode, comment)

  private fun updateContributoryFactorResponseSpec(
    factorId: UUID,
    request: UpdateContributoryFactorRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch().uri(urlToTest(factorId)).bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role))).exchange()

  private fun updateContributoryFactor(
    csipUuid: UUID,
    request: UpdateContributoryFactorRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): ContributoryFactor = updateContributoryFactorResponseSpec(csipUuid, request, username, role).successResponse(OK)

  private fun getContributoryFactory(uuid: UUID) = contributoryFactorRepository.getContributoryFactor(uuid)
}
