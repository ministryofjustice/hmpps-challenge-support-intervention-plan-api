package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.REFERENCE_DATA_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.NotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import java.time.LocalDate
import java.util.UUID

class InvestigationServiceTest : BaseServiceTest() {
  private val underTest = InvestigationService(csipRecordRepository, referenceDataRepository)

  @Test
  fun `create Investigation without interview`() {
    val createRequest = createRequest(withInterview = false)

    whenever(csipRecordRepository.findByRecordUuid(any())).thenReturn(csipRecord())
    whenever(csipRecordRepository.save(any())).thenReturn(
      csipRecord().apply {
        referral!!.createInvestigation(
          CsipRequestContext(username = TEST_USER, userDisplayName = TEST_USER_NAME),
          createRequest = createRequest,
          intervieweeRoleMap = emptyMap(),
          activeCaseLoadId = PRISON_CODE_LEEDS,
        )
      },
    )

    val result = underTest.createInvestigation(
      UUID.randomUUID(),
      createRequest,
      requestContext(),
    )

    with(result) {
      assertThat(staffInvolved).isEqualTo("staffInvolved")
      assertThat(evidenceSecured).isEqualTo("evidenceSecured")
      assertThat(occurrenceReason).isEqualTo("occurrenceReason")
      assertThat(personsUsualBehaviour).isEqualTo("personsUsualBehaviour")
      assertThat(personsTrigger).isEqualTo("personsTrigger")
      assertThat(protectiveFactors).isEqualTo("protectiveFactors")
      assertThat(interviews).isEmpty()
    }
  }

  @Test
  fun `create Investigation with invalid CSIP UUID`() {
    val recordUuid = UUID.randomUUID()
    val createRequest = createRequest(withInterview = false)

    val error = assertThrows<NotFoundException> {
      underTest.createInvestigation(
        recordUuid,
        createRequest,
        requestContext(),
      )
    }

    assertThat(error.message).isEqualTo("CSIP Record not found")
  }

  @Test
  fun `create Investigation with invalid Interviewee Role code`() {
    val createRequest = createRequest()

    val error = assertThrows<IllegalArgumentException> {
      underTest.createInvestigation(
        UUID.randomUUID(),
        createRequest,
        requestContext(),
      )
    }

    assertThat(error.message).isEqualTo("INTERVIEWEE_ROLE is invalid")
  }

  private fun createRequest(withInterview: Boolean = true) = CreateInvestigationRequest(
    staffInvolved = "staffInvolved",
    evidenceSecured = "evidenceSecured",
    occurrenceReason = "occurrenceReason",
    personsUsualBehaviour = "personsUsualBehaviour",
    personsTrigger = "personsTrigger",
    protectiveFactors = "protectiveFactors",
    interviews = withInterview.takeIf { it }?.let {
      listOf(
        CreateInterviewRequest(
          interviewee = "gloriatur",
          interviewDate = LocalDate.of(2021, 1, 1),
          intervieweeRoleCode = REFERENCE_DATA_CODE,
          interviewText = "text",
        ),
      )
    },
  )
}
