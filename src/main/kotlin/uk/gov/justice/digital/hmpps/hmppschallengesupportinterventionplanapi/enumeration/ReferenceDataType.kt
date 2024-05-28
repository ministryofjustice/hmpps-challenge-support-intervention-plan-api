package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import io.swagger.v3.oas.annotations.media.Schema
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.exception.InvalidDomainException

@Schema(
  type = "String",
  allowableValues = [
    "area-of-work",
    "contributory-factor-type",
    "role",
    "incident-involvement",
    "incident-location",
    "incident-type",
    "interviewee-role",
    "outcome-type",
  ],
)
enum class ReferenceDataType(val domain: String) {
  AREA_OF_WORK("area-of-work"), // Map to NOMIS domain CSIP_FUNC
  CONTRIBUTORY_FACTOR_TYPE("contributory-factor-type"), // Map to NOMIS domain CSIP_FAC
  DECISION_SIGNER_ROLE("role"), // Map to NOMIS domain CSIP_ROLE
  INCIDENT_INVOLVEMENT("incident-involvement"), // Map to NOMIS domain CSIP_INV
  INCIDENT_LOCATION("incident-location"), // Map to NOMIS domain CSIP_LOC
  INCIDENT_TYPE("incident-type"), // Map to NOMIS domain CSIP_TYP
  INTERVIEWEE_ROLE("interviewee-role"), // Map to NOMIS domain CSIP_INTVROL
  OUTCOME_TYPE("outcome-type"), // Map to NOMIS domain CSIP_OUT
  ;

  companion object {
    const val VALIDATION_DESCRIPTION =
      "Reference Data domain name must be one of: area-of-work, " + "contributory-factor-type, role, incident-involvement, incident-location, incident-type, " + "interviewee-role, or outcome-type"
    private val map = entries.associateBy(ReferenceDataType::domain)

    fun fromDomain(domain: String) =
      map[domain] ?: throw InvalidDomainException("Fail to map $domain to Reference Data Type. $VALIDATION_DESCRIPTION")
  }
}
