package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toInitialReferralEntity
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReferralRequest
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

fun createCsipRecordRequest(
  incidentTypeCode: String = "A",
  incidentLocationCode: String = "B",
  refererAreaCode: String = "C",
  incidentInvolvementCode: String? = "D",
  contributoryFactorTypeCode: Collection<String> = listOf("D"),
  logCode: String? = LOG_CODE,
) = CreateCsipRecordRequest(
  logCode,
  createReferralRequest(
    incidentTypeCode,
    incidentLocationCode,
    refererAreaCode,
    incidentInvolvementCode,
    contributoryFactorTypeCode,
  ),
)

fun createReferralRequest(
  incidentTypeCode: String = "A",
  incidentLocationCode: String = "B",
  refererAreaCode: String = "C",
  incidentInvolvementCode: String? = "D",
  contributoryFactorTypeCode: Collection<String> = listOf("D"),
) = CreateReferralRequest(
  LocalDate.now(),
  LocalTime.now(),
  incidentTypeCode,
  incidentLocationCode,
  "REFERRER",
  refererAreaCode,
  "summary",
  false,
  false,
  "",
  incidentInvolvementCode,
  "concern description",
  "known reasons",
  "",
  false,
  null,
  contributoryFactorTypeCode.map { createContributoryFactorRequest(it) },
)

fun referral(csipRecord: CsipRecord = csipRecord()) = createCsipRecordRequest().toInitialReferralEntity(
  csipRecord,
  csipRequestContext(),
  incidentType(),
  incidentLocation(),
  areaOfWork(),
  incidentInvolvement(),
  LocalDate.of(2030, 12, 25),
).apply {
  addContributoryFactor(
    createRequest = CreateContributoryFactorRequest(
      factorTypeCode = "D",
      comment = "comment",
    ),
    factorType = contributoryFactorType(),
    actionedAt = LocalDateTime.now(),
    actionedBy = TEST_USER,
    actionedByDisplayName = TEST_USER_NAME,
    source = Source.DPS,
  )
}

fun createContributoryFactorRequest(factorTypeCode: String = "D") =
  CreateContributoryFactorRequest(factorTypeCode, "comment")

fun csipRequestContext() =
  CsipRequestContext(username = "USERNAME", userDisplayName = "USER DISPLAY NAME", activeCaseLoadId = PRISON_NUMBER)

fun prisoner() = PrisonerDto(
  prisonerNumber = PRISON_NUMBER,
  bookingId = 1234,
  "First",
  "Middle",
  "Last",
  LocalDate.of(1988, 4, 3),
  PRISON_CODE_LEEDS,
)

fun incidentType() = ReferenceData(
  domain = ReferenceDataType.INCIDENT_TYPE,
  code = "A",
  description = "incident type",
  listSequence = 1,
  createdAt = LocalDateTime.now(),
  createdBy = "USER",
)

fun incidentLocation() = ReferenceData(
  domain = ReferenceDataType.INCIDENT_LOCATION,
  code = "B",
  description = "incident location",
  listSequence = 1,
  createdAt = LocalDateTime.now(),
  createdBy = "USER",
)

fun areaOfWork() = ReferenceData(
  domain = ReferenceDataType.AREA_OF_WORK,
  code = "C",
  description = "area of work",
  listSequence = 1,
  createdAt = LocalDateTime.now(),
  createdBy = "USER",
)

fun incidentInvolvement() = ReferenceData(
  domain = ReferenceDataType.INCIDENT_INVOLVEMENT,
  code = "B",
  description = "incident involvement",
  listSequence = 1,
  createdAt = LocalDateTime.now(),
  createdBy = "USER",
)

fun contributoryFactorType() = ReferenceData(
  domain = ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE,
  code = "D",
  description = "contributory factor type",
  listSequence = 1,
  createdAt = LocalDateTime.now(),
  createdBy = "USER",
)

fun csipRecord(): CsipRecord {
  val csipRecord = CsipRecord(
    prisonNumber = PRISON_NUMBER,
    logCode = LOG_CODE,
    createdAt = LocalDateTime.now(),
    createdBy = "USER",
    createdByDisplayName = "USER",
  )
  return csipRecord
}

const val LOG_CODE = "ZXY987"
