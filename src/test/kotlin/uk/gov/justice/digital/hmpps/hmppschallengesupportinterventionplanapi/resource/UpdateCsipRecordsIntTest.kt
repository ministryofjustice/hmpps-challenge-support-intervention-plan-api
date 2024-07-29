package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.awaitility.kotlin.withPollDelay
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.junit.jupiter.params.provider.NullSource
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.EuropeLondon
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_CSIP_UI
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.ROLE_NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.CsipDomainEvent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event.PersonReference
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AffectedComponent
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DomainEventType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.DPS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source.NOMIS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.USER_NOT_FOUND
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpdateReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.getCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.withReferral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LOG_CODE
import java.time.Duration.ofSeconds
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
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
  fun `400 bad request - invalid source`() {
    val response = webTestClient.patch().uri("/csip-records/${UUID.randomUUID()}")
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
    val response = updateCsipRecordResponseSpec(UUID.randomUUID(), updateCsipRecordRequest(), username = null)
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
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))

    val request = updateCsipRecordRequest(logCode = null, referral = updateReferral)
    val response = updateCsipRecordResponseSpec(record.recordUuid, request).errorResponse(HttpStatus.BAD_REQUEST)
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
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber, logCode = LOG_CODE))

    val request = updateCsipRecordRequest(logCode = LOG_CODE)
    val response = updateCsipRecord(record.recordUuid, request)
    with(response) {
      assertThat(logCode).isEqualTo(LOG_CODE)
    }

    val saved = csipRecordRepository.getCsipRecord(record.recordUuid)
    with(saved) {
      assertThat(logCode).isEqualTo(LOG_CODE)
    }
    val audit = auditEventRepository.findAll().singleOrNull { it.action == AuditEventAction.UPDATED }
    assertThat(audit).isNull()

    await withPollDelay ofSeconds(1) untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 0 }
  }

  @Test
  fun `200 ok - CSIP record updated log code with source DPS`() {
    val prisonNumber = givenValidPrisonNumber("U1234DR")
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))

    val request = updateCsipRecordRequest()
    val response = updateCsipRecord(record.recordUuid, request)
    with(response) {
      assertThat(logCode).isEqualTo(request.logCode)
    }

    val saved = csipRecordRepository.getCsipRecord(record.recordUuid)
    with(saved) {
      assertThat(logCode).isEqualTo(request.logCode)
      assertThat(lastModifiedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(lastModifiedBy).isEqualTo(TEST_USER)
      assertThat(lastModifiedByDisplayName).isEqualTo(TEST_USER_NAME)
    }

    val audit = auditEventRepository.findAll().single { it.action == AuditEventAction.UPDATED }
    assertThat(audit.description).isEqualTo("Updated CSIP record logCode changed from null to 'ZXY987'")
    assertThat(audit.affectedComponents).containsExactly(AffectedComponent.Record)
    assertThat(audit.source).isEqualTo(DPS)
    assertThat(audit.actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(audit.actionedBy).isEqualTo(TEST_USER)

    verifyDomainEvent(prisonNumber, saved.recordUuid, arrayOf(AffectedComponent.Record))
  }

  @Test
  fun `200 ok - CSIP record updated log code with source NOMIS`() {
    val prisonNumber = givenValidPrisonNumber("U2234NR")
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))

    val request = updateCsipRecordRequest()
    val response = updateCsipRecord(record.recordUuid, request, NOMIS, NOMIS_SYS_USER, ROLE_NOMIS)
    with(response) {
      assertThat(logCode).isEqualTo(request.logCode)
    }

    val saved = csipRecordRepository.getCsipRecord(record.recordUuid)
    with(saved) {
      assertThat(logCode).isEqualTo(request.logCode)
      assertThat(lastModifiedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(lastModifiedBy).isEqualTo(NOMIS_SYS_USER)
      assertThat(lastModifiedByDisplayName).isEqualTo(NOMIS_SYS_USER_DISPLAY_NAME)
    }

    val audit = auditEventRepository.findAll().single { it.action == AuditEventAction.UPDATED }
    assertThat(audit.description).isEqualTo("Updated CSIP record logCode changed from null to 'ZXY987'")
    assertThat(audit.affectedComponents).containsExactly(AffectedComponent.Record)
    assertThat(audit.source).isEqualTo(NOMIS)
    assertThat(audit.actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(audit.actionedBy).isEqualTo(NOMIS_SYS_USER)

    verifyDomainEvent(prisonNumber, saved.recordUuid, arrayOf(AffectedComponent.Record), NOMIS)
  }

  @Test
  fun `200 ok - CSIP record updated referral`() {
    val prisonNumber = givenValidPrisonNumber("U1235DR")
    val incidentType = givenReferenceData(ReferenceDataType.INCIDENT_TYPE, "ATO")
    val incidentLocation = givenReferenceData(ReferenceDataType.INCIDENT_LOCATION, "KIT")
    val refererArea = givenReferenceData(ReferenceDataType.AREA_OF_WORK, "HEA")

    val record = givenCsipRecord(
      generateCsipRecord(prisonNumber).withReferral(
        incidentType = { incidentType },
        incidentLocation = { incidentLocation },
        refererAreaOfWork = { refererArea },
      ),
    )

    val request = updateCsipRecordRequest(logCode = null, referral = updateReferral())
    val response = updateCsipRecord(record.recordUuid, request)
    with(response) {
      assertThat(logCode).isNull()
    }

    val saved = csipRecordRepository.getCsipRecord(record.recordUuid)
    with(saved) {
      assertThat(logCode).isNull()
      assertThat(lastModifiedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
      assertThat(lastModifiedBy).isEqualTo(TEST_USER)
      assertThat(lastModifiedByDisplayName).isEqualTo(TEST_USER_NAME)
    }

    val audit = auditEventRepository.findAll().single { it.action == AuditEventAction.UPDATED }
    assertThat(audit.description).isEqualTo(
      "Updated referral incidentType changed from 'ATO' to 'WIT', incidentLocation changed from 'KIT' to 'REC', refererAreaOfWork changed from 'HEA' to 'GYM'",
    )
    assertThat(audit.affectedComponents).containsExactly(AffectedComponent.Referral)
    assertThat(audit.source).isEqualTo(DPS)
    assertThat(audit.actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(audit.actionedBy).isEqualTo(TEST_USER)

    verifyDomainEvent(prisonNumber, saved.recordUuid, arrayOf(AffectedComponent.Referral))
  }

  @Test
  fun `200 ok - CSIP record updates csip and completes referral`() {
    val prisonNumber = givenValidPrisonNumber("U5463BT")
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber))
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

    updateCsipRecord(record.recordUuid, request)
    val saved = csipRecordRepository.getCsipRecord(record.recordUuid)
    with(saved) {
      assertThat(logCode).isEqualTo(request.logCode)
    }

    val audit = auditEventRepository.findAll().single { it.action == AuditEventAction.UPDATED }
    assertThat(audit.description).isEqualTo(
      "Updated CSIP record logCode changed from null to 'ZXY987' and updated referral descriptionOfConcern changed from 'descriptionOfConcern' to 'Updated concerns', " +
        "knownReasons changed from 'knownReasons' to 'Updated reasons', otherInformation changed from 'otherInformation' to 'Even more information that can change', " +
        "referralComplete changed from false to true, " +
        "referralCompletedDate changed from null to '${
          LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        }', referralCompletedBy changed from null to 'completedBy', " +
        "referralCompletedByDisplayName changed from null to 'completedByDisplayName'",
    )
    assertThat(audit.affectedComponents).containsExactlyInAnyOrder(
      AffectedComponent.Record,
      AffectedComponent.Referral,
    )
    assertThat(audit.source).isEqualTo(DPS)
    assertThat(audit.actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(audit.actionedBy).isEqualTo(TEST_USER)

    verifyDomainEvent(prisonNumber, saved.recordUuid, arrayOf(AffectedComponent.Record, AffectedComponent.Referral))
  }

  @Test
  fun `200 ok - Undo referral complete`() {
    val prisonNumber = givenValidPrisonNumber("U1234UC")
    val record = givenCsipRecordWithReferral(generateCsipRecord(prisonNumber), complete = true)
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

    updateCsipRecord(record.recordUuid, request)
    val saved = csipRecordRepository.getCsipRecord(record.recordUuid)
    val audit = auditEventRepository.findAll().single { it.action == AuditEventAction.UPDATED }
    assertThat(audit.description).isEqualTo(
      "Updated referral referralComplete changed from true to false, referralCompletedDate changed from '${
        LocalDate.now().minusDays(1).format(DateTimeFormatter.ISO_LOCAL_DATE)
      }' to null, " +
        "referralCompletedBy changed from 'referralCompletedBy' to null, " +
        "referralCompletedByDisplayName changed from 'referralCompletedByDisplayName' to null",
    )
    assertThat(audit.affectedComponents).containsExactlyInAnyOrder(AffectedComponent.Referral)
    assertThat(audit.source).isEqualTo(DPS)
    assertThat(audit.actionedAt).isCloseTo(LocalDateTime.now(), within(3, ChronoUnit.SECONDS))
    assertThat(audit.actionedBy).isEqualTo(TEST_USER)

    verifyDomainEvent(prisonNumber, saved.recordUuid, arrayOf(AffectedComponent.Referral))
  }

  private fun updateCsipRecordResponseSpec(
    uuid: UUID,
    request: UpdateCsipRecordRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String? = ROLE_CSIP_UI,
  ) = webTestClient.patch()
    .uri("/csip-records/$uuid")
    .bodyValue(request)
    .headers(setAuthorisation(roles = listOfNotNull(role)))
    .headers(setCsipRequestContext(source = source, username = username))
    .exchange()

  private fun updateCsipRecord(
    uuid: UUID,
    request: UpdateCsipRecordRequest,
    source: Source = DPS,
    username: String? = TEST_USER,
    role: String = ROLE_CSIP_UI,
  ): CsipRecord = updateCsipRecordResponseSpec(uuid, request, source, username, role).successResponse()

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
    affectedComponents: Array<AffectedComponent>,
    source: Source = DPS,
  ) {
    await untilCallTo { hmppsEventsQueue.countAllMessagesOnQueue() } matches { it == 1 }
    val events = hmppsEventsQueue.receiveDomainEventsOnQueue()
    val csipUpdatedEvent = events.filterIsInstance<CsipDomainEvent>().single()
    with(csipUpdatedEvent) {
      assertThat(eventType).isEqualTo(DomainEventType.CSIP_UPDATED.eventType)
      with(additionalInformation) {
        assertThat(this.recordUuid).isEqualTo(recordUuid)
        assertThat(this.affectedComponents).containsExactlyInAnyOrder(*affectedComponents)
        assertThat(this.source).isEqualTo(source)
      }
      assertThat(description).isEqualTo(DomainEventType.CSIP_UPDATED.description)
      assertThat(occurredAt).isCloseTo(
        ZonedDateTime.now().withZoneSameInstant(EuropeLondon),
        within(3, ChronoUnit.SECONDS),
      )
      assertThat(detailUrl).isEqualTo("http://localhost:8080/csip-records/$recordUuid")
      assertThat(personReference).isEqualTo(PersonReference.withPrisonNumber(prisonNumber))
    }
  }
}
