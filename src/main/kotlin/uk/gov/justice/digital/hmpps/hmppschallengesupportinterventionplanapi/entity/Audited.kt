package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.entity

import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.CsipRequestContext
import java.time.LocalDateTime

interface Audited {
  val createdAt: LocalDateTime
  val createdBy: String
  val createdByDisplayName: String

  val lastModifiedAt: LocalDateTime?
  val lastModifiedBy: String?
  val lastModifiedByDisplayName: String?

  fun recordCreatedDetails(context: CsipRequestContext)
  fun recordModifiedDetails(context: CsipRequestContext)
}
