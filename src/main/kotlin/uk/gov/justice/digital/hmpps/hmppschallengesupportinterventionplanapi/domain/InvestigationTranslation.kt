package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.UpsertInvestigationRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation as InvestigationModel

fun Investigation.toModel() = InvestigationModel(
  staffInvolved = staffInvolved,
  evidenceSecured = evidenceSecured,
  occurrenceReason = occurrenceReason,
  personsUsualBehaviour = personsUsualBehaviour,
  personsTrigger = personsTrigger,
  protectiveFactors = protectiveFactors,
  interviews = interviews().map { it.toModel() },
)

fun UpsertInvestigationRequest.toCsipRecordEntity(
  context: CsipRequestContext,
  referral: Referral,
  roleProvider: (Set<String>) -> Map<String, ReferenceData>,
): CsipRecord = referral.upsertInvestigation(
  context = context,
  request = this,
)
