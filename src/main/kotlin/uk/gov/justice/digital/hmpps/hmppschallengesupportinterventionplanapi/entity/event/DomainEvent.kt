package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.event

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Reason.USER
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.util.UUID

abstract class DomainEvent<T : AdditionalInformation> {
  abstract val eventType: String
  abstract val additionalInformation: T
  abstract val version: Int
  abstract val description: String
  abstract val occurredAt: String

  override fun toString(): String {
    return "v$version domain event '$eventType' " +
      "for resource '${additionalInformation.url}' " +
      "from source '${additionalInformation.source}' " +
      "with reason '${additionalInformation.reason}'"
  }
}

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
  JsonSubTypes.Type(value = CsipAdditionalInformation::class, name = "csip"),
  JsonSubTypes.Type(value = ReferenceDataAdditionalInformation::class, name = "csipReferenceData"),
)
abstract class AdditionalInformation {
  abstract val url: String
  abstract val source: Source
  abstract val reason: Reason
  abstract fun asString(): String
  abstract fun identifier(): String
}

data class CsipDomainEvent(
  override val eventType: String,
  override val additionalInformation: AdditionalInformation,
  override val version: Int = 1,
  override val description: String,
  override val occurredAt: String,
) : DomainEvent<AdditionalInformation>() {
  override fun toString(): String {
    return "v$version CSIP domain event '$eventType' " + additionalInformation.asString()
  }
}

data class CsipAdditionalInformation(
  override val url: String,
  val recordUuid: UUID,
  val prisonNumber: String,
  @JsonProperty("recordAffected")
  val isRecordAffected: Boolean,
  @JsonProperty("referralAffected")
  val isReferralAffected: Boolean,
  @JsonProperty("contributoryFactorAffected")
  val isContributoryFactorAffected: Boolean,
  @JsonProperty("saferCustodyScreeningOutcomeAffected")
  val isSaferCustodyScreeningOutcomeAffected: Boolean,
  @JsonProperty("investigationAffected")
  val isInvestigationAffected: Boolean,
  @JsonProperty("interviewAffected")
  val isInterviewAffected: Boolean,
  @JsonProperty("decisionAndActionsAffected")
  val isDecisionAndActionsAffected: Boolean,
  @JsonProperty("planAffected")
  val isPlanAffected: Boolean,
  @JsonProperty("identifiedNeedAffected")
  val isIdentifiedNeedAffected: Boolean,
  @JsonProperty("reviewAffected")
  val isReviewAffected: Boolean,
  @JsonProperty("attendeeAffected")
  val isAttendeeAffected: Boolean,
  override val source: Source,
  override val reason: Reason,
) : AdditionalInformation() {
  override fun asString(): String =
    "for CSIP record with UUID '$recordUuid' " +
      "for prison number '$prisonNumber' " +
      "from source '$source' " +
      "with reason '$reason'"

  override fun identifier(): String = recordUuid.toString()
}

data class ReferenceDataAdditionalInformation(
  override val url: String,
  val domain: String,
  val code: String,
  override val source: Source,
  override val reason: Reason = USER,
) : AdditionalInformation() {
  override fun identifier(): String = "$domain-$code"

  override fun asString(): String =
    "for reference code '$code' in domain '$domain'" +
      "from source '$source' " +
      "with reason '$reason'"
}
