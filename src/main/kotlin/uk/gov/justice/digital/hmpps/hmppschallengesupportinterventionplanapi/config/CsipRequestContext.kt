package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import jakarta.servlet.http.HttpServletRequest
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

data class CsipRequestContext(
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val source: Source = Source.DPS,
  val username: String,
  val userDisplayName: String,
)

fun HttpServletRequest.csipRequestContext() =
  getAttribute(CsipRequestContext::class.simpleName) as CsipRequestContext
