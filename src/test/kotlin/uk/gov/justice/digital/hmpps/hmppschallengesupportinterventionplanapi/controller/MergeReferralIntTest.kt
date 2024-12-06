package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.controller

import org.assertj.core.api.Assertions.assertThat
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.CONTRIBUTORY_FACTOR
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType.CSIP_UPDATED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.MergeContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.MergeReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

class MergeReferralIntTest : IntegrationTestBase() {

  @Test
  fun `401 unauthorised`() {
    webTestClient.put().uri("/csip-records/${UUID.randomUUID()}/referral").exchange().expectStatus().isUnauthorized
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = mergeReferralResponseSpec(UUID.randomUUID(), mergeReferralRequest(), role = role)
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
    val response = mergeReferralResponseSpec(UUID.randomUUID(), mergeReferralRequest(), username = USER_NOT_FOUND)
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
  fun `404 not found - csip record not found`() {
    val uuid = UUID.randomUUID()
    val response = mergeReferralResponseSpec(uuid, mergeReferralRequest())
      .errorResponse(HttpStatus.NOT_FOUND)

    with(response) {
      assertThat(status).isEqualTo(404)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Not found: CSIP Record not found")
      assertThat(developerMessage).isEqualTo("CSIP Record not found with identifier $uuid")
      assertThat(moreInfo).isNull()
    }
  }

  @ParameterizedTest
  @MethodSource("referenceDataValidation")
  fun `400 bad request - when reference data code invalid or inactive`(
    request: MergeReferralRequest,
    invalid: InvalidRd,
  ) {
    val record = givenCsipRecord(generateCsipRecord().withReferral())

    val response = mergeReferralResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: ${invalid.type} ${invalid.message}")
      assertThat(developerMessage).isEqualTo("Details => ${invalid.type}:${invalid.code(request)}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `409 conflict - attempt to add contributory factor already present without providing id for update`() {
    val record = dataSetup(generateCsipRecord().withReferral()) {
      requireNotNull(it.referral).withContributoryFactor()
      it
    }

    val response = mergeReferralResponseSpec(
      record.id,
      mergeReferralRequest(
        contributoryFactors = listOf(
          mergeContributoryFactorRequest(record.referral!!.contributoryFactors().first().contributoryFactorType.code),
        ),
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
  fun `200 ok - completes referral without contributory factors`() {
    val csip = dataSetup(generateCsipRecord().withReferral()) { it }
    val referral = requireNotNull(csip.referral)

    val request = mergeReferralRequest(
      incidentTypeCode = referral.incidentType.code,
      incidentLocationCode = referral.incidentLocation.code,
      refererAreaCode = referral.refererAreaOfWork.code,
      incidentInvolvementCode = referral.incidentInvolvement?.code,
      descriptionOfConcern = "Updated concerns",
      knownReasons = "Updated reasons",
      otherInformation = "Even more information that can change",
      isReferralComplete = true,
    )

    val response = mergeReferral(referral.id, request)
    assertThat(response.status.code).isEqualTo(CsipStatus.REFERRAL_SUBMITTED.name)
    with(response.referral) {
      assertThat(isReferralComplete).isEqualTo(true)
      assertThat(referralCompletedDate).isEqualTo(LocalDate.now())
      assertThat(referralCompletedBy).isEqualTo(TEST_USER)
      assertThat(referralCompletedByDisplayName).isEqualTo(TEST_USER_NAME)
    }

    val saved = csipRecordRepository.getCsipRecord(referral.id)
    with(saved) {
      assertThat(status?.code).isEqualTo(CsipStatus.REFERRAL_SUBMITTED.name)
      with(requireNotNull(saved.referral)) {
        assertThat(referralComplete).isEqualTo(true)
        assertThat(referralCompletedDate).isEqualTo(LocalDate.now())
        assertThat(referralCompletedBy).isEqualTo(TEST_USER)
        assertThat(referralCompletedByDisplayName).isEqualTo(TEST_USER_NAME)
      }
    }

    verifyAudit(referral, RevisionType.MOD, setOf(REFERRAL))
    verifyDomainEvents(csip.prisonNumber, saved.id, CSIP_UPDATED)
  }

  @Test
  fun `200 ok - completes referral with contributory factors`() {
    val referral = dataSetup(generateCsipRecord().withReferral()) {
      requireNotNull(it.referral)
        .withContributoryFactor(givenReferenceData(ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE, "MEN"))
        .withContributoryFactor(givenReferenceData(ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE, "SMO"))
    }

    val request = mergeReferralRequest(
      incidentTypeCode = referral.incidentType.code,
      incidentLocationCode = referral.incidentLocation.code,
      refererAreaCode = referral.refererAreaOfWork.code,
      incidentInvolvementCode = "OTH",
      descriptionOfConcern = "Updated concerns",
      knownReasons = "Updated reasons",
      otherInformation = "Even more information that can change",
      isProactiveReferral = true,
      isStaffAssaulted = false,
      contributoryFactors = listOf(
        mergeContributoryFactorRequest(type = "MED"),
        mergeContributoryFactorRequest(type = "NRT"),
        mergeContributoryFactorRequest(
          type = "MEN",
          id = referral.contributoryFactors().first { it.contributoryFactorType.code == "MEN" }.id,
        ),
      ),
    )

    val response = mergeReferral(referral.id, request)
    assertThat(response.status.code).isEqualTo(CsipStatus.REFERRAL_SUBMITTED.name)
    with(response.referral) {
      assertThat(isReferralComplete).isEqualTo(true)
      assertThat(referralCompletedDate).isEqualTo(LocalDate.now())
      assertThat(referralCompletedBy).isEqualTo(TEST_USER)
      assertThat(referralCompletedByDisplayName).isEqualTo(TEST_USER_NAME)
    }

    val saved = getCsipWithContributoryFactors(referral.id)
    with(saved) {
      assertThat(status?.code).isEqualTo(CsipStatus.REFERRAL_SUBMITTED.name)
      with(requireNotNull(saved.referral)) {
        assertThat(referralComplete).isEqualTo(true)
        assertThat(referralCompletedDate).isEqualTo(LocalDate.now())
        assertThat(referralCompletedBy).isEqualTo(TEST_USER)
        assertThat(referralCompletedByDisplayName).isEqualTo(TEST_USER_NAME)
        assertThat(contributoryFactors()).hasSize(4)
        assertThat(contributoryFactors().map { it.contributoryFactorType.code })
          .containsExactlyInAnyOrderElementsOf(listOf("MEN", "MED", "NRT", "SMO"))
      }
    }

    verifyAudit(referral, RevisionType.MOD, setOf(REFERRAL, CONTRIBUTORY_FACTOR))
    verifyDomainEvents(referral.csipRecord.prisonNumber, saved.id, CSIP_UPDATED)
  }

  private fun mergeReferralResponseSpec(
    uuid: UUID,
    request: MergeReferralRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.put()
    .uri("/csip-records/$uuid/referral")
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun mergeReferral(
    uuid: UUID,
    request: MergeReferralRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): CsipRecord = mergeReferralResponseSpec(uuid, request, username, role).successResponse()

  companion object {
    private const val INVALID = "is invalid"
    private const val NOT_ACTIVE = "is not active"

    @JvmStatic
    fun referenceDataValidation() = listOf(
      Arguments.of(
        mergeReferralRequest(incidentTypeCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, MergeReferralRequest::incidentTypeCode, INVALID),
      ),
      Arguments.of(
        mergeReferralRequest(incidentLocationCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_LOCATION, MergeReferralRequest::incidentLocationCode, INVALID),
      ),
      Arguments.of(
        mergeReferralRequest(refererAreaCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, MergeReferralRequest::refererAreaCode, INVALID),
      ),
      Arguments.of(
        mergeReferralRequest(incidentInvolvementCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_INVOLVEMENT, { it.incidentInvolvementCode!! }, INVALID),
      ),
      Arguments.of(
        mergeReferralRequest(incidentTypeCode = "IT_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, MergeReferralRequest::incidentTypeCode, NOT_ACTIVE),
      ),
      Arguments.of(
        mergeReferralRequest(incidentLocationCode = "IL_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_LOCATION, MergeReferralRequest::incidentLocationCode, NOT_ACTIVE),
      ),
      Arguments.of(
        mergeReferralRequest(refererAreaCode = "AOW_INACT"),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, MergeReferralRequest::refererAreaCode, NOT_ACTIVE),
      ),
      Arguments.of(
        mergeReferralRequest(incidentInvolvementCode = "II_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_INVOLVEMENT, { it.incidentInvolvementCode!! }, NOT_ACTIVE),
      ),
    )

    data class InvalidRd(
      val type: ReferenceDataType,
      val code: (MergeReferralRequest) -> String,
      val message: String,
    )

    private fun mergeReferralRequest(
      incidentDate: LocalDate = LocalDate.now(),
      incidentTime: LocalTime? = LocalTime.now(),
      incidentTypeCode: String = "WIT",
      incidentLocationCode: String = "REC",
      referredBy: String = "referredBy",
      refererAreaCode: String = "GYM",
      isProactiveReferral: Boolean? = true,
      isStaffAssaulted: Boolean? = false,
      assaultedStaffName: String? = null,
      incidentInvolvementCode: String? = null,
      descriptionOfConcern: String? = "descriptionOfConcern",
      knownReasons: String? = "knownReasons",
      otherInformation: String? = "otherInformation",
      isSaferCustodyTeamInformed: OptionalYesNoAnswer = DO_NOT_KNOW,
      isReferralComplete: Boolean? = true,
      contributoryFactors: List<MergeContributoryFactorRequest> = listOf(),
    ) = MergeReferralRequest(
      incidentDate,
      incidentTime,
      incidentTypeCode,
      incidentLocationCode,
      referredBy,
      refererAreaCode,
      isProactiveReferral,
      isStaffAssaulted,
      assaultedStaffName,
      incidentInvolvementCode,
      descriptionOfConcern,
      knownReasons,
      otherInformation,
      isSaferCustodyTeamInformed,
      isReferralComplete,
      contributoryFactors,
    )
  }

  private fun mergeContributoryFactorRequest(
    type: String = "BAS",
    comment: String? = "comment about the factor",
    id: UUID? = null,
  ) = MergeContributoryFactorRequest(type, comment, id)

  private fun getCsipWithContributoryFactors(id: UUID) = transactionTemplate.execute {
    val csip = csipRecordRepository.getCsipRecord(id)
    csip.referral!!.contributoryFactors()
    csip
  }!!
}
