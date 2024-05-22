package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import java.time.LocalDateTime

class AuditEventTest : ModelTest() {
  @Test
  fun `should serialize and deserialize auditEvent`() {
    val originalDateTime = LocalDateTime.parse("31 Jan 2023 09:01:02", dateTimeFormatter)
    val expectedDateTime = "2023-01-31T09:01:02"

    val auditEvent = AuditEvent(
      action = AuditEventAction.CREATED,
      description = "csip record created",
      actionedAt = originalDateTime,
      actionedBy = "user_name",
      actionedByCapturedName = "User Name",
      isRecordAffected = true,
      isReviewAffected = false,
      isInterviewAffected = false,
      isAttendeeAffected = false,
      isReferralAffected = true,
      isPlanAffected = false,
      isInvestigationAffected = false,
      isDecisionsAndActionsAffected = false,
      isIdentifiedNeedAffected = false,
      isContributoryFactorAffected = true,
      isSaferCustodyScreeningOutcomeAffected = false,
    )

    val json = objectMapper.writeValueAsString(auditEvent)
    val jsonMap = objectMapper.readValue(json, Map::class.java)

    assertThat(jsonMap["actionedAt"]).isEqualTo(expectedDateTime)

    val testSerialize = objectMapper.readValue(json, AuditEvent::class.java)
    assertThat(testSerialize).isEqualTo(auditEvent)
  }
}
