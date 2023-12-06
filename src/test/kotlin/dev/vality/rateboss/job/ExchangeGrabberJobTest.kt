package dev.vality.rateboss.job

import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.service.ExchangeDaoService
import dev.vality.rateboss.service.ExchangeEventService
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.model.ExchangeRates
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.TimeUnit

@SpringBootTest(properties = ["rates.jobCron=0/10 * * * * ?"])
class ExchangeGrabberJobTest : ContainerConfiguration() {

    @SpyBean
    lateinit var exchangeEventService: ExchangeEventService

    @SpyBean
    lateinit var exchangeDaoService: ExchangeDaoService

    @MockBean
    lateinit var exchangeRateSource: ExchangeRateSource

    @Test
    fun `test grabber job`() {
        whenever(exchangeRateSource.getExchangeRate(any())).then {
            ExchangeRates(
                rates = mapOf(
                    "AED" to BigDecimal.valueOf(3.593066),
                    "AMD" to BigDecimal.valueOf(397.376632)
                ),
                timestamp = Instant.now().epochSecond
            )
        }

        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            verify(exchangeEventService, atLeastOnce()).sendExchangeRates(any(), any(), any())
        }
        verify(exchangeDaoService, atLeastOnce()).saveExchangeRates(any(), any(), any())
    }
}
