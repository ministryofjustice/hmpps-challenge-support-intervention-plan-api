package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.format.FormatterRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.enumeration.ReferenceDataTypeConverter

@Configuration
class WebMvcConfig : WebMvcConfigurer {
  override fun addFormatters(registry: FormatterRegistry) {
    registry.addConverter(ReferenceDataTypeConverter())
  }
}
