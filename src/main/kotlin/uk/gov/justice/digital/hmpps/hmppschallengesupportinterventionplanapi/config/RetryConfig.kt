package uk.gov.justice.digital.hmpps.hmppschallengesupportinterventionplanapi.config

import org.springframework.context.annotation.Bean
import org.springframework.retry.backoff.ExponentialBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.stereotype.Component

@Component
class RetryConfig(private val serviceConfig: ServiceConfig) {
  @Bean
  fun retryTemplate(): RetryTemplate = RetryTemplate().apply {
    setRetryPolicy(SimpleRetryPolicy().apply { maxAttempts = serviceConfig.retryAttempts })
    setBackOffPolicy(ExponentialBackOffPolicy().apply { initialInterval = serviceConfig.backOffInterval })
  }
}
