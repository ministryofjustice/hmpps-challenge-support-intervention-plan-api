package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import java.time.LocalDateTime
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome as ScreeningOutcomeModel

fun SaferCustodyScreeningOutcome.toModel() = ScreeningOutcomeModel(
  outcome = outcomeType.toReferenceDataModel(),
  recordedBy = recordedBy,
  recordedByDisplayName = recordedByDisplayName,
  date = date,
  reasonForDecision = reasonForDecision,
)

fun CreateSaferCustodyScreeningOutcomeRequest.toCsipRecordEntity(
  referral: Referral,
  outcomeType: ReferenceData,
  actionedAt: LocalDateTime = LocalDateTime.now(),
  actionedBy: String,
  actionedByDisplayName: String,
  source: Source,
  activeCaseLoadId: String?,
) = referral.createSaferCustodyScreeningOutcome(
  outcomeType = outcomeType,
  date = date,
  reasonForDecision = reasonForDecision,
  actionedAt = actionedAt,
  actionedBy = actionedBy,
  actionedByDisplayName = actionedByDisplayName,
  source = source,
  activeCaseLoadId = activeCaseLoadId,
)
