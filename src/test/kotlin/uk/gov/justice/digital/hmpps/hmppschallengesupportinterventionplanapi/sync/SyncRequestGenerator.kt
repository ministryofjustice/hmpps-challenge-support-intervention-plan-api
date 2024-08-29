package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.NomisIdGenerator.newId
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

object SyncRequestGenerator {

  fun syncCsipRequest(
    logCode: String? = null,
    referral: SyncReferralRequest? = null,
    plan: SyncPlanRequest? = null,
    prisonNumber: String = NomisIdGenerator.prisonNumber(),
    prisonCodeWhenRecorded: String? = null,
    actionedAt: LocalDateTime = LocalDateTime.now(),
    actionedBy: String = "actionedBy",
    actionedByDisplayName: String = "actionedByDisplayName",
    activeCaseloadId: String? = null,
    id: Long = newId(),
    uuid: UUID? = null,
  ) = SyncCsipRequest(
    prisonNumber,
    logCode,
    referral,
    plan,
    prisonCodeWhenRecorded,
    actionedAt,
    actionedBy,
    actionedByDisplayName,
    activeCaseloadId,
    id,
    uuid,
  ).withAuditDetail()

  fun syncReferralRequest(
    referralDate: LocalDate = LocalDate.now().minusDays(1),
    incidentDate: LocalDate = LocalDate.now().minusDays(3),
    incidentTime: LocalTime? = null,
    incidentTypeCode: String = "FTE",
    incidentLocationCode: String = "OMU",
    referredBy: String = "referredBy",
    refererAreaCode: String = "PSY",
    isProactiveReferral: Boolean? = true,
    isStaffAssaulted: Boolean? = false,
    assaultedStaffName: String? = null,
    incidentInvolvementCode: String? = null,
    descriptionOfConcern: String? = "Description of concern",
    knownReasons: String? = "Known Reasons",
    otherInformation: String? = "Other Information",
    isSaferCustodyTeamInformed: OptionalYesNoAnswer = OptionalYesNoAnswer.YES,
    isReferralComplete: Boolean? = null,
    completedDate: LocalDate? = null,
    completedBy: String? = null,
    completedByDisplayName: String? = null,
    contributoryFactors: List<SyncContributoryFactorRequest> = listOf(),
    saferCustodyScreeningOutcome: SyncScreeningOutcomeRequest? = null,
    investigation: SyncInvestigationRequest? = null,
    decisionAndActions: SyncDecisionAndActionsRequest? = null,
  ) = SyncReferralRequest(
    referralDate,
    incidentDate,
    incidentTime,
    incidentTypeCode,
    incidentLocationCode,
    referredBy,
    refererAreaCode,
    isProactiveReferral,
    isStaffAssaulted,
    assaultedStaffName,
    incidentInvolvementCode,
    descriptionOfConcern,
    knownReasons,
    otherInformation,
    isSaferCustodyTeamInformed,
    isReferralComplete,
    completedDate,
    completedBy,
    completedByDisplayName,
    contributoryFactors,
    saferCustodyScreeningOutcome,
    investigation,
    decisionAndActions,
  )

  fun syncContributoryFactorRequest(
    typeCode: String = "DEB",
    comment: String? = "A comment about the factor",
    id: Long = newId(),
    uuid: UUID? = null,
  ): SyncContributoryFactorRequest = SyncContributoryFactorRequest(typeCode, comment, id, uuid).withAuditDetail()

  fun syncScreeningOutcomeRequest(
    outcomeCode: String = "OPE",
    reasonForDecision: String = "A reason for the decision",
    date: LocalDate = LocalDate.now(),
    recordedBy: String = "recordedBy",
    recordedByDisplayName: String = "recordedByDisplayName",
  ) = SyncScreeningOutcomeRequest(outcomeCode, reasonForDecision, date, recordedBy, recordedByDisplayName)

  fun syncInvestigationRequest(
    staffInvolved: String? = null,
    evidenceSecured: String? = "Evidence Secured",
    occurrenceReason: String? = "Occurrence Reason",
    personsUsualBehaviour: String? = "Person's Usual Behaviour",
    personsTrigger: String? = null,
    protectiveFactors: String? = null,
    interviews: List<SyncInterviewRequest> = listOf(),
  ) = SyncInvestigationRequest(
    staffInvolved,
    evidenceSecured,
    occurrenceReason,
    personsUsualBehaviour,
    personsTrigger,
    protectiveFactors,
    interviews,
  )

  fun syncInterviewRequest(
    interviewee: String = "Interviewee",
    interviewDate: LocalDate = LocalDate.now(),
    intervieweeRole: String = "OTHER",
    interviewText: String? = "Some notes about the interview",
    id: Long = newId(),
    uuid: UUID? = null,
  ) = SyncInterviewRequest(interviewee, interviewDate, intervieweeRole, interviewText, id, uuid)
    .withAuditDetail()

  fun syncDecisionRequest(
    conclusion: String? = "conclusion",
    outcomeCode: String = "ACC",
    signedOffByRole: String? = "CUSTMAN",
    date: LocalDate? = LocalDate.now(),
    recordedBy: String? = "recordedBy",
    recordedByDisplayName: String? = "recordedByDisplayName",
    nextSteps: String? = null,
    actionOther: String? = null,
    actions: Set<DecisionAction> = setOf(DecisionAction.OPEN_CSIP_ALERT),
  ) = SyncDecisionAndActionsRequest(
    conclusion,
    outcomeCode,
    signedOffByRole,
    date,
    recordedBy,
    recordedByDisplayName,
    nextSteps,
    actionOther,
    actions,
  )

  fun syncPlanRequest(
    caseManager: String = "Case Manager",
    reasonForPlan: String = "Reason for the plan",
    firstCaseReviewDate: LocalDate = LocalDate.now(),
    identifiedNeeds: List<SyncNeedRequest> = listOf(),
    reviews: List<SyncReviewRequest> = listOf(),
  ) = SyncPlanRequest(caseManager, reasonForPlan, firstCaseReviewDate, identifiedNeeds, reviews)

  fun syncNeedRequest(
    identifiedNeed: String = "Identified need",
    responsiblePerson: String = "Responsible Person",
    createdDate: LocalDate = LocalDate.now(),
    targetDate: LocalDate = LocalDate.now().plusWeeks(12),
    closedDate: LocalDate? = null,
    intervention: String = "Intervention",
    progression: String? = "Progression",
    id: Long = newId(),
    uuid: UUID? = null,
  ) = SyncNeedRequest(
    identifiedNeed, responsiblePerson, createdDate, targetDate, closedDate, intervention, progression, id, uuid,
  ).withAuditDetail()

  fun syncReviewRequest(
    reviewDate: LocalDate? = LocalDate.now(),
    recordedBy: String = "recordedBy",
    recordedByDisplayName: String = "recordedByDisplayName",
    nextReviewDate: LocalDate? = null,
    csipClosedDate: LocalDate? = null,
    summary: String? = "Summary of the review",
    actions: Set<ReviewAction> = setOf(ReviewAction.CASE_NOTE),
    attendees: List<SyncAttendeeRequest> = listOf(),
    id: Long = newId(),
    uuid: UUID? = null,
  ) = SyncReviewRequest(
    reviewDate,
    recordedBy,
    recordedByDisplayName,
    nextReviewDate,
    csipClosedDate,
    summary,
    actions,
    attendees,
    id,
    uuid,
  ).withAuditDetail()

  fun syncAttendeeRequest(
    name: String? = "Attendee name",
    role: String? = "",
    isAttended: Boolean? = true,
    contribution: String? = "Contribution",
    id: Long = newId(),
    uuid: UUID? = null,
  ) = SyncAttendeeRequest(name, role, isAttended, contribution, id, uuid).withAuditDetail()

  private fun invalidCsip() = syncCsipRequest(
    prisonNumber = "A".repeat(11),
    logCode = "A".repeat(11),
    referral = invalidReferral(),
    plan = invalidPlan(),
  )

  private fun invalidReferral() = syncReferralRequest(
    incidentTypeCode = "A".repeat(13),
    incidentLocationCode = "A".repeat(13),
    incidentInvolvementCode = "A".repeat(13),
    refererAreaCode = "A".repeat(13),
    referredBy = "A".repeat(241),
    assaultedStaffName = "A".repeat(1001),
    completedBy = "A".repeat(65),
    completedByDisplayName = "A".repeat(256),
    contributoryFactors = listOf(invalidSyncFactor()),
    saferCustodyScreeningOutcome = invalidScreeningOutcome(),
    investigation = invalidInvestigation(),
    decisionAndActions = invalidDecision(),
  )

  private fun invalidSyncFactor() = syncContributoryFactorRequest(typeCode = "A".repeat(13))

  private fun invalidScreeningOutcome() = syncScreeningOutcomeRequest(
    outcomeCode = "A".repeat(13),
    reasonForDecision = "A".repeat(4001),
    recordedBy = "A".repeat(65),
    recordedByDisplayName = "A".repeat(256),
  )

  private fun invalidInvestigation() = syncInvestigationRequest(
    staffInvolved = "A".repeat(4001),
    evidenceSecured = "A".repeat(4001),
    occurrenceReason = "A".repeat(4001),
    personsUsualBehaviour = "A".repeat(4001),
    personsTrigger = "A".repeat(4001),
    protectiveFactors = "A".repeat(4001),
    interviews = listOf(invalidInterview()),
  )

  private fun invalidInterview() = syncInterviewRequest(
    interviewee = "A".repeat(101),
    intervieweeRole = "A".repeat(13),
    interviewText = "A".repeat(4001),
  )

  private fun invalidDecision() = syncDecisionRequest(
    conclusion = "A".repeat(4001),
    outcomeCode = "A".repeat(13),
    signedOffByRole = "A".repeat(13),
    recordedBy = "A".repeat(65),
    recordedByDisplayName = "A".repeat(256),
    actionOther = "A".repeat(4001),
    nextSteps = "A".repeat(4001),
  )

  private fun invalidPlan() = syncPlanRequest(
    caseManager = "A".repeat(101),
    reasonForPlan = "A".repeat(240),
    identifiedNeeds = listOf(invalidNeed()),
    reviews = listOf(invalidReview()),
  )

  private fun invalidNeed() = syncNeedRequest(
    identifiedNeed = "A".repeat(1001),
    responsiblePerson = "A".repeat(101),
    intervention = "A".repeat(4001),
    progression = "A".repeat(4001),
  )

  private fun invalidReview() = syncReviewRequest(
    recordedBy = "A".repeat(65),
    recordedByDisplayName = "A".repeat(256),
    summary = "A".repeat(4001),
    attendees = listOf(invalidAttendee()),
  )

  private fun invalidAttendee() = syncAttendeeRequest(
    name = "A".repeat(101),
    role = "A".repeat(51),
    contribution = "A".repeat(4001),
  )

  fun <T : NomisAudited> T.withAuditDetail() = apply {
    createdAt = LocalDateTime.now().minusMinutes(1)
    createdBy = "NOM_USER"
    createdByDisplayName = "${this::class.simpleName!!} NOMIS User"
  }

  fun <T : NomisAudited> T.withModifiedDetail() = apply {
    withAuditDetail()
    lastModifiedAt = LocalDateTime.now().minusMinutes(1)
    lastModifiedBy = "NOM_USER"
    lastModifiedByDisplayName = "${this::class.simpleName!!} NOMIS User"
  }

  fun badSyncRequest() = invalidCsip()
}
