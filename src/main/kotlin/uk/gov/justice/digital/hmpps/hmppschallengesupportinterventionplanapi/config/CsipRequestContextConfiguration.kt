package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.ValidationException
import org.springframework.context.annotation.Configuration
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.servlet.HandlerInterceptor
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.Source
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.service.UserService
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.utils.LanguageFormatUtils.formatDisplayName
import uk.gov.justice.hmpps.kotlin.auth.AuthAwareAuthenticationToken

@Configuration
class CsipRequestContextConfiguration(private val csipRequestContextInterceptor: CsipRequestContextInterceptor) : WebMvcConfigurer {
  override fun addInterceptors(registry: InterceptorRegistry) {
    registry.addInterceptor(csipRequestContextInterceptor)
      .addPathPatterns("/reference-data/**")
      .addPathPatterns("/csip-records/**")
      .addPathPatterns("/prisoners/*/csip-records")
      .excludePathPatterns("/prisoners/csip-records")
  }
}

@Configuration
class CsipRequestContextInterceptor(
  private val userService: UserService,
) : HandlerInterceptor {
  override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
    if (arrayOf("POST", "PUT", "PATCH", "DELETE").contains(request.method)) {
      val userDetails = getUserDetails(getUsername())

      request.setAttribute(
        CsipRequestContext::class.simpleName,
        CsipRequestContext(
          source = Source.DPS,
          username = userDetails.username,
          userDisplayName = formatDisplayName(userDetails.name),
          activeCaseLoadId = userDetails.activeCaseLoadId,
        ),
      )
    }

    return true
  }

  private fun authentication(): AuthAwareAuthenticationToken = SecurityContextHolder.getContext().authentication as AuthAwareAuthenticationToken?
    ?: throw AccessDeniedException("User is not authenticated")

  private fun getUsername(): String = authentication().name
    .trim().takeUnless(String::isBlank)
    ?.also { if (it.length > 64) throw ValidationException("Created by must be <= 64 characters") }
    ?: throw ValidationException("Could not find non empty username")

  private fun getUserDetails(username: String) = userService.getUserDetails(username) ?: throw ValidationException("User details for supplied username not found")
}
