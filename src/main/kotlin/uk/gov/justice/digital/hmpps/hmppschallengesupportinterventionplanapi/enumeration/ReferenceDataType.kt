package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration

import jakarta.validation.ValidationException

enum class ReferenceDataType(val domain: String) {
  AreaOfWork("area-of-work"), // Map to NOMIS domain CSIP_FUNC
  ContributoryFactorType("contributory-factor-type"), // Map to NOMIS domain CSIP_FAC
  DecisionSignerRole("role"), // Map to NOMIS domain CSIP_ROLE
  IncidentInvolvement("incident-involvement"), // Map to NOMIS domain CSIP_INV
  IncidentLocation("incident-location"), // Map to NOMIS domain CSIP_LOC
  IncidentType("incident-type"), // Map to NOMIS domain CSIP_TYP
  IntervieweeRole("interviewee-role"), // Map to NOMIS domain CSIP_INTVROL
  OutcomeType("outcome-type"), // Map to NOMIS domain CSIP_OUT
  ;

  companion object {
    const val VALIDATION_DESCRIPTION = "Reference Data domain name must be one of: area-of-work, " +
      "contributory-factor-type, role, incident-involvement, incident-location, incident-type, " +
      "interviewee-role, or outcome-type"
    private val map = entries.associateBy(ReferenceDataType::domain)

    fun fromDomain(domain: String) =
      map[domain] ?: throw ValidationException("Fail to map $domain to Reference Data Type. $VALIDATION_DESCRIPTION")
  }
}
