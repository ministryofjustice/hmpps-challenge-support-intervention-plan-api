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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Record
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction.DELETED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.LocalDate
import java.util.UUID

class DeleteCsipRecordsIntTest : IntegrationTestBase() {

  @Autowired
  lateinit var transactionTemplate: TransactionTemplate

  @Test
  fun `401 unauthorised`() {
    webTestClient.delete().uri(urlToTest(UUID.randomUUID())).exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = deleteCsipRecordResponseSpec(UUID.randomUUID(), role = role)
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
    val response = webTestClient.delete().uri(urlToTest(UUID.randomUUID()))
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
    val response = deleteCsipRecordResponseSpec(UUID.randomUUID(), username = null)
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
    val response = deleteCsipRecordResponseSpec(UUID.randomUUID(), username = USER_NOT_FOUND)
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
  fun `200 ok - csip record already deleted`() {
    val uuid = UUID.randomUUID()
    deleteCsipRecordResponseSpec(uuid).expectStatus().isOk
  }

  @Test
  fun `204 no content - CSIP record soft deleted by DPS`() {
    val prisonNumber = givenValidPrisonNumber("D1234DS")
    val record = transactionTemplate.execute {
      val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
      val referral = requireNotNull(record.referral)
        .withContributoryFactor()
        .withContributoryFactor()
        .withInvestigation()
      requireNotNull(referral.investigation)
        .withInterview(interviewDate = LocalDate.now().minusDays(2))
        .withInterview(interviewDate = LocalDate.now().minusDays(1))
        .withInterview()
      record
    }!!

    val factorUuids = record.referral!!.contributoryFactors().map { it.contributoryFactorUuid }.toSet()
    assertThat(factorUuids).hasSize(2)

    val interviewIds = record.referral!!.investigation!!.interviews().map { it.interviewUuid }.toSet()
    assertThat(interviewIds).hasSize(3)

    deleteCsipRecordResponseSpec(record.recordUuid).expectStatus().isNoContent

    verifyDoesNotExist(csipRecordRepository.findByRecordUuid(record.recordUuid)) { IllegalStateException("CSIP record not deleted") }
    verifyAudit(record, DELETED, setOf(Record, Referral, ContributoryFactor, Investigation, Interview), "CSIP deleted")

    verifyDomainEvents(
      prisonNumber,
      record.recordUuid,
      setOf(Record, Referral, ContributoryFactor, Investigation, Interview),
      setOf(DomainEventType.CSIP_DELETED),
      entityIds = factorUuids + interviewIds,
      expectedCount = 6,
    )
  }

  @Test
  fun `204 no content - CSIP record soft deleted by NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("D1234NS")
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
    deleteCsipRecordResponseSpec(record.recordUuid, NOMIS, NOMIS_SYS_USER, ROLE_NOMIS).expectStatus().isNoContent

    verifyDoesNotExist(csipRecordRepository.findByRecordUuid(record.recordUuid)) { IllegalStateException("CSIP record not deleted") }

    verifyAudit(
      record,
      DELETED,
      setOf(Record, Referral),
      "CSIP deleted",
      source = NOMIS,
      username = NOMIS_SYS_USER,
      userDisplayName = NOMIS_SYS_USER_DISPLAY_NAME,
    )

    verifyDomainEvents(
      prisonNumber,
      record.recordUuid,
      setOf(Record, Referral),
      setOf(DomainEventType.CSIP_DELETED),
      source = NOMIS,
    )
  }

  private fun urlToTest(csipRecordUuid: UUID) = "/csip-records/$csipRecordUuid"

  private fun deleteCsipRecordResponseSpec(
    uuid: UUID,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.delete()
    .uri(urlToTest(uuid))
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username))
    .exchange()
}
