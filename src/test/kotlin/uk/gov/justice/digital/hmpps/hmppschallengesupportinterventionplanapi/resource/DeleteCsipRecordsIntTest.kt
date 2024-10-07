package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.data.repository.findByIdOrNull
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.toPersonLocation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.ATTENDEE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.CONTRIBUTORY_FACTOR
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.IDENTIFIED_NEED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.INTERVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.INVESTIGATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.PLAN
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REVIEW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_DELETED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.verifyDoesNotExist
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
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
    val record = dataSetup(generateCsipRecord()) {
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

    val factorUuids = record.referral!!.contributoryFactors().map { it.id }.toSet()
    assertThat(factorUuids).hasSize(2)

    val interviewIds = record.referral!!.investigation!!.interviews().map { it.id }.toSet()
    assertThat(interviewIds).hasSize(3)

    val needsIds = record.plan!!.identifiedNeeds().map { it.id }.toSet()
    assertThat(needsIds).hasSize(1)

    val reviewIds = record.plan!!.reviews().map { it.id }.toSet()
    assertThat(reviewIds).hasSize(2)

    val attendeeIds = record.plan!!.reviews().flatMap { r -> r.attendees().map { it.id } }
    assertThat(attendeeIds).hasSize(1)

    deleteCsipRecordResponseSpec(record.id).expectStatus().isNoContent

    val affectedComponents =
      setOf(RECORD, REFERRAL, CONTRIBUTORY_FACTOR, INVESTIGATION, INTERVIEW, PLAN, IDENTIFIED_NEED, REVIEW, ATTENDEE)
    verifyDoesNotExist(csipRecordRepository.findById(record.id)) { IllegalStateException("CSIP record not deleted") }
    verifyDoesNotExist(personLocationRepository.findByIdOrNull(record.prisonNumber)) {
      IllegalStateException("Person Location not deleted")
    }
    verifyAudit(record, RevisionType.DEL, affectedComponents)
    verifyDomainEvents(record.prisonNumber, record.id, CSIP_DELETED)
  }

  @Test
  fun `204 no content - Delete one of multiple leaves Person Location`() {
    val personLocation = prisoner().toPersonLocation()
    dataSetup(generateCsipRecord(personLocation).withCompletedReferral()) { it }
    val toDelete = dataSetup(generateCsipRecord(personLocation).withReferral()) { it }
    assertThat(csipRecordRepository.countByPrisonNumber(toDelete.prisonNumber)).isEqualTo(2)

    deleteCsipRecordResponseSpec(toDelete.id).expectStatus().isNoContent

    verifyDoesNotExist(csipRecordRepository.findById(toDelete.id)) { IllegalStateException("CSIP record not deleted") }
    assertThat(personLocationRepository.findByIdOrNull(toDelete.prisonNumber)).isNotNull()
    assertThat(csipRecordRepository.countByPrisonNumber(toDelete.prisonNumber)).isEqualTo(1)
    verifyDomainEvents(toDelete.prisonNumber, toDelete.id, CSIP_DELETED)
  }

  private fun urlToTest(csipRecordUuid: UUID) = "/csip-records/$csipRecordUuid"

  private fun deleteCsipRecordResponseSpec(
    uuid: UUID,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.delete()
    .uri(urlToTest(uuid))
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()
}
