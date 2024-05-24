package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model

import com.fasterxml.jackson.annotation.JsonFormat
import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.AuditEventAction
import java.time.LocalDateTime

@Schema(description = "Audit Event of the actions on a CSIP record")
data class AuditEvent(
  @Schema(
    description = "The audit event type",
    example = "CREATED",
  )
  val action: AuditEventAction,

  @Schema(
    description = "A description of what has changed",
    example = "The Referral incident date was updated from 2012-02-03 to 2012-04-05",
  )
  val description: String,

  @Schema(
    description = "The date and time the action on the CSIP record is audited",
    example = "2021-09-27T14:19:25",
  )
  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
  val actionedAt: LocalDateTime,

  @Schema(
    description = "The username of the user who actioned on the CSIP Record",
    example = "USER1234",
  )
  val actionedBy: String,

  @Schema(
    description = "The displayable name of the user who actioned on the CSIP Record",
    example = "Firstname Lastname",
  )
  val actionedByCapturedName: String,

  @Schema(
    description = "If the root CSIP record entity is affected",
  )
  val isRecordAffected: Boolean?,

  @Schema(
    description = "If the Referral entity is affected",
  )
  val isReferralAffected: Boolean?,

  @Schema(
    description = "If a Contributory Factor entity is affected",
  )
  val isContributoryFactorAffected: Boolean?,

  @Schema(
    description = "If the Safer Custody Screening Outcome entity is affected",
  )
  val isSaferCustodyScreeningOutcomeAffected: Boolean?,

  @Schema(
    description = "If the Investigation entity is affected",
  )
  val isInvestigationAffected: Boolean?,

  @Schema(
    description = "If an Interview entity is affected",
  )
  val isInterviewAffected: Boolean?,

  @Schema(
    description = "If the Decision And Actions entity is affected",
  )
  val isDecisionAndActionsAffected: Boolean?,

  @Schema(
    description = "If the Plan entity is affected",
  )
  val isPlanAffected: Boolean?,

  @Schema(
    description = "If an Identified Need entity is affected",
  )
  val isIdentifiedNeedAffected: Boolean?,

  @Schema(
    description = "If a Review entity is affected",
  )
  val isReviewAffected: Boolean?,

  @Schema(
    description = "If an Attendee entity is affected",
  )
  val isAttendeeAffected: Boolean?,
)
