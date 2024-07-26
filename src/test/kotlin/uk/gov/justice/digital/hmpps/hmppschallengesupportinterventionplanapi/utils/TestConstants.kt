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
  incidentTypeCode: String = "ATO",
  incidentLocationCode: String = "EDU",
  refererAreaCode: String = "ACT",
  incidentInvolvementCode: String? = "OTH",
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
  false,
  false,
  "",
  incidentInvolvementCode,
  "concern description",
  "known reasons",
  "",
  NO,
  null,
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
