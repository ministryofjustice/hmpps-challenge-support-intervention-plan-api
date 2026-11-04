package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.sar

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.CsipRecord
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
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * One CSIP for the prisoner, as it appears in a subject access request response.
 *
 * This is the response the Offender SAR team signed off at the data review (IR-2067), and follows the `[SAR: Y|N]`
 * tags on the column comments (V69, with their change in V70). What is left out is as deliberate as what is
 * included, and follows the SAR integration guiding principles:
 *  - internal identifiers are excluded - every UUID, version number and NOMIS id. The log code is the reference
 *    people actually quote;
 *  - the change history kept in the `*_audit` tables is excluded. The Offender SAR team confirmed on 3 November 2026
 *    that the current version of each CSIP is what they need;
 *  - codes that stand for something travel as their descriptions (Verbal abuse, Wing, Open CSIP), which are the
 *    wording the CSIP service shows its own users. Fixed lists held in the code rather than as reference data
 *    (decision and review actions, yes / no / do not know) are given plain-English wording here - see [label];
 *  - the prison where the CSIP was recorded travels as its code, for the template to resolve with `getPrisonName`;
 *  - a member of staff the service recorded as doing something (completing the referral, recording the screening,
 *    investigation, decision or a review) appears by surname only, taken from the name recorded at the time - see
 *    [surname]. Their username is not sent. That is the Offender SAR team's rule from the incident reporting data
 *    review, which they applied to CSIP;
 *  - names staff typed in (referred by, assaulted staff, staff involved, interviewees, case manager, responsible
 *    person, attendees) are sent exactly as typed. They may be full names, initials, job titles or people who are not
 *    staff, so they cannot be reliably reduced to a surname. The Offender SAR team agreed to redact them as needed;
 *  - the area of work of the person who made the referral is excluded, at the Offender SAR team's request (V70);
 *  - nothing owned by another service is fetched. CSIP Assist's suggested case notes are excluded: they are copies of
 *    case notes, which have their own section of the report.
 *
 * Lists are in a fixed order, so the same CSIP always gives the same report: contributory factors and the people at
 * a review alphabetically, interviews and identified needs by date, reviews in the order they were held.
 *
 * Other people named in a CSIP - other prisoners, staff and family members, mainly in free text - are included.
 * Redacting third-party data is the Offender SAR team's job, as they agreed at the data review.
 */
@Schema(description = "A Challenge, Support and Intervention Plan (CSIP) for the prisoner, for a subject access request")
data class SarCsipRecord(
  @Schema(description = "The CSIP's reference, where one was given", example = "LEI001")
  val logCode: String?,

  @Schema(description = "Code of the prison where the CSIP was recorded", example = "LEI")
  val prisonCodeWhenRecorded: String?,

  @Schema(description = "Where the CSIP has got to", example = "CSIP open")
  val status: String?,

  @Schema(description = "The referral that started the CSIP, with the screening, investigation and decision")
  val referral: SarReferral?,

  @Schema(description = "The plan, once one has been made, with its identified needs and reviews")
  val plan: SarPlan?,
) {
  companion object {
    fun from(record: CsipRecord) = SarCsipRecord(
      logCode = record.logCode,
      prisonCodeWhenRecorded = record.prisonCodeWhenRecorded,
      status = record.status?.description,
      referral = record.referral?.let(SarReferral::from),
      plan = record.plan?.let(SarPlan::from),
    )
  }
}

@Schema(description = "The referral that started a CSIP")
data class SarReferral(
  val referralDate: LocalDate,
  val incidentDate: LocalDate,
  @Schema(description = "Time of the incident, 24-hour clock", example = "14:30")
  val incidentTime: String?,
  val incidentType: String,
  val incidentLocation: String,
  @Schema(description = "Who made the referral, as typed in")
  val referredBy: String,
  @Schema(description = "Yes if the referral was made before any incident, to prevent one", example = "Yes")
  val proactiveReferral: String?,
  @Schema(example = "No")
  val staffAssaulted: String?,
  @Schema(description = "Names of any staff assaulted, as typed in")
  val assaultedStaffName: String?,
  @Schema(description = "How the prisoner was involved in the incident", example = "Perpetrator")
  val incidentInvolvement: String?,
  val descriptionOfConcern: String?,
  val knownReasons: String?,
  val otherInformation: String?,
  @Schema(example = "Do not know")
  val saferCustodyTeamInformed: String,
  @Schema(description = "Surname of the member of staff who completed the referral", example = "Smith")
  val referralCompletedByLastName: String?,
  val referralCompletedDate: LocalDate?,
  val contributoryFactors: List<SarContributoryFactor>,
  val saferCustodyScreeningOutcome: SarScreeningOutcome?,
  val investigation: SarInvestigation?,
  val decisionAndActions: SarDecisionAndActions?,
) {
  companion object {
    fun from(referral: Referral) = SarReferral(
      referralDate = referral.referralDate,
      incidentDate = referral.incidentDate,
      incidentTime = referral.incidentTime?.format(TIME),
      incidentType = referral.incidentType.description,
      incidentLocation = referral.incidentLocation.description,
      referredBy = referral.referredBy,
      proactiveReferral = referral.proactiveReferral.yesNo(),
      staffAssaulted = referral.staffAssaulted.yesNo(),
      assaultedStaffName = referral.assaultedStaffName,
      incidentInvolvement = referral.incidentInvolvement?.description,
      descriptionOfConcern = referral.descriptionOfConcern,
      knownReasons = referral.knownReasons,
      otherInformation = referral.otherInformation,
      saferCustodyTeamInformed = referral.saferCustodyTeamInformed.label(),
      referralCompletedByLastName = surname(referral.referralCompletedByDisplayName, referral.referralCompletedBy),
      referralCompletedDate = referral.referralCompletedDate,
      contributoryFactors = referral.contributoryFactors()
        .sortedWith(compareBy<ContributoryFactor> { it.contributoryFactorType.description }.thenBy { it.id })
        .map(SarContributoryFactor::from),
      saferCustodyScreeningOutcome = referral.saferCustodyScreeningOutcome?.let(SarScreeningOutcome::from),
      investigation = referral.investigation?.let(SarInvestigation::from),
      decisionAndActions = referral.decisionAndActions?.let(SarDecisionAndActions::from),
    )

    private val TIME = DateTimeFormatter.ofPattern("HH:mm")
  }
}

@Schema(description = "Something identified at referral as contributing to the prisoner's behaviour")
data class SarContributoryFactor(
  @Schema(example = "Debt")
  val factorType: String,
  val comment: String?,
) {
  companion object {
    fun from(factor: ContributoryFactor) = SarContributoryFactor(
      factorType = factor.contributoryFactorType.description,
      comment = factor.comment,
    )
  }
}

@Schema(description = "The Safer Custody team's screening of the referral")
data class SarScreeningOutcome(
  @Schema(example = "Progress to investigation")
  val outcome: String,
  val date: LocalDate,
  val reasonForDecision: String?,
  @Schema(example = "Smith")
  val recordedByLastName: String?,
) {
  companion object {
    fun from(screening: SaferCustodyScreeningOutcome) = SarScreeningOutcome(
      outcome = screening.outcome.description,
      date = screening.date,
      reasonForDecision = screening.reasonForDecision,
      recordedByLastName = surname(screening.recordedByDisplayName, screening.recordedBy),
    )
  }
}

@Schema(description = "The investigation into the referral")
data class SarInvestigation(
  @Schema(description = "Staff involved, as typed in")
  val staffInvolved: String?,
  val evidenceSecured: String?,
  val occurrenceReason: String?,
  val personsUsualBehaviour: String?,
  val personsTrigger: String?,
  val protectiveFactors: String?,
  @Schema(example = "Smith")
  val recordedByLastName: String?,
  val interviews: List<SarInterview>,
) {
  companion object {
    fun from(investigation: Investigation) = SarInvestigation(
      staffInvolved = investigation.staffInvolved,
      evidenceSecured = investigation.evidenceSecured,
      occurrenceReason = investigation.occurrenceReason,
      personsUsualBehaviour = investigation.personsUsualBehaviour,
      personsTrigger = investigation.personsTrigger,
      protectiveFactors = investigation.protectiveFactors,
      recordedByLastName = surname(investigation.recordedByDisplayName, investigation.recordedBy),
      interviews = investigation.interviews()
        .sortedWith(compareBy<Interview> { it.interviewDate }.thenBy { it.id })
        .map(SarInterview::from),
    )
  }
}

@Schema(description = "An interview held as part of the investigation")
data class SarInterview(
  @Schema(description = "Who was interviewed, as typed in")
  val interviewee: String,
  val interviewDate: LocalDate,
  @Schema(example = "Witness")
  val intervieweeRole: String,
  val interviewText: String?,
) {
  companion object {
    fun from(interview: Interview) = SarInterview(
      interviewee = interview.interviewee,
      interviewDate = interview.interviewDate,
      intervieweeRole = interview.intervieweeRole.description,
      interviewText = interview.interviewText,
    )
  }
}

@Schema(description = "The decision made after the investigation, and the actions agreed")
data class SarDecisionAndActions(
  @Schema(example = "Open a CSIP")
  val outcome: String?,
  val conclusion: String?,
  @Schema(description = "The role of the person who signed off the decision", example = "Custodial manager")
  val signedOffByRole: String?,
  @Schema(example = "Smith")
  val recordedByLastName: String?,
  val date: LocalDate?,
  val nextSteps: String?,
  val actions: List<String>,
  val actionOther: String?,
) {
  companion object {
    fun from(decision: DecisionAndActions) = SarDecisionAndActions(
      outcome = decision.outcome?.description,
      conclusion = decision.conclusion,
      signedOffByRole = decision.signedOffBy?.description,
      recordedByLastName = surname(decision.recordedByDisplayName, decision.recordedBy),
      date = decision.date,
      nextSteps = decision.nextSteps,
      actions = decision.actions.map { it.label() },
      actionOther = decision.actionOther,
    )
  }
}

@Schema(description = "The plan made to support the prisoner")
data class SarPlan(
  @Schema(description = "The case manager, as typed in")
  val caseManager: String?,
  val reasonForPlan: String?,
  val firstCaseReviewDate: LocalDate?,
  val identifiedNeeds: List<SarIdentifiedNeed>,
  val reviews: List<SarReview>,
) {
  companion object {
    fun from(plan: Plan) = SarPlan(
      caseManager = plan.caseManager,
      reasonForPlan = plan.reasonForPlan,
      firstCaseReviewDate = plan.firstCaseReviewDate,
      identifiedNeeds = plan.identifiedNeeds()
        .sortedWith(compareBy<IdentifiedNeed> { it.createdDate }.thenBy { it.targetDate }.thenBy { it.id })
        .map(SarIdentifiedNeed::from),
      reviews = plan.reviews().sortedBy { it.reviewSequence }.map(SarReview::from),
    )
  }
}

@Schema(description = "A need identified in the plan, and how it is being met")
data class SarIdentifiedNeed(
  val identifiedNeed: String,
  @Schema(description = "Who is responsible for it, as typed in")
  val responsiblePerson: String,
  val createdDate: LocalDate,
  val targetDate: LocalDate,
  val closedDate: LocalDate?,
  val intervention: String,
  val progression: String?,
) {
  companion object {
    fun from(need: IdentifiedNeed) = SarIdentifiedNeed(
      identifiedNeed = need.identifiedNeed,
      responsiblePerson = need.responsiblePerson,
      createdDate = need.createdDate,
      targetDate = need.targetDate,
      closedDate = need.closedDate,
      intervention = need.intervention,
      progression = need.progression,
    )
  }
}

@Schema(description = "A review of the plan")
data class SarReview(
  val reviewDate: LocalDate,
  @Schema(example = "Smith")
  val recordedByLastName: String?,
  val summary: String?,
  val actions: List<String>,
  val nextReviewDate: LocalDate?,
  val csipClosedDate: LocalDate?,
  val attendees: List<SarAttendee>,
) {
  companion object {
    fun from(review: Review) = SarReview(
      reviewDate = review.reviewDate,
      recordedByLastName = surname(review.recordedByDisplayName, review.recordedBy),
      summary = review.summary,
      actions = review.actions.map { it.label() },
      nextReviewDate = review.nextReviewDate,
      csipClosedDate = review.csipClosedDate,
      attendees = review.attendees()
        .sortedWith(compareBy<Attendee, String?>(nullsLast()) { it.name?.lowercase() }.thenBy { it.id })
        .map(SarAttendee::from),
    )
  }
}

@Schema(description = "Someone invited to a review, and what they contributed")
data class SarAttendee(
  @Schema(description = "As typed in")
  val name: String?,
  @Schema(description = "As typed in")
  val role: String?,
  @Schema(example = "Yes")
  val attended: String?,
  val contribution: String?,
) {
  companion object {
    fun from(attendee: Attendee) = SarAttendee(
      name = attendee.name,
      role = attendee.role,
      attended = attendee.attended.yesNo(),
      contribution = attendee.contribution,
    )
  }
}

/**
 * The surname of a member of staff, from the name the service recorded for them at the time.
 *
 * Names recorded in DPS are "First Last"; names from NOMIS can be in capitals. The surname is the last word, so
 * "Mary Jane Smith" gives "Smith" and "Anne-Marie van der Berg" gives "Berg" - a compound surname written with
 * spaces loses its first part, which is the price of not showing first names. A name all in capitals, or all in
 * lower case, is given ordinary capitalisation, as in the incident reporting report: "O'NEILL" becomes "O'Neill".
 * A name with any mix of cases was typed that way deliberately and is left alone.
 *
 * Changes synced from NOMIS can record the username as the display name, so a "name" matching [username] gives
 * nothing: a username is not sent, and the report shows no name rather than a staff login.
 */
internal fun surname(displayName: String?, username: String?): String? {
  val name = displayName?.trim()?.takeIf { it.isNotEmpty() && !it.equals(username?.trim(), ignoreCase = true) } ?: return null
  val last = name.split(Regex("""\s+""")).last()
  if (last.any { it.isUpperCase() } && last.any { it.isLowerCase() }) {
    return last
  }
  return last.lowercase().replace(Regex("""(^|['-])(\p{L})""")) { it.value.uppercase() }
}

private fun Boolean?.yesNo() = this?.let { if (it) "Yes" else "No" }

internal fun OptionalYesNoAnswer.label() = when (this) {
  OptionalYesNoAnswer.YES -> "Yes"
  OptionalYesNoAnswer.NO -> "No"
  OptionalYesNoAnswer.DO_NOT_KNOW -> "Do not know"
}

/** Wording for the decision actions, following the list the CSIP service shows staff when a decision is recorded. */
internal fun DecisionAction.label() = when (this) {
  DecisionAction.OPEN_CSIP_ALERT -> "Open a CSIP alert"
  DecisionAction.NON_ASSOCIATIONS_UPDATED -> "Update non-associations"
  DecisionAction.OBSERVATION_BOOK -> "Make an entry in the observation book"
  DecisionAction.UNIT_OR_CELL_MOVE -> "Unit or cell move"
  DecisionAction.CSRA_OR_RSRA_REVIEW -> "CSRA or RSRA (Room Sharing Risk Assessment) review"
  DecisionAction.SERVICE_REFERRAL -> "Service referral"
  DecisionAction.SIM_REFERRAL -> "SIM referral"
}

internal fun ReviewAction.label() = when (this) {
  ReviewAction.RESPONSIBLE_PEOPLE_INFORMED -> "People responsible for the prisoner informed"
  ReviewAction.CSIP_UPDATED -> "CSIP updated"
  ReviewAction.REMAIN_ON_CSIP -> "Keep the prisoner on the plan"
  ReviewAction.CASE_NOTE -> "Case note made"
  ReviewAction.CLOSE_CSIP -> "Close the CSIP"
}
