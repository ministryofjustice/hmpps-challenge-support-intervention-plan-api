package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.REFERENCE_DATA_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.verifyAllReferenceData
import java.time.LocalDate
import java.util.UUID

class InvestigationServiceTest : BaseServiceTest() {
  private val underTest = InvestigationService(csipRecordRepository, referenceDataRepository)

  @Test
  fun `create Investigation without interview`() {
    val createRequest = createRequest(withInterview = false)

    whenever(csipRecordRepository.findByRecordUuid(any())).thenReturn(csipRecord())
    whenever(auditEventRepository.save(any())).thenAnswer { it.arguments[0] }
    whenever(csipRecordRepository.save(any())).thenReturn(
      csipRecord().apply {
        referral!!.createInvestigation(
          CsipRequestContext(username = TEST_USER, userDisplayName = TEST_USER_NAME),
          request = createRequest,
        ) { codes -> referenceDataRepository.verifyAllReferenceData(ReferenceDataType.INTERVIEWEE_ROLE, codes) }
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
