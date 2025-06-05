package dev.vality.rateboss.job

import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.service.ExchangeDaoService
import dev.vality.rateboss.source.impl.CbrExchangeRateSource
import dev.vality.rateboss.source.model.ExchangeRates
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.quartz.Scheduler
import org.quartz.TriggerKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.TimeUnit

@SpringBootTest(
    properties = [
        "rates.cbr-job.jobCron=0/5 * * * * ?",
        "rates.cbr-job.currencies.[0].symbolCode=RUB",
        "rates.cbr-job.currencies.[0].exponent=2",
    ],
)
class CbrExchangeGrabberJobTest : ContainerConfiguration() {
    @MockitoSpyBean
    lateinit var exchangeDaoService: ExchangeDaoService

    @MockitoBean
    lateinit var cbrExchangeRateSource: CbrExchangeRateSource

    @Autowired
    lateinit var scheduler: Scheduler

    @Autowired
    @Qualifier("rates-dev.vality.rateboss.config.properties.RatesProperties")
    lateinit var ratesProperties: RatesProperties

    @BeforeEach
    fun setUp() {
        scheduler.unscheduleJob(TriggerKey(ratesProperties.fixerJob.jobTriggerName))
    }

    @Test
    fun `test grabber job`() {
        whenever(cbrExchangeRateSource.getSourceId()).thenReturn("sourceId")
        whenever(cbrExchangeRateSource.getExchangeRate(any())).then {
            ExchangeRates(
                rates =
                    mapOf(
                        "USD" to BigDecimal.valueOf(90.593066),
                        "EUR" to BigDecimal.valueOf(98.376632),
                    ),
                timestamp = Instant.now().epochSecond,
            )
        }
        await().atMost(5, TimeUnit.SECONDS).untilAsserted {
            verify(exchangeDaoService, atLeastOnce()).saveExchangeRates(any())
        }
    }
}
