package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.dto.PrisonerDto
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer.NO
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_NUMBER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateReferralRequest
import java.time.LocalDate
import java.time.LocalTime

fun createCsipRecordRequest(
  createReferralRequest: CreateReferralRequest = createReferralRequest(),
  logCode: String? = LOG_CODE,
) = CreateCsipRecordRequest(
  logCode,
  createReferralRequest,
)

fun createReferralRequest(
  incidentTypeCode: String = "ATO",
  incidentLocationCode: String = "EDU",
  refererAreaCode: String = "ACT",
  incidentInvolvementCode: String = "OTH",
  contributoryFactorTypeCode: Collection<String> = listOf("AFL"),
  referralComplete: Boolean? = null,
  completedDate: LocalDate? = null,
  completedBy: String? = null,
  completedByDisplayName: String? = null,
) = CreateReferralRequest(
  LocalDate.now(),
  LocalTime.now(),
  incidentTypeCode,
  incidentLocationCode,
  "REFERRER",
  refererAreaCode,
  false,
  false,
  "",
  incidentInvolvementCode,
  "concern description",
  "known reasons",
  "",
  NO,
  referralComplete,
  completedDate,
  completedBy,
  completedByDisplayName,
  contributoryFactorTypeCode.map { createContributoryFactorRequest(it) },
)

fun createContributoryFactorRequest(factorTypeCode: String = "D") =
  CreateContributoryFactorRequest(factorTypeCode, "comment")

fun prisoner() = PrisonerDto(
  prisonerNumber = PRISON_NUMBER,
  bookingId = 1234,
  "First",
  "Middle",
  "Last",
  LocalDate.of(1988, 4, 3),
  PRISON_CODE_LEEDS,
)

const val LOG_CODE = "ZXY987"
