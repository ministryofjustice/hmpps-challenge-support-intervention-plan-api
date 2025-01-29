package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.prisonersearch.PrisonerDetails
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.PersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.plan.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.SaferCustodyScreeningOutcome
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.CsipRecord as CsipModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Attendee as AttendeeModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.IdentifiedNeed as NeedModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Plan as PlanModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.plan.Review as ReviewModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.ContributoryFactor as FactorModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.DecisionAndActions as DecisionModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Interview as InterviewModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Investigation as InvestigationModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.Referral as ReferralModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.SaferCustodyScreeningOutcome as ScreeningModel

const val LOG_CODE = "ZXY987"

fun createContributoryFactorRequest(type: String = "BAS", comment: String? = "comment about the factor") = CreateContributoryFactorRequest(type, comment)

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
  restrictedPatient: Boolean = false,
  cellLocation: String? = cellLocation(),
  supportingPrisonId: String? = null,
) = PrisonerDetails(
  prisonerNumber,
  firstName,
  lastName,
  prisonId,
  status,
  restrictedPatient,
  cellLocation,
  supportingPrisonId,
)

fun CsipModel.verifyAgainst(csip: CsipRecord) {
  assertThat(recordUuid).isEqualTo(csip.id)
  assertThat(logCode).isEqualTo(csip.logCode)
  csip.referral?.also { referral.verifyAgainst(it) }
  csip.plan?.also { requireNotNull(plan).verifyAgainst(it) }
}

fun ReferralModel.verifyAgainst(referral: Referral) {
  assertThat(incidentDate).isEqualTo(referral.incidentDate)
  referral.incidentTime?.also { assertThat(incidentTime).isCloseTo(it, within(1, ChronoUnit.SECONDS)) }
  assertThat(referredBy).isEqualTo(referral.referredBy)
  assertThat(isProactiveReferral).isEqualTo(referral.proactiveReferral)
  assertThat(isStaffAssaulted).isEqualTo(referral.staffAssaulted)
  assertThat(assaultedStaffName).isEqualTo(referral.assaultedStaffName)
  assertThat(descriptionOfConcern).isEqualTo(referral.descriptionOfConcern)
  assertThat(knownReasons).isEqualTo(referral.knownReasons)
  assertThat(otherInformation).isEqualTo(referral.otherInformation)
  assertThat(isSaferCustodyTeamInformed).isEqualTo(referral.saferCustodyTeamInformed)
  assertThat(isReferralComplete).isEqualTo(referral.referralComplete)
  assertThat(incidentType.code).isEqualTo(referral.incidentType.code)
  assertThat(incidentLocation.code).isEqualTo(referral.incidentLocation.code)
  assertThat(refererArea.code).isEqualTo(referral.refererAreaOfWork.code)
  assertThat(incidentInvolvement?.code).isEqualTo(referral.incidentInvolvement?.code)
  assertThat(contributoryFactors.size).isEqualTo(referral.contributoryFactors().size)
  referral.saferCustodyScreeningOutcome?.also { requireNotNull(saferCustodyScreeningOutcome).verifyAgainst(it) }
  referral.decisionAndActions?.also { requireNotNull(decisionAndActions).verifyAgainst(it) }
  referral.investigation?.also { requireNotNull(investigation).verifyAgainst(it) }
  referral.contributoryFactors()
    .forEach { cf -> contributoryFactors.first { it.factorUuid == cf.id }.verifyAgainst(cf) }
}

private fun FactorModel.verifyAgainst(factor: ContributoryFactor) {
  assertThat(factorType.code).isEqualTo(factor.contributoryFactorType.code)
  assertThat(comment).isEqualTo(factor.comment)
}

private fun ScreeningModel.verifyAgainst(screening: SaferCustodyScreeningOutcome) {
  assertThat(outcome.code).isEqualTo(screening.outcome.code)
  assertThat(reasonForDecision).isEqualTo(screening.reasonForDecision)
  assertThat(date).isEqualTo(screening.date)
  assertThat(recordedBy).isEqualTo(screening.recordedBy)
  assertThat(recordedByDisplayName).isEqualTo(screening.recordedByDisplayName)
}

private fun DecisionModel.verifyAgainst(decision: DecisionAndActions) {
  assertThat(outcome?.code).isEqualTo(decision.outcome?.code)
  assertThat(conclusion).isEqualTo(decision.conclusion)
  assertThat(signedOffByRole?.code).isEqualTo(decision.signedOffBy?.code)
  assertThat(recordedBy).isEqualTo(decision.recordedBy)
  assertThat(recordedByDisplayName).isEqualTo(decision.recordedByDisplayName)
  assertThat(date).isEqualTo(decision.date)
  assertThat(nextSteps).isEqualTo(decision.nextSteps)
  assertThat(actions).containsExactlyInAnyOrderElementsOf(decision.actions)
  assertThat(actionOther).isEqualTo(decision.actionOther)
}

private fun InvestigationModel.verifyAgainst(investigation: Investigation) {
  assertThat(staffInvolved).isEqualTo(investigation.staffInvolved)
  assertThat(evidenceSecured).isEqualTo(investigation.evidenceSecured)
  assertThat(occurrenceReason).isEqualTo(investigation.occurrenceReason)
  assertThat(personsUsualBehaviour).isEqualTo(investigation.personsUsualBehaviour)
  assertThat(personsTrigger).isEqualTo(investigation.personsTrigger)
  assertThat(protectiveFactors).isEqualTo(investigation.protectiveFactors)
  assertThat(interviews.size).isEqualTo(investigation.interviews().size)
  investigation.interviews()
    .forEach { interview -> interviews.first { it.interviewUuid == interview.id }.verifyAgainst(interview) }
}

private fun InterviewModel.verifyAgainst(interview: Interview) {
  assertThat(interviewee).isEqualTo(interview.interviewee)
  assertThat(intervieweeRole.code).isEqualTo(interview.intervieweeRole.code)
  assertThat(interviewDate).isEqualTo(interview.interviewDate)
  assertThat(interviewText).isEqualTo(interview.interviewText)
}

private fun PlanModel.verifyAgainst(plan: Plan) {
  assertThat(caseManager).isEqualTo(plan.caseManager)
  assertThat(reasonForPlan).isEqualTo(plan.reasonForPlan)
  assertThat(firstCaseReviewDate).isEqualTo(plan.firstCaseReviewDate)
  assertThat(identifiedNeeds.size).isEqualTo(plan.identifiedNeeds().size)
  plan.identifiedNeeds()
    .forEach { need -> identifiedNeeds.first { it.identifiedNeedUuid == need.id }.verifyAgainst(need) }
  assertThat(reviews.size).isEqualTo(plan.reviews().size)
  plan.reviews().forEach { r -> reviews.first { it.reviewUuid == r.id }.verifyAgainst(r) }
}

private fun NeedModel.verifyAgainst(need: IdentifiedNeed) {
  assertThat(identifiedNeed).isEqualTo(need.identifiedNeed)
  assertThat(responsiblePerson).isEqualTo(need.responsiblePerson)
  assertThat(createdDate).isEqualTo(need.createdDate)
  assertThat(targetDate).isEqualTo(need.targetDate)
  assertThat(closedDate).isEqualTo(need.closedDate)
  assertThat(intervention).isEqualTo(need.intervention)
  assertThat(progression).isEqualTo(need.progression)
}

private fun ReviewModel.verifyAgainst(review: Review) {
  assertThat(reviewDate).isEqualTo(review.reviewDate)
  assertThat(recordedBy).isEqualTo(review.recordedBy)
  assertThat(recordedByDisplayName).isEqualTo(review.recordedByDisplayName)
  assertThat(nextReviewDate).isEqualTo(review.nextReviewDate)
  assertThat(csipClosedDate).isEqualTo(review.csipClosedDate)
  assertThat(summary).isEqualTo(review.summary)
  assertThat(actions).isEqualTo(review.actions)
  assertThat(attendees.size).isEqualTo(review.attendees().size)
  review.attendees().forEach { a -> attendees.first { it.attendeeUuid == a.id }.verifyAgainst(a) }
}

private fun AttendeeModel.verifyAgainst(attendee: Attendee) {
  assertThat(name).isEqualTo(attendee.name)
  assertThat(role).isEqualTo(attendee.role)
  assertThat(isAttended).isEqualTo(attendee.attended)
  assertThat(contribution).isEqualTo(attendee.contribution)
}
