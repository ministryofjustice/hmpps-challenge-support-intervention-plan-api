package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service

import org.mockito.kotlin.mock
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.REFERENCE_DATA_CODE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.DO_NOT_KNOW
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.CsipRecordRepository
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.repository.ReferenceDataRepository
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

abstract class BaseServiceTest {
  protected val csipRecordRepository = mock<CsipRecordRepository>()
  protected val referenceDataRepository = mock<ReferenceDataRepository>()

  protected fun csipRecord() = CsipRecord(
    recordId = 5516,
    recordUuid = UUID.randomUUID(),
    prisonNumber = "quisque",
    prisonCodeWhenRecorded = null,
    logCode = null,
    createdAt = LocalDateTime.now(),
    createdBy = "ornatus",
    createdByDisplayName = "Belinda Drake",
  ).apply {
    setReferral(
      Referral(
        csipRecord = this,
        incidentDate = LocalDate.now(),
        incidentTime = null,
        referredBy = "falli",
        referralDate = LocalDate.now(),
        proactiveReferral = null,
        staffAssaulted = null,
        assaultedStaffName = null,
        releaseDate = null,
        descriptionOfConcern = "purus",
        knownReasons = "iuvaret",
        otherInformation = null,
        saferCustodyTeamInformed = DO_NOT_KNOW,
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

  protected fun referenceData() = ReferenceData(
    referenceDataId = 1,
    domain = ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
    code = REFERENCE_DATA_CODE,
    description = "Reference",
    listSequence = 99,
    createdAt = LocalDateTime.now(),
    createdBy = "admin",
  )

  protected fun requestContext() = CsipRequestContext(
    source = Source.DPS,
    username = TEST_USER,
    userDisplayName = TEST_USER_NAME,
    activeCaseLoadId = PRISON_CODE_LEEDS,
  )
}
