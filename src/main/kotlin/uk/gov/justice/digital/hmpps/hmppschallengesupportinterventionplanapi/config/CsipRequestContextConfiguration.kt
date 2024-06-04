package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ValidationException
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.client.manageusers.dto.UserDetailsDto
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.SOURCE
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.constant.USERNAME
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LanguageFormatUtils
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken

@Configuration
class CsipRequestContextConfiguration(private val csipRequestContextInterceptor: CsipRequestContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    log.info("Adding csip request context interceptor")
    registry.addInterceptor(csipRequestContextInterceptor).addPathPatterns("/csip-records/**")
    registry.addInterceptor(csipRequestContextInterceptor).addPathPatterns("/prisoners/**")
    registry.addInterceptor(csipRequestContextInterceptor).addPathPatterns("/reference-data/**")
  }

  companion object {
    private val log = LoggerFactory.getLogger(this::class.java)
  }
}

@Configuration
class CsipRequestContextInterceptor(
  private val userService: UserService,
) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (arrayOf("POST", "PUT", "PATCH", "DELETE").contains(request.method)) {
      val source = request.getSource()
      val userDetails = request.getUserDetails(source)

      request.setAttribute(
        CsipRequestContext::class.simpleName,
        CsipRequestContext(
          source = source,
          username = userDetails.username,
          userDisplayName = LanguageFormatUtils.formatDisplayName(userDetails.name),
          activeCaseLoadId = userDetails.activeCaseLoadId,
        ),
      )
    }

    return true
  }

  private fun HttpServletRequest.getSource(): Source =
    getHeader(SOURCE)?.let { Source.valueOf(it) } ?: Source.DPS

  private fun authentication(): AuthAwareAuthenticationToken =
    SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
      ?: throw AccessDeniedException("User is not authenticated")

  private fun getUsernameFromClaim(): String? =
    authentication().let {
      it.tokenAttributes["user_name"] as String?
        ?: it.tokenAttributes["username"] as String?
    }

  private fun HttpServletRequest.getUsername(source: Source): String =
    (getUsernameFromClaim() ?: getHeader(USERNAME))
      ?.trim()?.takeUnless(String::isBlank)?.also { if (it.length > 32) throw ValidationException("Created by must be <= 32 characters") }
      ?: if (source != Source.DPS) {
        source.name
      } else {
        throw ValidationException("Could not find non empty username from user_name or username token claims or Username header")
      }

  private fun HttpServletRequest.getUserDetails(source: Source) =
    getUsername(source).let {
      userService.getUserDetails(it)
        ?: if (source != Source.DPS) {
          UserDetailsDto(username = it, active = true, name = it, authSource = it, userId = it, uuid = null, activeCaseLoadId = null)
        } else {
          null
        }
    } ?: throw ValidationException("User details for supplied username not found")
}
