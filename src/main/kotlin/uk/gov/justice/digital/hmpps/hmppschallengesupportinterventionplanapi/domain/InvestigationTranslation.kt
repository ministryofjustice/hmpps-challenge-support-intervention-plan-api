package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
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

fun CreateInvestigationRequest.toCsipRecordEntity(
  context: CsipRequestContext,
  referral: Referral,
  intervieweeRoleMap: Map<String, ReferenceData>,
  activeCaseLoadId: String?,
  source: Source,
): CsipRecord = referral.createInvestigation(
  context = context,
  createRequest = this,
  intervieweeRoleMap = intervieweeRoleMap,
  activeCaseLoadId = activeCaseLoadId,
  source = source,
)
