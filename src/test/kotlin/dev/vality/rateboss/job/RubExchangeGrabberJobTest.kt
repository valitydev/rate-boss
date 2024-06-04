package dev.vality.rateboss.job

import dev.vality.exrates.events.CurrencyEvent
import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.service.ExchangeDaoService
import dev.vality.rateboss.service.ExchangeEventService
import dev.vality.rateboss.source.impl.CbrExchangeRateSource
import dev.vality.rateboss.source.model.ExchangeRates
import org.apache.kafka.clients.producer.ProducerRecord
import org.awaitility.Awaitility.await
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.support.SendResult
import org.springframework.util.concurrent.SettableListenableFuture
import java.math.BigDecimal
import java.time.Instant
import java.util.concurrent.TimeUnit

@SpringBootTest(
    properties = [
        "rates.jobCron=0/5 * * * * ?",
        "rates.currencies.[0].symbolCode=RUB",
        "rates.currencies.[0].exponent=2"
    ]
)
class RubExchangeGrabberJobTest : ContainerConfiguration() {

    @SpyBean
    lateinit var exchangeEventService: ExchangeEventService

    @MockBean
    lateinit var kafkaTemplate: KafkaTemplate<String, CurrencyEvent>

    @SpyBean
    lateinit var exchangeDaoService: ExchangeDaoService

    @MockBean
    lateinit var cbrExchangeRateSource: CbrExchangeRateSource

    @Test
    fun `test grabber job`() {
        whenever(cbrExchangeRateSource.getSourceId()).thenReturn("sourceId")
        whenever(cbrExchangeRateSource.getExchangeRate(any())).then {
            ExchangeRates(
                rates = mapOf(
                    "USD" to BigDecimal.valueOf(90.593066),
                    "EUR" to BigDecimal.valueOf(98.376632)
                ),
                timestamp = Instant.now().epochSecond
            )
        }
        val future = SettableListenableFuture<SendResult<String, CurrencyEvent>>()
        whenever(kafkaTemplate.send(any<ProducerRecord<String, CurrencyEvent>>())).thenReturn(future)

        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            verify(exchangeEventService, atLeastOnce()).sendExchangeRates(any(), any(), any())
        }
        await().atMost(1, TimeUnit.SECONDS).untilAsserted {
            verify(exchangeDaoService, atLeastOnce()).saveExchangeRates(any())
        }
    }
}
