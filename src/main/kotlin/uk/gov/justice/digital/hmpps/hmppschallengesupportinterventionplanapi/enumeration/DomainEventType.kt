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
    "A Contributory factor has been created in the CSIP service",
  ),
  CONTRIBUTORY_FACTOR_DELETED(
    "person.csip.contributory-factor.created",
    "A Contributory factor has been deleted in the CSIP service",
  ),
  INTERVIEW_CREATED("person.csip.interview.created", "An interview record has been created in the CSIP service"),
  INTERVIEW_DELETED("person.csip.interview.deleted", "An interview record has been deleted in the CSIP service"),

  IDENTIFIED_NEED_DELETED(
      "person.csip.identified-need.deleted",
      "An identified need record has been deleted in the CSIP service",
  ),
  ATTENDEE_DELETED("person.csip.attendee.deleted", "An attendee record has been deleted in the CSIP service"),
  REVIEW_DELETED("person.csip.review.deleted", "A review record has been deleted in the CSIP service"),
}
