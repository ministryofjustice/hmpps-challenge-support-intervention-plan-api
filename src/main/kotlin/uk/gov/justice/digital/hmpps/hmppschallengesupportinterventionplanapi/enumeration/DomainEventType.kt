package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

enum class DomainEventType(
  val eventType: String,
  val description: String,
) {
  CSIP_CREATED("person.csip.record.created", "A CSIP record has been created in the CSIP service"),
  CSIP_UPDATED("person.csip.record.updated", "A CSIP record has been updated in the CSIP service"),
  CSIP_DELETED("person.csip.record.deleted", "A CSIP record has been deleted in the CSIP service"),
  CONTRIBUTORY_FACTOR_CREATED(
    "person.csip.contributory-factor.created",
    "A contributory factor has been created in the CSIP service",
  ),
  CONTRIBUTORY_FACTOR_UPDATED(
    "person.csip.contributory-factor.updated",
    "A contributory factor has been updated in the CSIP service",
  ),
  CONTRIBUTORY_FACTOR_DELETED(
    "person.csip.contributory-factor.deleted",
    "A contributory factor has been deleted in the CSIP service",
  ),
  INTERVIEW_CREATED("person.csip.interview.created", "An interview record has been created in the CSIP service"),
  INTERVIEW_UPDATED("person.csip.interview.updated", "An interview record has been updated in the CSIP service"),
  INTERVIEW_DELETED("person.csip.interview.deleted", "An interview record has been deleted in the CSIP service"),
  IDENTIFIED_NEED_CREATED(
    "person.csip.identified-need.created",
    "An identified need record has been created in the CSIP service",
  ),
  IDENTIFIED_NEED_UPDATED(
    "person.csip.identified-need.updated",
    "An identified need record has been updated in the CSIP service",
  ),
  IDENTIFIED_NEED_DELETED(
    "person.csip.identified-need.deleted",
    "An identified need record has been deleted in the CSIP service",
  ),
  REVIEW_CREATED("person.csip.review.created", "A review record has been created in the CSIP service"),
  REVIEW_UPDATED("person.csip.review.updated", "A review record has been updated in the CSIP service"),
  REVIEW_DELETED("person.csip.review.deleted", "A review record has been deleted in the CSIP service"),
  ATTENDEE_CREATED("person.csip.attendee.created", "An attendee record has been created in the CSIP service"),
  ATTENDEE_UPDATED("person.csip.attendee.updated", "An attendee record has been updated in the CSIP service"),
  ATTENDEE_DELETED("person.csip.attendee.deleted", "An attendee record has been deleted in the CSIP service"),
  ;

  companion object {
    fun fromEventName(eventName: String): DomainEventType? = entries.find { it.eventType == eventName }
  }
}

enum class PersistenceAction {
  CREATED,
  UPDATED,
  DELETED,
}
