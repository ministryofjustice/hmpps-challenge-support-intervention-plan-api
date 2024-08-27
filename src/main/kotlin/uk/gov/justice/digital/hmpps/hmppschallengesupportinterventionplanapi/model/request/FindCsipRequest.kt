package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.model.request

import org.springframework.data.domain.Pageable
import java.time.LocalDateTime

data class FindCsipRequest(
  val prisonNumbers: Set<String>,
  val logCode: String?,
  val from: LocalDateTime?,
  val to: LocalDateTime?,
  val pageable: Pageable,
)
