package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

data class CsipRequestContext(
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val source: Source = Source.DPS,
  val username: String,
  val userDisplayName: String,
  val activeCaseLoadId: String? = null,
)

fun HttpServletRequest.csipRequestContext() =
  getAttribute(CsipRequestContext::class.simpleName) as CsipRequestContext

fun csipRequestContext(): CsipRequestContext = RequestContextHolder.getRequestAttributes()
  ?.getAttribute(CsipRequestContext::class.simpleName!!, 0) as CsipRequestContext?
  ?: CsipRequestContext(username = "SYS", userDisplayName = "Sys")
