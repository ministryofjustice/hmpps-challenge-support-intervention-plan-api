package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome as ScreeningOutcomeModel

fun SaferCustodyScreeningOutcome.toModel() = ScreeningOutcomeModel(
  outcome = outcomeType.toReferenceDataModel(),
  recordedBy = recordedBy,
  recordedByDisplayName = recordedByDisplayName,
  date = date,
  reasonForDecision = reasonForDecision,
)
