package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.referenceData.ReferenceData
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.UUID

class CsipRecordTest : ModelTest() {
  @Test
  fun `should serialize and deserialize CsipRecord`() {
    val originalDate = LocalDate.parse("01 Feb 2023", dateFormatter)
    val originalTime = LocalTime.parse("10:21:22", timeFormatter)
    val originalDateTime = LocalDateTime.parse("31 Jan 2023 09:01:02", dateTimeFormatter)

    val expectedDate = "2023-02-01"
    val expectTime = "10:21:22"
    val expectedDateTime = "2023-01-31T09:01:02"

    val referenceData = ReferenceData("code", "desciption", 0, originalDateTime, "", null, null, null, null)
    val csipRecord = CsipRecord(
      recordUuid = UUID.randomUUID(),
      prisonNumber = "ABC12345",
      prisonCodeWhenRecorded = "ABC",
      logNumber = "LOG001",
      createdAt = originalDateTime,
      createdBy = "user_name",
      createdByDisplayName = "User Name",
      lastModifiedAt = originalDateTime,
      lastModifiedBy = "user_name",
      lastModifiedByDisplayName = "User Name",
      referral = Referral(
        incidentDate = originalDate,
        incidentTime = originalTime,
        incidentType = referenceData,
        incidentLocation = referenceData,
        referredBy = "referer_name",
        refererArea = referenceData,
        referralSummary = null,
        isStaffAssaulted = true,
        isProactiveReferral = false,
        isReferralComplete = true,
        isSaferCustodyTeamInformed = true,
        knownReasons = "reasons",
        descriptionOfConcern = "",
        incidentInvolvement = referenceData,
        assaultedStaffName = "staff name 1, staff name 2",
        releaseDate = LocalDate.of(2021, 11, 1),
        otherInformation = "",
        contributoryFactors = listOf(
          ContributoryFactor(
            factorUuid = UUID.randomUUID(),
            factorType = referenceData,
            comment = null,
            createdAt = originalDateTime,
            createdBy = "nisl",
            createdByDisplayName = "Rosalind Cervantes",
            lastModifiedAt = null,
            lastModifiedBy = null,
            lastModifiedByDisplayName = null,
          ),
        ),
        decisionAndActions = DecisionAndActions(
          conclusion = null,
          outcome = referenceData,
          outcomeSignedOffByRole = null,
          outcomeRecordedBy = null,
          outcomeRecordedByDisplayName = null,
          outcomeDate = originalDate,
          nextSteps = null,
          isActionOpenCsipAlert = false,
          isActionNonAssociationsUpdated = false,
          isActionObservationBook = false,
          isActionUnitOrCellMove = false,
          isActionCsraOrRsraReview = false,
          isActionServiceReferral = false,
          isActionSimReferral = false,
          actionOther = null,
        ),
        saferCustodyScreeningOutcome = SaferCustodyScreeningOutcome(
          outcome = referenceData,
          recordedBy = "cubilia",
          recordedByDisplayName = "Omar Dotson",
          date = originalDate,
          reasonForDecision = "mazim",
        ),
        investigation = Investigation(
          staffInvolved = null,
          evidenceSecured = null,
          occurrenceReason = null,
          personsUsualBehaviour = null,
          personsTrigger = null,
          protectiveFactors = null,
          interviews = listOf(
            Interview(
              interviewUuid = UUID.randomUUID(),
              interviewee = "",
              interviewDate = originalDate,
              intervieweeRole = referenceData,
              interviewText = null,
              createdAt = originalDateTime,
              createdBy = "",
              createdByDisplayName = "",
              lastModifiedAt = null,
              lastModifiedBy = null,
              lastModifiedByDisplayName = null,
            ),
          ),
        ),
      ),
      plan = Plan(
        caseManager = "pro",
        reasonForPlan = "viverra",
        firstCaseReviewDate = originalDate,
        identifiedNeeds = listOf(
          IdentifiedNeed(
            identifiedNeedUuid = UUID.randomUUID(),
            identifiedNeed = "fringilla",
            needIdentifiedBy = "dolor",
            createdDate = originalDate,
            targetDate = originalDate,
            closedDate = null,
            intervention = "tractatos",
            progression = null,
            createdAt = originalDateTime,
            createdBy = "deserunt",
            createdByDisplayName = "Lewis Pate",
            lastModifiedAt = null,
            lastModifiedBy = null,
            lastModifiedByDisplayName = null,
          ),
        ),
        reviews = listOf(
          Review(
            reviewUuid = UUID.randomUUID(),
            reviewSequence = 7410,
            reviewDate = null,
            recordedBy = "periculis",
            recordedByDisplayName = "Vickie Head",
            nextReviewDate = null,
            isActionResponsiblePeopleInformed = null,
            isActionCsipUpdated = null,
            isActionRemainOnCsip = null,
            isActionCaseNote = null,
            isActionCloseCsip = null,
            csipClosedDate = null,
            summary = null,
            createdAt = originalDateTime,
            createdBy = "interdum",
            createdByDisplayName = "Marsha Barr",
            lastModifiedAt = null,
            lastModifiedBy = null,
            lastModifiedByDisplayName = null,
            attendees = listOf(
              Attendee(
                attendeeUuid = UUID.randomUUID(),
                name = null,
                role = null,
                isAttended = null,
                contribution = null,
                createdAt = originalDateTime,
                createdBy = "movet",
                createdByDisplayName = "Joy Atkinson",
                lastModifiedAt = null,
                lastModifiedBy = null,
                lastModifiedByDisplayName = null,
              ),
            ),
          ),
        ),
      ),
    )

    val json = objectMapper.writeValueAsString(csipRecord)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    val referralMap = jsonMap["referral"] as Map<*, *>

    assertThat(jsonMap["createdAt"]).isEqualTo(expectedDateTime)
    assertThat(referralMap["incidentDate"]).isEqualTo(expectedDate)
    assertThat(referralMap["incidentTime"]).isEqualTo(expectTime)

    val testSerialize = objectMapper.readValue(json, CsipRecord::class.java)
    assertThat(testSerialize).isEqualTo(csipRecord)
  }
}
