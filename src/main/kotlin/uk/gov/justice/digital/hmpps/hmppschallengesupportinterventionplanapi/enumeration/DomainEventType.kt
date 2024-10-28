package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

const val CSIP_DOMAIN_EVENT_PREFIX = "person.csip-record."

enum class DomainEventType(
  val eventType: String,
  val description: String,
) {
  CSIP_CREATED("${CSIP_DOMAIN_EVENT_PREFIX}created", "A CSIP record has been created in the CSIP service"),
  CSIP_UPDATED("${CSIP_DOMAIN_EVENT_PREFIX}updated", "A CSIP record has been updated in the CSIP service"),
  CSIP_DELETED("${CSIP_DOMAIN_EVENT_PREFIX}deleted", "A CSIP record has been deleted in the CSIP service"),
  CSIP_MOVED("${CSIP_DOMAIN_EVENT_PREFIX}moved", "A CSIP record has been moved in the CSIP service"),
}
