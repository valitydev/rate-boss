package dev.vality.rateboss.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.client.RestTemplate

@Configuration
class AppConfig {
    @Bean
    fun restTemplate() = RestTemplate()

    @Bean
    fun retryTemplate(
        @Value("\${retryTemplate.backOffPeriod}") backOffPeriod: Long,
        @Value("\${retryTemplate.maxAttempts}") maxAttempts: Int,
    ): RetryTemplate {
        val retryTemplate = RetryTemplate()
        val fixedBackOffPolicy = FixedBackOffPolicy()
        fixedBackOffPolicy.backOffPeriod = backOffPeriod
        retryTemplate.setBackOffPolicy(fixedBackOffPolicy)
        val retryPolicy = SimpleRetryPolicy()
        retryPolicy.maxAttempts = maxAttempts
        retryTemplate.setRetryPolicy(retryPolicy)

        return retryTemplate
    }
}
