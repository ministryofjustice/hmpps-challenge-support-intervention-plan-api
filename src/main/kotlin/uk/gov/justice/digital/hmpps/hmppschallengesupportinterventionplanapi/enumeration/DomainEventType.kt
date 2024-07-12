package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

enum class DomainEventType(
  val eventType: String,
  val description: String,
) {
  CSIP_CREATED("person.csip.record.created", "A CSIP record has been created in the CSIP service"),
  CSIP_UPDATED("person.csip.record.updated", "A CSIP record has been updated in the CSIP service"),
  CONTRIBUTORY_FACTOR_CREATED(
    "person.csip.contributory-factor.created",
    "A Contributory factor has been created in the CSIP service",
  ),
  INTERVIEW_CREATED("person.csip.interview.updated", "A Interview record has been created in the CSIP service"),
}
