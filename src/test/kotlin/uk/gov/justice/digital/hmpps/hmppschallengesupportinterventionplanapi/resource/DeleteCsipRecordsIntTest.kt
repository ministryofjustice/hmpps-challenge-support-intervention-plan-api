package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.CONTRIBUTORY_FACTOR
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.IDENTIFIED_NEED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.INTERVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.INVESTIGATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.PLAN
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.nomisContext
import java.time.LocalDate
import java.util.UUID

class DeleteCsipRecordsIntTest : IntegrationTestBase() {

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
  fun `204 no content - CSIP record deleted by DPS`() {
    val prisonNumber = givenValidPrisonNumber("D1234DS")
    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral()
      val referral = requireNotNull(it.referral)
        .withContributoryFactor()
        .withContributoryFactor()
        .withInvestigation()
      requireNotNull(referral.investigation)
        .withInterview(interviewDate = LocalDate.now().minusDays(2))
        .withInterview(interviewDate = LocalDate.now().minusDays(1))
        .withInterview()
      it.withPlan()
      val plan = requireNotNull(it.plan)
        .withNeed()
        .withReview()
        .withReview()
      val review1 = plan.reviews().first()
      review1.withAttendee()
      it
    }

    val factorUuids = record.referral!!.contributoryFactors().map { it.uuid }.toSet()
    assertThat(factorUuids).hasSize(2)

    val interviewIds = record.referral!!.investigation!!.interviews().map { it.uuid }.toSet()
    assertThat(interviewIds).hasSize(3)

    val needsIds = record.plan!!.identifiedNeeds().map { it.uuid }.toSet()
    assertThat(needsIds).hasSize(1)

    val reviewIds = record.plan!!.reviews().map { it.uuid }.toSet()
    assertThat(reviewIds).hasSize(2)

    val attendeeIds = record.plan!!.reviews().flatMap { r -> r.attendees().map { it.uuid } }
    assertThat(attendeeIds).hasSize(1)

    deleteCsipRecordResponseSpec(record.uuid).expectStatus().isNoContent

    val affectedComponents =
      setOf(
        RECORD, REFERRAL, CONTRIBUTORY_FACTOR, INVESTIGATION, INTERVIEW, PLAN, IDENTIFIED_NEED, REVIEW,
        CsipComponent.ATTENDEE,
      )
    verifyDoesNotExist(csipRecordRepository.findByUuid(record.uuid)) { IllegalStateException("CSIP record not deleted") }
    verifyAudit(
      record,
      RevisionType.DEL,
      affectedComponents,
    )

    verifyDomainEvents(
      prisonNumber,
      record.uuid,
      affectedComponents,
      setOf(
        DomainEventType.CSIP_DELETED,
        DomainEventType.CONTRIBUTORY_FACTOR_DELETED,
        DomainEventType.INTERVIEW_DELETED,
        DomainEventType.IDENTIFIED_NEED_DELETED,
        DomainEventType.REVIEW_DELETED,
        DomainEventType.ATTENDEE_DELETED,
      ),
      entityIds = factorUuids + interviewIds + needsIds + reviewIds + attendeeIds,
      // expected messages: 1 csip, 2 factors, 3 interviews, 1 identified need, 2 reviews, 2 attendees
      expectedCount = 10,
    )
  }

  @Test
  fun `204 no content - CSIP record deleted by NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("D1234NS")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }
    deleteCsipRecordResponseSpec(record.uuid, NOMIS, NOMIS_SYS_USER, ROLE_NOMIS).expectStatus().isNoContent

    verifyDoesNotExist(csipRecordRepository.findByUuid(record.uuid)) { IllegalStateException("CSIP record not deleted") }

    verifyAudit(
      record,
      RevisionType.DEL,
      setOf(RECORD, REFERRAL),
      nomisContext(),
    )

    verifyDomainEvents(
      prisonNumber,
      record.uuid,
      setOf(RECORD, REFERRAL),
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
