package dev.vality.rateboss.job

import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.service.ExchangeDaoService
import dev.vality.rateboss.source.impl.NbuzExchangeRateSource
import dev.vality.rateboss.source.model.ExchangeRates
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.quartz.JobKey
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
        "rates.nbuz-job.jobCron=0/5 * * * * ?",
        "rates.nbuz-job.currencies.[0].symbolCode=UZS",
        "rates.nbuz-job.currencies.[0].exponent=2",
    ],
)
class NbuzExchangeGrabberJobTest : ContainerConfiguration() {
    @MockitoSpyBean
    lateinit var exchangeDaoService: ExchangeDaoService

    @MockitoBean
    lateinit var nbuzExchangeRateSource: NbuzExchangeRateSource

    @Autowired
    lateinit var scheduler: Scheduler

    @Autowired
    @Qualifier("rates-dev.vality.rateboss.config.properties.RatesProperties")
    lateinit var ratesProperties: RatesProperties

    @BeforeEach
    fun setUp() {
        scheduler.unscheduleJob(TriggerKey(ratesProperties.fixerJob.jobTriggerName))
        scheduler.unscheduleJob(TriggerKey(ratesProperties.cbrJob.jobTriggerName))
        scheduler.unscheduleJob(TriggerKey(ratesProperties.nbkzJob.jobTriggerName))
        scheduler.unscheduleJob(TriggerKey(ratesProperties.nbkrJob.jobTriggerName))
        scheduler.unscheduleJob(TriggerKey(ratesProperties.nbazJob.jobTriggerName))
    }

    @Test
    fun `test grabber job`() {
        whenever(nbuzExchangeRateSource.getSourceId()).thenReturn("sourceId")
        whenever(nbuzExchangeRateSource.getExchangeRate(any())).then {
            ExchangeRates(
                rates =
                    mapOf(
                        "USD" to BigDecimal.valueOf(11979.64),
                        "EUR" to BigDecimal.valueOf(13901.17),
                    ),
                timestamp = Instant.now().epochSecond,
            )
        }
        scheduler.triggerJob(JobKey(ratesProperties.nbuzJob.jobKey))
        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            verify(exchangeDaoService, atLeastOnce()).saveExchangeRates(any())
        }
    }
}
