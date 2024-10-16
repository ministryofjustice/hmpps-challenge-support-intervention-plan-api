package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.NOMIS_SYS_USER_DISPLAY_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.PRISON_CODE_LEEDS
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.wiremock.TEST_USER_NAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateAttendeeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.request.CreateIdentifiedNeedRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateContributoryFactorRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateInterviewRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.CreateReferralRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateCsipRecordRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.cellLocation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.NomisIdGenerator.prisonNumber
import java.time.LocalDate
import java.time.temporal.ChronoUnit

const val LOG_CODE = "ZXY987"

fun createContributoryFactorRequest(type: String = "BAS", comment: String? = "comment about the factor") =
  CreateContributoryFactorRequest(type, comment)

fun createInterviewRequest(
  roleCode: String = "OTHER",
  date: LocalDate = LocalDate.now(),
  interviewee: String = "A Person",
  notes: String? = null,
) = CreateInterviewRequest(interviewee, date, roleCode, notes)

fun createIdentifiedNeedRequest(
  identifiedNeed: String = "An identified need",
  needIdentifiedBy: String = "I Dent",
  createdDate: LocalDate = LocalDate.now(),
  targetDate: LocalDate = LocalDate.now().plusWeeks(8),
  closedDate: LocalDate? = null,
  intervention: String = "intervention description",
  progression: String? = null,
) = CreateIdentifiedNeedRequest(
  identifiedNeed,
  needIdentifiedBy,
  createdDate,
  targetDate,
  closedDate,
  intervention,
  progression,
)

fun createAttendeeRequest(
  name: String? = "name",
  role: String? = "role",
  isAttended: Boolean? = true,
  contribution: String? = "a small contribution",
) = CreateAttendeeRequest(name, role, isAttended, contribution)

fun testUserContext() = CsipRequestContext(
  source = Source.DPS,
  username = TEST_USER,
  userDisplayName = TEST_USER_NAME,
  activeCaseLoadId = PRISON_CODE_LEEDS,
)

fun nomisContext() = CsipRequestContext(
  source = Source.NOMIS,
  username = NOMIS_SYS_USER,
  userDisplayName = NOMIS_SYS_USER_DISPLAY_NAME,
)

fun CsipRecord.verifyAgainst(request: CreateCsipRecordRequest) {
  assertThat(logCode).isEqualTo(request.logCode)
  requireNotNull(referral).verifyAgainst(request.referral)
}

fun Referral.verifyAgainst(request: CreateReferralRequest) {
  assertThat(incidentDate).isEqualTo(request.incidentDate)
  assertThat(incidentTime).isCloseTo(request.incidentTime, within(1, ChronoUnit.SECONDS))
  assertThat(referredBy).isEqualTo(request.referredBy)
  assertThat(proactiveReferral).isEqualTo(request.isProactiveReferral)
  assertThat(staffAssaulted).isEqualTo(request.isStaffAssaulted)
  assertThat(assaultedStaffName).isEqualTo(request.assaultedStaffName)
  assertThat(descriptionOfConcern).isEqualTo(request.descriptionOfConcern)
  assertThat(knownReasons).isEqualTo(request.knownReasons)
  assertThat(otherInformation).isEqualTo(request.otherInformation)
  assertThat(saferCustodyTeamInformed).isEqualTo(request.isSaferCustodyTeamInformed)
  assertThat(referralComplete).isEqualTo(request.isReferralComplete)
  assertThat(incidentType.code).isEqualTo(request.incidentTypeCode)
  assertThat(incidentLocation.code).isEqualTo(request.incidentLocationCode)
  assertThat(refererAreaOfWork.code).isEqualTo(request.refererAreaCode)
  assertThat(incidentInvolvement?.code).isEqualTo(request.incidentInvolvementCode)
}

fun PersonSummary.verifyAgainst(prisonerDetails: PrisonerDetails) {
  assertThat(prisonNumber).isEqualTo(prisonerDetails.prisonerNumber)
  assertThat(firstName).isEqualTo(prisonerDetails.firstName)
  assertThat(lastName).isEqualTo(prisonerDetails.lastName)
  assertThat(status).isEqualTo(prisonerDetails.status)
  assertThat(prisonCode).isEqualTo(prisonerDetails.prisonId)
  assertThat(cellLocation).isEqualTo(prisonerDetails.cellLocation)
}

fun prisoner(
  prisonerNumber: String = prisonNumber(),
  firstName: String = "First",
  lastName: String = "Last",
  prisonId: String? = "LEI",
  status: String = "ACTIVE IN",
  cellLocation: String? = cellLocation(),
) = PrisonerDetails(prisonerNumber, firstName, lastName, prisonId, status, cellLocation)
