package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.CsipRecordNotFoundException
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateDecisionAndActionsRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.*

class DecisionActionsServiceTest {
  private val csipRecordRepository = mock<CsipRecordRepository>()
  private val referenceDataRepository = mock<ReferenceDataRepository>()
  private val underTest = DecisionActionsService(csipRecordRepository, referenceDataRepository)

  @Test
  fun `create Screening Outcome`() {
    whenever(referenceDataRepository.findByDomainAndCode(any(), eq("CODE"))).thenReturn(referenceDataDecisionOutcome())
    whenever(referenceDataRepository.findByDomainAndCode(any(), eq("SIGNEDOFF"))).thenReturn(referenceDataDecisionOutcomeSignedOffBy())
    whenever(csipRecordRepository.findByRecordUuid(any())).thenReturn(csipRecord())
    whenever(csipRecordRepository.saveAndFlush(any())).thenReturn(
      csipRecord()
        .apply {
          referral!!.createDecisionAndActions(
            decisionOutcome = referenceDataDecisionOutcome(),
            decisionOutcomeSignedOffBy = referenceDataDecisionOutcomeSignedOffBy(),
            decisionConclusion = "yes",
            decisionOutcomeRecordedBy = TEST_USER,
            decisionOutcomeRecordedByDisplayName = TEST_USER_NAME,
            decisionOutcomeDate = LocalDate.of(2021, 1, 1),
            nextSteps = "next",
            actionOpenCsipAlert = true,
            actionNonAssociationsUpdated = true,
            actionObservationBook = true,
            actionUnitOrCellMove = true,
            actionCsraOrRsraReview = true,
            actionServiceReferral = true,
            actionSimReferral = true,
            actionOther = "other",
            actionedAt = LocalDateTime.of(2021, 1, 1, 0, 0, 0),
            source = Source.DPS,
            activeCaseLoadId = PRISON_CODE_LEEDS,
          )
        },
    )

    val result = underTest.createDecisionAndActionsRequest(
      UUID.randomUUID(),
      CreateDecisionAndActionsRequest(
        outcomeTypeCode = "CODE",
        outcomeSignedOffByRoleCode = "SIGNEDOFF",
        conclusion = "yes",
        outcomeRecordedBy = null,
        outcomeRecordedByDisplayName = null,
        outcomeDate = LocalDate.of(2021, 1, 1),
        nextSteps = "next",
        isActionOpenCsipAlert = true,
        isActionNonAssociationsUpdated = true,
        isActionObservationBook = true,
        isActionUnitOrCellMove = true,
        isActionCsraOrRsraReview = true,
        isActionServiceReferral = true,
        isActionSimReferral = true,
        actionOther = "other",
      ),
      requestContext(),
    )

    with(result) {
      assertThat(outcome.code).isEqualTo("CODE")
      assertThat(outcomeSignedOffByRole?.code).isEqualTo("SIGNEDOFF")
      assertThat(conclusion).isEqualTo("yes")
      assertThat(outcomeRecordedBy).isEqualTo(TEST_USER)
      assertThat(outcomeRecordedByDisplayName).isEqualTo(TEST_USER_NAME)
      assertThat(outcomeDate).isEqualTo(LocalDate.of(2021, 1, 1))
      assertThat(nextSteps).isEqualTo("next")
      assertThat(isActionOpenCsipAlert).isEqualTo(true)
      assertThat(isActionNonAssociationsUpdated).isEqualTo(true)
      assertThat(isActionObservationBook).isEqualTo(true)
      assertThat(isActionUnitOrCellMove).isEqualTo(true)
      assertThat(isActionCsraOrRsraReview).isEqualTo(true)
      assertThat(isActionServiceReferral).isEqualTo(true)
      assertThat(isActionSimReferral).isEqualTo(true)
      assertThat(actionOther).isEqualTo("other")
    }
  }

  @Test
  fun `create Screening Outcome with invalid OutcomeType code`() {
    whenever(referenceDataRepository.findByDomainAndCode(any(), eq("SIGNEDOFF"))).thenReturn(referenceDataDecisionOutcomeSignedOffBy())
    val error = assertThrows<IllegalArgumentException> {
      underTest.createDecisionAndActionsRequest(
        UUID.randomUUID(),
        CreateDecisionAndActionsRequest(
          outcomeTypeCode = "WRONG_CODE",
          outcomeSignedOffByRoleCode = "SIGNEDOFF",
          conclusion = "yes",
          outcomeRecordedBy = null,
          outcomeRecordedByDisplayName = null,
          outcomeDate = LocalDate.of(2021, 1, 1),
          nextSteps = "next",
          isActionOpenCsipAlert = true,
          isActionNonAssociationsUpdated = true,
          isActionObservationBook = true,
          isActionUnitOrCellMove = true,
          isActionCsraOrRsraReview = true,
          isActionServiceReferral = true,
          isActionSimReferral = true,
          actionOther = "other",
        ),
        requestContext(),
      )
    }

    assertThat(error.message).isEqualTo("OUTCOME_TYPE code 'WRONG_CODE' does not exist")
  }

  @Test
  fun `create Screening Outcome with invalid OutcomeSignedOffBy code`() {
    whenever(referenceDataRepository.findByDomainAndCode(any(), eq("CODE"))).thenReturn(referenceDataDecisionOutcome())
    val error = assertThrows<IllegalArgumentException> {
      underTest.createDecisionAndActionsRequest(
        UUID.randomUUID(),
        CreateDecisionAndActionsRequest(
          outcomeTypeCode = "CODE",
          outcomeSignedOffByRoleCode = "WRONG_CODE",
          conclusion = "yes",
          outcomeRecordedBy = null,
          outcomeRecordedByDisplayName = null,
          outcomeDate = LocalDate.of(2021, 1, 1),
          nextSteps = "next",
          isActionOpenCsipAlert = true,
          isActionNonAssociationsUpdated = true,
          isActionObservationBook = true,
          isActionUnitOrCellMove = true,
          isActionCsraOrRsraReview = true,
          isActionServiceReferral = true,
          isActionSimReferral = true,
          actionOther = "other",
        ),
        requestContext(),
      )
    }

    assertThat(error.message).isEqualTo("OUTCOME_TYPE code 'WRONG_CODE' does not exist")
  }

  @Test
  fun `create Screening Outcome with invalid Csip Record UUID`() {
    val recordUuid = UUID.randomUUID()
    whenever(referenceDataRepository.findByDomainAndCode(any(), eq("CODE"))).thenReturn(referenceDataDecisionOutcome())
    whenever(referenceDataRepository.findByDomainAndCode(any(), eq("SIGNEDOFF"))).thenReturn(referenceDataDecisionOutcomeSignedOffBy())

    val error = assertThrows<CsipRecordNotFoundException> {
      underTest.createDecisionAndActionsRequest(
        recordUuid,
        CreateDecisionAndActionsRequest(
          outcomeTypeCode = "CODE",
          outcomeSignedOffByRoleCode = "SIGNEDOFF",
          conclusion = "yes",
          outcomeRecordedBy = null,
          outcomeRecordedByDisplayName = null,
          outcomeDate = LocalDate.of(2021, 1, 1),
          nextSteps = "next",
          isActionOpenCsipAlert = true,
          isActionNonAssociationsUpdated = true,
          isActionObservationBook = true,
          isActionUnitOrCellMove = true,
          isActionCsraOrRsraReview = true,
          isActionServiceReferral = true,
          isActionSimReferral = true,
          actionOther = "other",
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
        incidentType = referenceDataDecisionOutcome(),
        incidentLocation = referenceDataDecisionOutcome(),
        refererAreaOfWork = referenceDataDecisionOutcome(),
        incidentInvolvement = referenceDataDecisionOutcome(),
      ),
    )
  }

  private fun referenceDataDecisionOutcome() = ReferenceData(
    referenceDataId = 1,
    domain = ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
    code = "CODE",
    description = "Reference",
    listSequence = 99,
    createdAt = LocalDateTime.now(),
    createdBy = "admin",
  )

  private fun referenceDataDecisionOutcomeSignedOffBy() = ReferenceData(
    referenceDataId = 2,
    domain = ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
    code = "SIGNEDOFF",
    description = "Signed off",
    listSequence = 100,
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