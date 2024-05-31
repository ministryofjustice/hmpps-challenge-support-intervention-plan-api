package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.runApplication
import uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config.EventProperties

@EnableConfigurationProperties(EventProperties::class)
@SpringBootApplication
class HmppsChallengeSupportInterventionPlanApi

fun main(args: Array<String>) {
  runApplication<HmppsChallengeSupportInterventionPlanApi>(*args)
}
