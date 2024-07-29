package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome as ScreeningOutcomeModel

fun SaferCustodyScreeningOutcome.toModel() = ScreeningOutcomeModel(
  outcome = outcomeType.toReferenceDataModel(),
  recordedBy = recordedBy,
  recordedByDisplayName = recordedByDisplayName,
  date = date,
  reasonForDecision = reasonForDecision,
)

fun CreateSaferCustodyScreeningOutcomeRequest.toCsipRecordEntity(
  context: CsipRequestContext,
  referral: Referral,
  outcomeType: ReferenceData,
) = referral.createSaferCustodyScreeningOutcome(
  context = context,
  outcomeType = outcomeType,
  date = date,
  reasonForDecision = reasonForDecision,
)
