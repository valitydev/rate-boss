package dev.vality.rateboss.job

import dev.vality.exrates.events.CurrencyEvent
import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.service.ExchangeDaoService
import dev.vality.rateboss.service.ExchangeEventService
import dev.vality.rateboss.source.impl.FixerExchangeRateSource
import dev.vality.rateboss.source.model.ExchangeRates
import org.apache.kafka.clients.producer.ProducerRecord
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
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit

@SpringBootTest(
    properties = [
        "rates.fixer-job.jobCron=0/5 * * * * ?",
        "rates.fixer-job.currencies.[0].symbolCode=USD",
        "rates.fixer-job.currencies.[0].exponent=2",
    ],
)
class FixerExchangeGrabberJobTest : ContainerConfiguration() {
    @MockitoSpyBean
    lateinit var exchangeDaoService: ExchangeDaoService

    @MockitoSpyBean
    lateinit var exchangeEventService: ExchangeEventService

    @MockitoBean
    lateinit var kafkaTemplate: KafkaTemplate<String, CurrencyEvent>

    @MockitoBean
    lateinit var fixerExchangeRateSource: FixerExchangeRateSource

    @Autowired
    lateinit var scheduler: Scheduler

    @Autowired
    @Qualifier("rates-dev.vality.rateboss.config.properties.RatesProperties")
    lateinit var ratesProperties: RatesProperties

    @BeforeEach
    fun setUp() {
        scheduler.unscheduleJob(TriggerKey(ratesProperties.cbrJob.jobTriggerName))
    }

    @Test
    fun `test grabber job`() {
        whenever(fixerExchangeRateSource.getSourceId()).thenReturn("sourceId")
        whenever(fixerExchangeRateSource.getExchangeRate(any())).then {
            ExchangeRates(
                rates =
                    mapOf(
                        "AED" to BigDecimal.valueOf(3.593066),
                        "AMD" to BigDecimal.valueOf(397.376632),
                    ),
                timestamp = Instant.now().epochSecond,
            )
        }
        val future = CompletableFuture<SendResult<String, CurrencyEvent>>()
        whenever(kafkaTemplate.send(any<ProducerRecord<String, CurrencyEvent>>())).thenReturn(future)

        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            verify(exchangeEventService, atLeastOnce()).sendExchangeRates(any(), any(), any())
        }
        await().atMost(1, TimeUnit.SECONDS).untilAsserted {
            verify(exchangeDaoService, atLeastOnce()).saveExchangeRates(any())
        }
    }
}
