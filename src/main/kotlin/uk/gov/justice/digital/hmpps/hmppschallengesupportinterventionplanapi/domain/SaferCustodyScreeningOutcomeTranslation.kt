package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.SaferCustodyScreeningOutcome
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateSaferCustodyScreeningOutcomeRequest
import java.time.LocalDateTime

fun SaferCustodyScreeningOutcome.toModel() =
  uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.SaferCustodyScreeningOutcome(
    outcome = outcomeType.toReferenceDataModel(),
    recordedBy = recordedBy,
    recordedByDisplayName = recordedByDisplayName,
    date = date,
    reasonForDecision = reasonForDecision,
  )

fun CreateSaferCustodyScreeningOutcomeRequest.toCsipRecordEntity(
  csipRecord: CsipRecord,
  outcomeType: ReferenceData,
  recordedAt: LocalDateTime = LocalDateTime.now(),
  recordedBy: String,
  recordedByDisplayName: String,
  source: Source,
  activeCaseLoadId: String?,
) =
  SaferCustodyScreeningOutcome(
    csipRecord = csipRecord,
    outcomeType = outcomeType,
    recordedBy = recordedBy,
    recordedByDisplayName = recordedByDisplayName,
    date = date,
    reasonForDecision = reasonForDecision,
  ).create(
    createdAt = recordedAt,
    createdBy = recordedBy,
    createdByDisplayName = recordedByDisplayName,
    source = source,
    activeCaseLoadId = activeCaseLoadId,
  )
