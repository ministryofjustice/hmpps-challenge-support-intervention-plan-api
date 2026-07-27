package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.INVESTIGATION_REQUIRED
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataKey
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referencedata.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.AREA_OF_WORK
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.SCREENING_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpsertSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.set
import java.time.LocalDate

class SaferCustodyScreeningOutcomeServiceTest {

  private val csipRecordRepository = mock<CsipRecordRepository>()
  private val referenceDataRepository = mock<ReferenceDataRepository>()
  private val jdaService = mock<JdaService>()

  private val service = SaferCustodyScreeningOutcomeService(
    csipRecordRepository,
    referenceDataRepository,
    jdaService,
  )

  private val prisonNumber = "A1234AA"
  private val prisonCode = "NMI"

  private lateinit var record: CsipRecord

  @BeforeEach
  fun setUp() {
    record = generateCsipRecord(
      personSummary = prisoner(prisonerNumber = prisonNumber).toPersonSummary(),
      prisonCodeWhenRecorded = prisonCode,
    )

    val referral = Referral(
      csipRecord = record,
      referralDate = LocalDate.now(),
      incidentDate = LocalDate.now(),
      referredBy = "referredBy",
      descriptionOfConcern = null,
      knownReasons = null,
      saferCustodyTeamInformed = DO_NOT_KNOW,
      incidentType = referenceData(INCIDENT_TYPE, "INT"),
      incidentLocation = referenceData(INCIDENT_LOCATION, "LOC"),
      refererAreaOfWork = referenceData(AREA_OF_WORK, "AOW"),
      incidentInvolvement = null,
    )

    record.set(record::referral, referral)

    whenever(csipRecordRepository.findById(record.id)).thenReturn(record)
  }

  @Test
  fun `should submit case notes for analysis when outcome is investigation required`() {
    whenever(
      referenceDataRepository.findByKey(ReferenceDataKey(SCREENING_OUTCOME_TYPE, INVESTIGATION_REQUIRED)),
    ).thenReturn(referenceData(SCREENING_OUTCOME_TYPE, INVESTIGATION_REQUIRED))

    service.upsertScreeningOutcome(
      record.id,
      upsertScreeningOutcomeRequest(outcomeTypeCode = INVESTIGATION_REQUIRED),
    )

    val offenderIdentifierCaptor = argumentCaptor<String>()
    val prisonCodeCaptor = argumentCaptor<String>()
    val correlationIdCaptor = argumentCaptor<String>()

    verify(jdaService).submitCaseNotesForAnalysis(
      offenderIdentifier = offenderIdentifierCaptor.capture(),
      prisonCode = prisonCodeCaptor.capture(),
      correlationId = correlationIdCaptor.capture(),
    )

    assertThat(offenderIdentifierCaptor.firstValue)
      .isEqualTo(prisonNumber)

    assertThat(prisonCodeCaptor.firstValue)
      .isEqualTo(prisonCode)

    assertThat(correlationIdCaptor.firstValue)
      .isEqualTo(record.referral!!.id.toString())
  }

  @Test
  fun `should not submit case notes for analysis when outcome is not investigation required`() {
    whenever(
      referenceDataRepository.findByKey(ReferenceDataKey(SCREENING_OUTCOME_TYPE, "CUR")),
    ).thenReturn(referenceData(SCREENING_OUTCOME_TYPE, "CUR"))

    service.upsertScreeningOutcome(
      record.referral!!.id,
      upsertScreeningOutcomeRequest(outcomeTypeCode = "CUR"),
    )

    verifyNoInteractions(jdaService)
  }

  @Test
  fun `should not submit case notes for analysis when prison code when recorded is null`() {
    val recordWithNoPrisonCode = generateCsipRecord(
      personSummary = prisoner(prisonerNumber = prisonNumber).toPersonSummary(),
      prisonCodeWhenRecorded = null,
    )

    val referral = Referral(
      csipRecord = recordWithNoPrisonCode,
      referralDate = LocalDate.now(),
      incidentDate = LocalDate.now(),
      referredBy = "referredBy",
      descriptionOfConcern = null,
      knownReasons = null,
      saferCustodyTeamInformed = DO_NOT_KNOW,
      incidentType = referenceData(INCIDENT_TYPE, "INT"),
      incidentLocation = referenceData(INCIDENT_LOCATION, "LOC"),
      refererAreaOfWork = referenceData(AREA_OF_WORK, "AOW"),
      incidentInvolvement = null,
    )

    recordWithNoPrisonCode.set(recordWithNoPrisonCode::referral, referral)

    whenever(csipRecordRepository.findById(recordWithNoPrisonCode.id)).thenReturn(recordWithNoPrisonCode)
    whenever(
      referenceDataRepository.findByKey(ReferenceDataKey(SCREENING_OUTCOME_TYPE, INVESTIGATION_REQUIRED)),
    ).thenReturn(referenceData(SCREENING_OUTCOME_TYPE, INVESTIGATION_REQUIRED))

    service.upsertScreeningOutcome(
      recordWithNoPrisonCode.id,
      upsertScreeningOutcomeRequest(outcomeTypeCode = INVESTIGATION_REQUIRED),
    )

    verifyNoInteractions(jdaService)
  }

  private fun upsertScreeningOutcomeRequest(
    outcomeTypeCode: String = INVESTIGATION_REQUIRED,
  ) = UpsertSaferCustodyScreeningOutcomeRequest(
    outcomeTypeCode = outcomeTypeCode,
    date = LocalDate.now(),
    reasonForDecision = "A reason for the decision",
    recordedBy = "recordedBy",
    recordedByDisplayName = "Recorded By",
  )

  private fun referenceData(domain: ReferenceDataType, code: String) = ReferenceData(
    key = ReferenceDataKey(domain, code),
    description = code,
    listSequence = 1,
    deactivatedAt = null,
  )
}
