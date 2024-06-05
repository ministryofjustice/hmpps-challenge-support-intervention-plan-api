package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.CsipRecord
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Investigation
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.ReferenceData
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity.Referral
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request.CreateInvestigationRequest
import java.time.LocalDateTime

fun Investigation.toModel() = uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.Investigation(
  staffInvolved = staffInvolved,
  evidenceSecured = evidenceSecured,
  occurrenceReason = occurrenceReason,
  personsUsualBehaviour = personsUsualBehaviour,
  personsTrigger = personsTrigger,
  protectiveFactors = protectiveFactors,
  interviews = interviews().map { it.toModel() },
)

fun CreateInvestigationRequest.toCsipRecordEntity(
  referral: Referral,
  intervieweeRoleMap: Map<String, ReferenceData>,
  actionedAt: LocalDateTime = LocalDateTime.now(),
  actionedBy: String,
  actionedByDisplayName: String,
  activeCaseLoadId: String?,
  source: Source,
): CsipRecord = referral.createInvestigation(
  createRequest = this,
  intervieweeRoleMap = intervieweeRoleMap,
  actionedAt = actionedAt,
  actionedBy = actionedBy,
  actionedByDisplayName = actionedByDisplayName,
  activeCaseLoadId = activeCaseLoadId,
  source = source,
)
