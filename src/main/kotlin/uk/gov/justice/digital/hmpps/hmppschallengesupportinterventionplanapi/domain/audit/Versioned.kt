package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.domain.audit

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.Version
import org.hibernate.envers.NotAudited

interface Versioned {
  val version: Int?
}

@MappedSuperclass
open class SimpleVersion : Versioned {
  @field:NotAudited
  @field:Version
  override val version: Int? = null
}
