package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.hibernate.envers.RevisionType
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.csipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.RECORD
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipComponent.REFERRAL
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.CsipStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_CODE
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.UUID

class UpdateCsipRecordsIntTest : IntegrationTestBase() {

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = ["WRONG_ROLE"])
  fun `403 forbidden - no required role`(role: String?) {
    val response = updateCsipRecordResponseSpec(UUID.randomUUID(), updateCsipRecordRequest(), role = role)
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
  fun `401 unauthorised`() {
    webTestClient.post().uri("/csip-records/${UUID.randomUUID()}").exchange().expectStatus().isUnauthorized
  }

  @Test
  fun `400 bad request - username not found`() {
    val response = updateCsipRecordResponseSpec(UUID.randomUUID(), updateCsipRecordRequest(), username = USER_NOT_FOUND)
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
    val response = updateCsipRecordResponseSpec(uuid, updateCsipRecordRequest())
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
    updateReferral: UpdateReferral,
    invalid: InvalidRd,
  ) {
    val prisonNumber = givenValidPrisonNumber("R1234VC")
    val record = givenCsipRecord(generateCsipRecord(prisonNumber).withReferral())

    val request = updateCsipRecordRequest(logCode = null, referral = updateReferral)
    val response = updateCsipRecordResponseSpec(record.id, request).errorResponse(HttpStatus.BAD_REQUEST)
    with(response) {
      assertThat(status).isEqualTo(400)
      assertThat(errorCode).isNull()
      assertThat(userMessage).isEqualTo("Validation failure: ${invalid.type} ${invalid.message}")
      assertThat(developerMessage).isEqualTo("Details => ${invalid.type}:${invalid.code(updateReferral)}")
      assertThat(moreInfo).isNull()
    }
  }

  @Test
  fun `200 ok - CSIP record not updated does not create audit record`() {
    val prisonNumber = givenValidPrisonNumber("U1234NC")
    val record = dataSetup(generateCsipRecord(prisonNumber, logCode = LOG_CODE).withReferral()) { it }

    val request = updateCsipRecordRequest(logCode = LOG_CODE)
    val response = updateCsipRecord(record.id, request)
    with(response) {
      assertThat(logCode).isEqualTo(LOG_CODE)
    }

    val saved = csipRecordRepository.getCsipRecord(record.id)
    with(saved) {
      assertThat(logCode).isEqualTo(LOG_CODE)
      assertThat(lastModifiedAt).isNull()
      assertThat(lastModifiedBy).isNull()
      assertThat(lastModifiedByDisplayName).isNull()
    }

    // verify the latest audit record is the initial insert from the given of the test
    verifyAudit(
      saved,
      RevisionType.ADD,
      setOf(RECORD, REFERRAL),
      csipRequestContext(),
    )

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - CSIP record updated log code with source DPS`() {
    val prisonNumber = givenValidPrisonNumber("U1234DR")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }

    val request = updateCsipRecordRequest()
    val response = updateCsipRecord(record.id, request)
    with(response) {
      assertThat(logCode).isEqualTo(request.logCode)
    }

    val saved = csipRecordRepository.getCsipRecord(record.id)
    with(saved) {
      assertThat(logCode).isEqualTo(request.logCode)
      assertThat(lastModifiedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(lastModifiedBy).isEqualTo(TEST_USER)
      assertThat(lastModifiedByDisplayName).isEqualTo(TEST_USER_NAME)
    }

    verifyAudit(
      saved,
      RevisionType.MOD,
      setOf(RECORD),
    )

    verifyDomainEvent(prisonNumber, saved.id, setOf(RECORD))
  }

  @Test
  fun `200 ok - CSIP record updated referral`() {
    val prisonNumber = givenValidPrisonNumber("U1235DR")
    val incidentType = givenReferenceData(ReferenceDataType.INCIDENT_TYPE, "ATO")
    val incidentLocation = givenReferenceData(ReferenceDataType.INCIDENT_LOCATION, "KIT")
    val refererArea = givenReferenceData(ReferenceDataType.AREA_OF_WORK, "HEA")

    val record = dataSetup(generateCsipRecord(prisonNumber)) {
      it.withReferral(
        incidentType = { incidentType },
        incidentLocation = { incidentLocation },
        refererAreaOfWork = { refererArea },
      )
    }

    val request = updateCsipRecordRequest(logCode = null, referral = updateReferral(descriptionOfConcern = "Concern"))
    val response = updateCsipRecord(record.id, request)
    with(response) {
      assertThat(logCode).isNull()
      assertThat(status).isEqualTo(CsipStatus.REFERRAL_PENDING)
    }

    val saved = csipRecordRepository.getCsipRecord(record.id)
    with(saved) {
      assertThat(logCode).isNull()
      val referral = requireNotNull(saved.referral)
      assertThat(referral.lastModifiedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(referral.lastModifiedBy).isEqualTo(TEST_USER)
      assertThat(referral.lastModifiedByDisplayName).isEqualTo(TEST_USER_NAME)
    }

    verifyAudit(
      saved.referral!!,
      RevisionType.MOD,
      setOf(REFERRAL),
    )

    verifyDomainEvent(prisonNumber, saved.id, setOf(REFERRAL))
  }

  @Test
  fun `200 ok - CSIP record updates csip and completes referral`() {
    val prisonNumber = givenValidPrisonNumber("U5463BT")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withReferral() }
    val referral = requireNotNull(record.referral)

    val request = updateCsipRecordRequest(
      logCode = LOG_CODE,
      referral = updateReferral(
        incidentTypeCode = referral.incidentType.code,
        incidentLocationCode = referral.incidentLocation.code,
        refererAreaCode = referral.refererAreaOfWork.code,
        incidentInvolvementCode = referral.incidentInvolvement?.code,
        descriptionOfConcern = "Updated concerns",
        knownReasons = "Updated reasons",
        otherInformation = "Even more information that can change",
        isReferralComplete = true,
      ),
    )

    updateCsipRecord(record.id, request)
    val saved = csipRecordRepository.getCsipRecord(record.id)
    with(saved) {
      assertThat(logCode).isEqualTo(request.logCode)
      assertThat(status).isEqualTo(CsipStatus.REFERRAL_SUBMITTED)
    }

    verifyAudit(
      record,
      RevisionType.MOD,
      setOf(RECORD, REFERRAL),
    )

    verifyDomainEvent(prisonNumber, saved.id, setOf(RECORD, REFERRAL))
  }

  @Test
  fun `200 ok - Undo referral complete`() {
    val prisonNumber = givenValidPrisonNumber("U1234UC")
    val record = dataSetup(generateCsipRecord(prisonNumber)) { it.withCompletedReferral() }
    val referral = requireNotNull(record.referral)

    val request = updateCsipRecordRequest(
      logCode = null,
      referral = updateReferral(
        incidentTypeCode = referral.incidentType.code,
        incidentLocationCode = referral.incidentLocation.code,
        refererAreaCode = referral.refererAreaOfWork.code,
        incidentInvolvementCode = referral.incidentInvolvement?.code,
        isReferralComplete = false,
      ),
    )

    updateCsipRecord(record.id, request)
    val saved = csipRecordRepository.getCsipRecord(record.id)

    verifyAudit(
      saved.referral!!,
      RevisionType.MOD,
      setOf(REFERRAL),
    )

    verifyDomainEvent(prisonNumber, saved.id, setOf(REFERRAL))
  }

  private fun updateCsipRecordResponseSpec(
    uuid: UUID,
    request: UpdateCsipRecordRequest,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch()
    .uri("/csip-records/$uuid")
    .bodyValue(request)
    .headers(setAuthorisation(user = username, roles = listOfNotNull(role)))
    .exchange()

  private fun updateCsipRecord(
    uuid: UUID,
    request: UpdateCsipRecordRequest,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): CsipRecord = updateCsipRecordResponseSpec(uuid, request, username, role).successResponse()

  companion object {
    private const val INVALID = "is invalid"
    private const val NOT_ACTIVE = "is not active"

    @JvmStatic
    fun referenceDataValidation() = listOf(
      Arguments.of(
        updateReferral(incidentTypeCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, UpdateReferral::incidentTypeCode, INVALID),
      ),
      Arguments.of(
        updateReferral(incidentLocationCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_LOCATION, UpdateReferral::incidentLocationCode, INVALID),
      ),
      Arguments.of(
        updateReferral(refererAreaCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, UpdateReferral::refererAreaCode, INVALID),
      ),
      Arguments.of(
        updateReferral(incidentInvolvementCode = "NONEXISTENT"),
        InvalidRd(ReferenceDataType.INCIDENT_INVOLVEMENT, { it.incidentInvolvementCode!! }, INVALID),
      ),
      Arguments.of(
        updateReferral(incidentTypeCode = "IT_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_TYPE, UpdateReferral::incidentTypeCode, NOT_ACTIVE),
      ),
      Arguments.of(
        updateReferral(incidentLocationCode = "IL_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_LOCATION, UpdateReferral::incidentLocationCode, NOT_ACTIVE),
      ),
      Arguments.of(
        updateReferral(refererAreaCode = "AOW_INACT"),
        InvalidRd(ReferenceDataType.AREA_OF_WORK, UpdateReferral::refererAreaCode, NOT_ACTIVE),
      ),
      Arguments.of(
        updateReferral(incidentInvolvementCode = "II_INACT"),
        InvalidRd(ReferenceDataType.INCIDENT_INVOLVEMENT, { it.incidentInvolvementCode!! }, NOT_ACTIVE),
      ),
    )

    data class InvalidRd(
      val type: ReferenceDataType,
      val code: (UpdateReferral) -> String,
      val message: String,
    )

    private fun updateCsipRecordRequest(
      logCode: String? = LOG_CODE,
      referral: UpdateReferral? = null,
    ) = UpdateCsipRecordRequest(logCode, referral)

    private fun updateReferral(
      incidentDate: LocalDate = LocalDate.now(),
      incidentTime: LocalTime? = null,
      incidentTypeCode: String = "WIT",
      incidentLocationCode: String = "REC",
      referredBy: String = "referredBy",
      refererAreaCode: String = "GYM",
      isProactiveReferral: Boolean? = null,
      isStaffAssaulted: Boolean? = null,
      assaultedStaffName: String? = null,
      incidentInvolvementCode: String? = null,
      descriptionOfConcern: String? = "descriptionOfConcern",
      knownReasons: String? = "knownReasons",
      otherInformation: String? = "otherInformation",
      isSaferCustodyTeamInformed: OptionalYesNoAnswer = DO_NOT_KNOW,
      isReferralComplete: Boolean? = null,
    ) = UpdateReferral(
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
      isReferralComplete?.let { if (it) LocalDate.now() else null },
      isReferralComplete?.let { if (it) "completedBy" else null },
      isReferralComplete?.let { if (it) "completedByDisplayName" else null },
    )
  }

  private fun verifyDomainEvent(
    prisonNumber: String,
    recordUuid: UUID,
    affectedComponents: Set<CsipComponent>,
    source: Source = DPS,
  ) {
    verifyDomainEvents(
      prisonNumber,
      recordUuid,
      affectedComponents,
      setOf(DomainEventType.CSIP_UPDATED),
      source = source,
    )
  }
}
