package dev.vality.rateboss.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.SimpleClientHttpRequestFactory
import org.springframework.retry.backoff.FixedBackOffPolicy
import org.springframework.retry.policy.SimpleRetryPolicy
import org.springframework.retry.support.RetryTemplate
import org.springframework.web.client.RestTemplate
import java.time.Clock

@Configuration
class AppConfig {
    @Bean
    fun restTemplate(ratesProperties: RatesProperties): RestTemplate {
        val properties = ratesProperties.http
        val requestFactory = SimpleClientHttpRequestFactory()
        requestFactory.setConnectTimeout(properties.connectTimeout)
        requestFactory.setReadTimeout(properties.readTimeout)
        return RestTemplate(requestFactory)
    }

    @Bean
    fun nbpClock(): Clock = Clock.systemUTC()

    @Bean
    fun ecbClock(): Clock = Clock.systemUTC()

    @Bean
    fun xmlMapper(): XmlMapper =
        XmlMapper
            .builder()
            .addModule(kotlinModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()

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
