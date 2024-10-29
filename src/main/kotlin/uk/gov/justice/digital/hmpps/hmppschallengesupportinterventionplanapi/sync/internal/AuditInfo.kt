package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.internal

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit.Auditable
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.sync.NomisAudited

fun <T : Auditable> T.withAuditInfo(request: NomisAudited): T = apply {
  createdAt = request.createdAt
  createdBy = request.createdBy
  lastModifiedAt = request.lastModifiedAt
  lastModifiedBy = request.lastModifiedBy
}
