package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sar

import jakarta.persistence.EntityManager
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.data.repository.findByIdOrNull
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.referral.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.toPersonSummary
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.DecisionAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.OptionalYesNoAnswer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.CONTRIBUTORY_FACTOR_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.DECISION_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.DECISION_SIGNER_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_INVOLVEMENT
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_LOCATION
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INCIDENT_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.INTERVIEWEE_ROLE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataType.SCREENING_OUTCOME_TYPE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReviewAction
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referral.request.UpdateInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.EntityGenerator.generateCsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.prisoner
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.set
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarFlywaySchemaTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelperConfig
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import java.time.LocalDate
import java.time.LocalTime
import javax.sql.DataSource

/**
 * The four checks from the HMPPS SAR test library, against a fixed set of CSIPs.
 *
 * These are approval tests: each compares against a committed file under `src/test/resources/sar/`, and the point
 * of them is what happens when one fails.
 *
 *  - `SarApiDataTest` / `SarReportTest` fail when the response or the rendered report changes. The rendered HTML is
 *    the artefact the Offender SAR team review and sign off (IR-2067), and no template change reaches preprod or prod
 *    without that sign-off. So a diff here is the trigger for a conversation with them through the SAR change control
 *    process, not something to regenerate and move on from. `SarReportTest` renders the template served by
 *    `GET /subject-access-request/template`, so it also proves the endpoint is switched on.
 *  - `SarJpaEntitiesTest` fails when a column is added to any entity, and `SarFlywaySchemaTest` when a migration is
 *    added. Both force whoever makes the change to decide whether it belongs in a prisoner's report - and to tag the
 *    new column in the SAR data requirements extract - rather than the field quietly never appearing.
 *
 * Regenerate the files by running with `SAR_GENERATE_ACTUAL=true`, which writes `*.log` files under
 * `src/test/resources/`, then read them before copying them over the approved ones.
 */
@Import(SarIntegrationTestHelperConfig::class)
class SubjectAccessRequestLibraryTest :
  IntegrationTestBase(),
  SarApiDataTest,
  SarFlywaySchemaTest,
  SarJpaEntitiesTest,
  SarReportTest {

  @Autowired
  private lateinit var dataSource: DataSource

  @Autowired
  private lateinit var sarIntegrationTestHelper: SarIntegrationTestHelper

  override fun getDataSourceInstance(): DataSource = dataSource

  override fun getEntityManagerInstance(): EntityManager = entityManager

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getPrn(): String = PRISONER

  /**
   * Deserialise the response as plain JSON rather than into the response classes. That is what the SAR tool itself
   * does - it fetches the JSON and renders the template against the parsed result - so the template helpers see the
   * same strings they see in production.
   */
  override fun getContentType(): Class<*> = List::class.java

  /**
   * The test library asks for "a complete representation of all possible data", because anything not represented
   * here is a shape of report nobody has ever looked at. Between them these CSIPs exercise every field in the
   * response and every branch in the template:
   *  - a CSIP that went the whole way, from referral to a closed plan: two contributory factors, screening, an
   *    investigation with two interviews, a decision with actions, two identified needs and two reviews with people
   *    at them. The member of staff who completed the referral is recorded in capitals, as NOMIS records names, so
   *    the approved report shows their surname given ordinary capitalisation;
   *  - an older referral with no log code or prison, that ended at screening with no further action, so every empty
   *    section shows;
   *  - a CSIP for another prisoner, which must not appear at all.
   *
   * Dates are fixed so the approved files stay stable. Idempotent, because the library calls this itself and the
   * records must not stack up.
   */
  override fun setupTestData() {
    transactionTemplate.executeWithoutResult {
      csipRecordRepository.deleteAll(
        csipRecordRepository.findByPrisonNumber(PRISONER) + csipRecordRepository.findByPrisonNumber(OTHER_PRISONER),
      )
    }
    closedCsip()
    noFurtherActionReferral()
    otherPrisonersCsip()
  }

  private fun closedCsip() {
    val record = generateCsipRecord(
      personSummary = person(PRISONER, "Sam", "Smith"),
      prisonCodeWhenRecorded = "MDI",
      logCode = "MDI-0042",
    )
    dataSetup(record) {
      it.withReferral(
        incidentType = { givenReferenceData(INCIDENT_TYPE, "ATS") },
        incidentLocation = { givenReferenceData(INCIDENT_LOCATION, "EXY") },
        incidentInvolvement = { givenReferenceData(INCIDENT_INVOLVEMENT, "PER") },
        incidentDate = LocalDate.parse("2025-03-08"),
        incidentTime = LocalTime.parse("14:30"),
        referredBy = "Officer Jane O'Neill",
        referralDate = LocalDate.parse("2025-03-10"),
        proactiveReferral = false,
        staffAssaulted = true,
        assaultedStaffName = "Officer P Green",
        descriptionOfConcern = "Shouted threats at an officer on the exercise yard and pushed them.",
        knownReasons = "Upset after a visit was cancelled.",
        otherInformation = "Has asked to speak to the chaplain.",
        saferCustodyTeamInformed = OptionalYesNoAnswer.YES,
        referralComplete = true,
        referralCompletedBy = "JONEILL_GEN",
        referralCompletedByDisplayName = "JANE O'NEILL",
        referralCompletedDate = LocalDate.parse("2025-03-10"),
      )
      val referral = requireNotNull(it.referral)
        .withContributoryFactor(
          type = givenReferenceData(CONTRIBUTORY_FACTOR_TYPE, "BAS"),
          comment = "Had no kettle in their cell for a week.",
        )
        .withContributoryFactor(type = givenReferenceData(CONTRIBUTORY_FACTOR_TYPE, "AFL"), comment = null)
        .withSaferCustodyScreeningOutcome(
          outcome = givenReferenceData(SCREENING_OUTCOME_TYPE, "OPE"),
          recordedBy = "JSMITH_GEN",
          recordedByDisplayName = "John Smith",
          date = LocalDate.parse("2025-03-11"),
          reasonForDecision = "Second incident this month. Needs looking into.",
        )
        .withDecisionAndActions(
          outcome = givenReferenceData(DECISION_OUTCOME_TYPE, "CUR"),
          signedOffBy = givenReferenceData(DECISION_SIGNER_ROLE, "CUSTMAN"),
          conclusion = "A plan is needed to support contact with family.",
          recordedBy = "JSMITH_GEN",
          recordedByDisplayName = "John Smith",
          date = LocalDate.parse("2025-03-20"),
          nextSteps = "Case manager to meet the prisoner this week.",
          actions = sortedSetOf(DecisionAction.OPEN_CSIP_ALERT, DecisionAction.UNIT_OR_CELL_MOVE),
          actionOther = "Speak to the visits team.",
        )
      referral.set(
        referral::investigation,
        Investigation(referral, "AKHAN_GEN", "Amir Khan").update(
          UpdateInvestigationRequest(
            staffInvolved = "Officer Green, Officer O'Neill",
            evidenceSecured = "Body-worn camera footage.",
            occurrenceReason = "Frustration about the cancelled visit.",
            personsUsualBehaviour = "Usually calm and polite to staff.",
            personsTrigger = "Changes to family contact.",
            protectiveFactors = "Close to their sister. Works in the kitchen.",
          ),
        ),
      )
      requireNotNull(referral.investigation)
        .withInterview(
          interviewee = "Sam Smith",
          interviewDate = LocalDate.parse("2025-03-13"),
          intervieweeRole = givenReferenceData(INTERVIEWEE_ROLE, "PERP"),
          interviewText = "Said they were angry about the visit and are sorry.",
        )
        .withInterview(
          interviewee = "Joe Jones B2345SR",
          interviewDate = LocalDate.parse("2025-03-14"),
          intervieweeRole = givenReferenceData(INTERVIEWEE_ROLE, "WITNESS"),
          interviewText = "Saw the officer being pushed.",
        )

      it.withPlan(
        caseManager = "Supervising Officer Patel",
        reasonForPlan = "To support the prisoner to keep in touch with family.",
        firstCaseReviewDate = LocalDate.parse("2025-04-10"),
      )
      val plan = requireNotNull(it.plan)
        .withNeed(
          identifiedNeed = "Regular contact with family.",
          responsiblePerson = "Visits team",
          createdDate = LocalDate.parse("2025-03-21"),
          targetDate = LocalDate.parse("2025-04-21"),
          closedDate = LocalDate.parse("2025-05-08"),
          intervention = "Book a social video call each week.",
          progression = "Calls taking place each week.",
        )
        .withNeed(
          identifiedNeed = "Somewhere to talk when upset.",
          responsiblePerson = "Chaplaincy",
          createdDate = LocalDate.parse("2025-03-21"),
          targetDate = LocalDate.parse("2025-05-21"),
          closedDate = LocalDate.parse("2025-05-08"),
          intervention = "Weekly meeting with the chaplain.",
          progression = null,
        )
        .withReview(
          reviewDate = LocalDate.parse("2025-04-10"),
          recordedBy = "SPATEL_GEN",
          recordedByDisplayName = "Sunita Patel",
          nextReviewDate = LocalDate.parse("2025-05-08"),
          summary = "Going well. No further incidents.",
          actions = sortedSetOf(ReviewAction.RESPONSIBLE_PEOPLE_INFORMED, ReviewAction.REMAIN_ON_CSIP),
        )
        .withReview(
          reviewDate = LocalDate.parse("2025-05-08"),
          recordedBy = "SPATEL_GEN",
          recordedByDisplayName = "Sunita Patel",
          nextReviewDate = null,
          csipClosedDate = LocalDate.parse("2025-05-08"),
          summary = "Needs met. CSIP closed.",
          actions = sortedSetOf(ReviewAction.CASE_NOTE, ReviewAction.CLOSE_CSIP),
        )
      plan.reviews().first { r -> r.reviewSequence == 1 }
        .withAttendee(name = "Sam Smith", role = "Prisoner", attended = true, contribution = "Calls are helping.")
        .withAttendee(name = "Sister", role = "Family", attended = false, contribution = "Sent a letter of support.")
    }
  }

  private fun noFurtherActionReferral() {
    val record = generateCsipRecord(
      personSummary = person(PRISONER, "Sam", "Smith"),
    )
    dataSetup(record) {
      it.withReferral(
        incidentType = { givenReferenceData(INCIDENT_TYPE, "ATO") },
        incidentLocation = { givenReferenceData(INCIDENT_LOCATION, "EDU") },
        incidentDate = LocalDate.parse("2024-06-02"),
        referredBy = "Education",
        referralDate = LocalDate.parse("2024-06-03"),
        descriptionOfConcern = "Argument with another learner.",
        knownReasons = null,
        otherInformation = null,
        saferCustodyTeamInformed = OptionalYesNoAnswer.DO_NOT_KNOW,
      )
      requireNotNull(it.referral).withSaferCustodyScreeningOutcome(
        outcome = givenReferenceData(SCREENING_OUTCOME_TYPE, "NFA"),
        recordedBy = "JSMITH_GEN",
        recordedByDisplayName = "John Smith",
        date = LocalDate.parse("2024-06-04"),
        reasonForDecision = "A one-off, resolved on the day.",
      )
    }
  }

  private fun otherPrisonersCsip() {
    val record = generateCsipRecord(
      personSummary = person(OTHER_PRISONER, "Joe", "Jones"),
      prisonCodeWhenRecorded = "MDI",
      logCode = "MDI-0043",
    )
    dataSetup(record) {
      it.withReferral(
        incidentType = { givenReferenceData(INCIDENT_TYPE, "ATS") },
        incidentLocation = { givenReferenceData(INCIDENT_LOCATION, "EXY") },
        incidentDate = LocalDate.parse("2025-03-08"),
        referralDate = LocalDate.parse("2025-03-10"),
      )
    }
  }

  /** The prisoner's existing summary if an earlier CSIP created it, as a prison number has only one. */
  private fun person(prn: String, firstName: String, lastName: String) = personSummaryRepository.findByIdOrNull(prn)
    ?: prisoner(prisonerNumber = prn, firstName = firstName, lastName = lastName).toPersonSummary()

  private companion object {
    const val PRISONER = "A1234SR"
    const val OTHER_PRISONER = "B2345SR"
  }
}
