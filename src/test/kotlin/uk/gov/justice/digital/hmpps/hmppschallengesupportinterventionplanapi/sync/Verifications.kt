package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Attendee
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Auditable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ContributoryFactor
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.DecisionAndActions
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.IdentifiedNeed
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Interview
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Plan
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Review
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
import java.time.temporal.ChronoUnit

fun CsipRecord.verifyAgainst(request: SyncCsipRequest) {
  assertThat(prisonNumber).isEqualTo(request.prisonNumber)
  assertThat(logCode).isEqualTo(request.logCode)
  request.referral?.also { requireNotNull(referral).verifyAgainst(it) }
  request.plan?.also { requireNotNull(plan).verifyAgainst(it) }
  verifyAuditFields(request)
}

fun Referral.verifyAgainst(request: SyncReferralRequest) {
  assertThat(incidentDate).isEqualTo(request.incidentDate)
  assertThat(incidentTime).isEqualTo(request.incidentTime)
  assertThat(referredBy).isEqualTo(request.referredBy)
  assertThat(proactiveReferral).isEqualTo(request.isProactiveReferral)
  assertThat(staffAssaulted).isEqualTo(request.isStaffAssaulted)
  assertThat(assaultedStaffName).isEqualTo(request.assaultedStaffName)
  assertThat(descriptionOfConcern).isEqualTo(request.descriptionOfConcern)
  assertThat(knownReasons).isEqualTo(request.knownReasons)
  assertThat(otherInformation).isEqualTo(request.otherInformation)
  assertThat(saferCustodyTeamInformed).isEqualTo(request.isSaferCustodyTeamInformed)
  assertThat(referralComplete).isEqualTo(request.isReferralComplete)
  assertThat(referralCompletedDate).isEqualTo(request.completedDate)
  assertThat(referralCompletedBy).isEqualTo(request.completedBy)
  assertThat(referralCompletedByDisplayName).isEqualTo(request.completedByDisplayName)
  assertThat(incidentType.code).isEqualTo(request.incidentTypeCode)
  assertThat(incidentLocation.code).isEqualTo(request.incidentLocationCode)
  assertThat(refererAreaOfWork.code).isEqualTo(request.refererAreaCode)
  assertThat(incidentInvolvement?.code).isEqualTo(request.incidentInvolvementCode)
  request.saferCustodyScreeningOutcome?.also { requireNotNull(saferCustodyScreeningOutcome).verifyAgainst(it) }
}

fun ContributoryFactor.verifyAgainst(request: SyncContributoryFactorRequest) {
  assertThat(contributoryFactorType.code).isEqualTo(request.factorTypeCode)
  assertThat(comment).isEqualTo(request.comment)
  verifyAuditFields(request)
}

fun SaferCustodyScreeningOutcome.verifyAgainst(request: SyncScreeningOutcomeRequest) {
  assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
  assertThat(reasonForDecision).isEqualTo(request.reasonForDecision)
  assertThat(date).isEqualTo(request.date)
  assertThat(recordedBy).isEqualTo(request.recordedBy)
  assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
}

fun Investigation.verifyAgainst(request: SyncInvestigationRequest) {
  assertThat(evidenceSecured).isEqualTo(request.evidenceSecured)
  assertThat(occurrenceReason).isEqualTo(request.occurrenceReason)
  assertThat(personsUsualBehaviour).isEqualTo(request.personsUsualBehaviour)
  assertThat(personsTrigger).isEqualTo(request.personsTrigger)
  assertThat(protectiveFactors).isEqualTo(request.protectiveFactors)
}

fun Interview.verifyAgainst(request: SyncInterviewRequest) {
  assertThat(interviewee).isEqualTo(request.interviewee)
  assertThat(interviewDate).isEqualTo(request.interviewDate)
  assertThat(intervieweeRole.code).isEqualTo(request.intervieweeRoleCode)
  assertThat(interviewText).isEqualTo(request.interviewText)
  verifyAuditFields(request)
}

fun DecisionAndActions.verifyAgainst(request: SyncDecisionAndActionsRequest) {
  assertThat(outcome.code).isEqualTo(request.outcomeTypeCode)
  assertThat(signedOffBy?.code).isEqualTo(request.signedOffByRoleCode)
  assertThat(date).isEqualTo(request.date)
  assertThat(recordedBy).isEqualTo(request.recordedBy)
  assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
  assertThat(nextSteps).isEqualTo(request.nextSteps)
  assertThat(actionOther).isEqualTo(request.actionOther)
  assertThat(actions).containsExactlyInAnyOrderElementsOf(request.actions)
}

fun Plan.verifyAgainst(request: SyncPlanRequest) {
  assertThat(caseManager).isEqualTo(request.caseManager)
  assertThat(reasonForPlan).isEqualTo(request.reasonForPlan)
  assertThat(firstCaseReviewDate).isEqualTo(request.firstCaseReviewDate)
}

fun IdentifiedNeed.verifyAgainst(request: SyncNeedRequest) {
  assertThat(identifiedNeed).isEqualTo(request.identifiedNeed)
  assertThat(responsiblePerson).isEqualTo(request.responsiblePerson)
  assertThat(createdDate).isEqualTo(request.createdDate)
  assertThat(targetDate).isEqualTo(request.targetDate)
  assertThat(closedDate).isEqualTo(request.closedDate)
  assertThat(intervention).isEqualTo(request.intervention)
  assertThat(progression).isEqualTo(request.progression)
  verifyAuditFields(request)
}

fun Review.verifyAgainst(request: SyncReviewRequest) {
  assertThat(reviewDate).isEqualTo(request.reviewDate)
  assertThat(recordedBy).isEqualTo(request.recordedBy)
  assertThat(recordedByDisplayName).isEqualTo(request.recordedByDisplayName)
  assertThat(nextReviewDate).isEqualTo(request.nextReviewDate)
  assertThat(csipClosedDate).isEqualTo(request.csipClosedDate)
  assertThat(summary).isEqualTo(request.summary)
  assertThat(actions).containsExactlyInAnyOrderElementsOf(request.actions)
  verifyAuditFields(request)
}

fun Attendee.verifyAgainst(request: SyncAttendeeRequest) {
  assertThat(name).isEqualTo(request.name)
  assertThat(role).isEqualTo(request.role)
  assertThat(attended).isEqualTo(request.isAttended)
  assertThat(contribution).isEqualTo(request.contribution)
  verifyAuditFields(request)
}

fun Auditable.verifyAuditFields(request: NomisAudited) {
  assertThat(createdAt).isCloseTo(request.createdAt, within(1, ChronoUnit.SECONDS))
  assertThat(createdBy).isEqualTo(request.createdBy)
  assertThat(createdByDisplayName).isEqualTo(request.createdByDisplayName)
  request.lastModifiedAt?.also {
    assertThat(lastModifiedAt).isCloseTo(it, within(1, ChronoUnit.SECONDS))
    assertThat(lastModifiedBy).isEqualTo(request.lastModifiedBy)
    assertThat(lastModifiedByDisplayName).isEqualTo(request.lastModifiedByDisplayName)
  }
}