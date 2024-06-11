package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

enum class DomainEventType(
  val eventType: String,
  val description: String,
) {
  CSIP_CREATED("prisoner-csip.csip-record-created", "A CSIP record has been created in the CSIP service"),
  CSIP_UPDATED("prisoner-csip.csip-record-updated", "A CSIP record has been updated in the CSIP service"),
  CONTRIBUTORY_FACTOR_CREATED("prisoner-csip.contributory-factor-created", "A Contributory factor has been created in the CSIP service"),
  INTERVIEW_CREATED("prisoner-csip.interview-created", "A Interview record has been created in the CSIP service"),
}
