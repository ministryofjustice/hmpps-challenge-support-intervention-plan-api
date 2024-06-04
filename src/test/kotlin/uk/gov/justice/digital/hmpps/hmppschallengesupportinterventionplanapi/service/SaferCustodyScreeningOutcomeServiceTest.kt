package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.CsipRecordNotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SaferCustodyScreeningOutcomeServiceTest {
  private val csipRecordRepository = mock<CsipRecordRepository>()
  private val referenceDataRepository = mock<ReferenceDataRepository>()
  private val underTest = SaferCustodyScreeningOutcomeService(csipRecordRepository, referenceDataRepository)

  @Test
  fun `create Screening Outcome`() {
    whenever(referenceDataRepository.findByDomainAndCode(any(), any())).thenReturn(referenceData())
    whenever(csipRecordRepository.findByRecordUuid(any())).thenReturn(csipRecord())
    whenever(csipRecordRepository.saveAndFlush(any())).thenReturn(
      csipRecord()
        .apply {
          setSaferCustodyScreeningOutcome(
            SaferCustodyScreeningOutcome(
              csipRecord = this,
              outcomeType = referenceData(),
              recordedBy = TEST_USER,
              recordedByDisplayName = TEST_USER_NAME,
              date = LocalDate.of(2021, 1, 1),
              reasonForDecision = "an",
            ),
          )
        },
    )

    val result = underTest.createScreeningOutcome(
      UUID.randomUUID(),
      CreateSaferCustodyScreeningOutcomeRequest(
        outcomeTypeCode = "CODE",
        date = LocalDate.of(2021, 1, 1),
        reasonForDecision = "an",
      ),
      requestContext(),
    )

    with(result) {
      assertThat(reasonForDecision).isEqualTo("an")
      assertThat(date).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(recordedBy).isEqualTo(TEST_USER)
      assertThat(recordedByDisplayName).isEqualTo(TEST_USER_NAME)
      assertThat(outcome.code).isEqualTo("CODE")
      assertThat(outcome.description).isEqualTo("Reference")
    }
  }

  @Test
  fun `create Screening Outcome with invalid OutcomeType code`() {
    val error = assertThrows<IllegalArgumentException> {
      underTest.createScreeningOutcome(
        UUID.randomUUID(),
        CreateSaferCustodyScreeningOutcomeRequest(
          outcomeTypeCode = "WRONG_CODE",
          date = LocalDate.of(2021, 1, 1),
          reasonForDecision = "an",
        ),
        requestContext(),
      )
    }

    assertThat(error.message).isEqualTo("OUTCOME_TYPE code 'WRONG_CODE' does not exist")
  }

  @Test
  fun `create Screening Outcome with invalid Csip Record UUID`() {
    val recordUuid = UUID.randomUUID()
    whenever(referenceDataRepository.findByDomainAndCode(any(), any())).thenReturn(referenceData())

    val error = assertThrows<CsipRecordNotFoundException> {
      underTest.createScreeningOutcome(
        recordUuid,
        CreateSaferCustodyScreeningOutcomeRequest(
          outcomeTypeCode = "WRONG_CODE",
          date = LocalDate.of(2021, 1, 1),
          reasonForDecision = "an",
        ),
        requestContext(),
      )
    }

    assertThat(error.message).isEqualTo("Could not find CSIP record with UUID $recordUuid")
  }

  private fun csipRecord() = CsipRecord(
    recordId = 5516,
    recordUuid = UUID.randomUUID(),
    prisonNumber = "quisque",
    prisonCodeWhenRecorded = null,
    logNumber = null,
    createdAt = LocalDateTime.now(),
    createdBy = "ornatus",
    createdByDisplayName = "Belinda Drake",
    lastModifiedAt = null,
    lastModifiedBy = null,
    lastModifiedByDisplayName = null,
  ).apply {
    setReferral(
      Referral(
        csipRecord = this,
        incidentDate = LocalDate.now(),
        incidentTime = null,
        referredBy = "falli",
        referralDate = LocalDate.now(),
        referralSummary = null,
        proactiveReferral = null,
        staffAssaulted = null,
        assaultedStaffName = null,
        releaseDate = null,
        descriptionOfConcern = "purus",
        knownReasons = "iuvaret",
        otherInformation = null,
        saferCustodyTeamInformed = null,
        referralComplete = null,
        referralCompletedBy = null,
        referralCompletedByDisplayName = null,
        referralCompletedDate = null,
        incidentType = referenceData(),
        incidentLocation = referenceData(),
        refererAreaOfWork = referenceData(),
        incidentInvolvement = referenceData(),
      ),
    )
  }

  private fun referenceData() = ReferenceData(
    referenceDataId = 1,
    domain = ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
    code = "CODE",
    description = "Reference",
    listSequence = 99,
    createdAt = LocalDateTime.now(),
    createdBy = "admin",
  )

  private fun requestContext() = CsipRequestContext(
    source = Source.DPS,
    username = TEST_USER,
    userDisplayName = TEST_USER_NAME,
    activeCaseLoadId = PRISON_CODE_LEEDS,
  )
}
