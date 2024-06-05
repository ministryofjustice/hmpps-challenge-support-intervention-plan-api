package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.CsipRecordNotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

class SaferCustodyScreeningOutcomeServiceTest : BaseServiceTest() {
  private val underTest = SaferCustodyScreeningOutcomeService(csipRecordRepository, referenceDataRepository)

  @Test
  fun `create Screening Outcome`() {
    whenever(referenceDataRepository.findByDomainAndCode(any(), any())).thenReturn(referenceData())
    whenever(csipRecordRepository.findByRecordUuid(any())).thenReturn(csipRecord())
    whenever(csipRecordRepository.saveAndFlush(any())).thenReturn(
      csipRecord()
        .apply {
          referral!!.createSaferCustodyScreeningOutcome(
            outcomeType = referenceData(),
            date = LocalDate.of(2021, 1, 1),
            reasonForDecision = "an",
            actionedAt = LocalDateTime.of(2021, 1, 1, 1, 0),
            actionedBy = TEST_USER,
            actionedByDisplayName = TEST_USER_NAME,
            source = Source.DPS,
            activeCaseLoadId = PRISON_CODE_LEEDS,
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
}
