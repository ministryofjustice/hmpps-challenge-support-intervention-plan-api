package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.springframework.web.context.request.RequestContextHolder.getRequestAttributes
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import java.time.LocalDateTime

const val SYSTEM_USER_NAME = "SYS"
const val SYSTEM_DISPLAY_NAME = "Sys"

data class CsipRequestContext(
  val requestAt: LocalDateTime = LocalDateTime.now(),
  val source: Source = Source.DPS,
  val username: String,
  val userDisplayName: String,
  val activeCaseLoadId: String? = null,
)

fun csipRequestContext(): CsipRequestContext = getRequestAttributes()
  ?.getAttribute(CsipRequestContext::class.simpleName!!, 0) as CsipRequestContext?
  ?: CsipRequestContext(username = SYSTEM_USER_NAME, userDisplayName = SYSTEM_DISPLAY_NAME).also {
    getRequestAttributes()?.setAttribute(CsipRequestContext::class.simpleName!!, it, 0)
  }
