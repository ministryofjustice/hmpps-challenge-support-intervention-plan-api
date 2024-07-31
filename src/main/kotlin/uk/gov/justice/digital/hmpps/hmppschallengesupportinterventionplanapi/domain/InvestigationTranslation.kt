package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation as InvestigationModel
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation

fun Investigation.toModel() = InvestigationModel(
  staffInvolved = staffInvolved,
  evidenceSecured = evidenceSecured,
  occurrenceReason = occurrenceReason,
  personsUsualBehaviour = personsUsualBehaviour,
  personsTrigger = personsTrigger,
  protectiveFactors = protectiveFactors,
  interviews = interviews().map { it.toModel() },
)
