package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.springframework.boot.actuate.info.Info
import org.springframework.boot.actuate.info.InfoContributor
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@Component
class ServiceConfigInfo(
  private val serviceConfig: ServiceConfig,
) : InfoContributor {
  override fun contribute(builder: Info.Builder) {
    builder
      .withDetail("activeAgencies", serviceConfig.activePrisons)
      .withDetail("publishEvents", serviceConfig.publishEvents)
  }
}

@ConfigurationProperties(prefix = "service")
data class ServiceConfig(
  val activePrisons: Set<String>,
  val baseUrl: String,
  val publishEvents: Boolean,
)
