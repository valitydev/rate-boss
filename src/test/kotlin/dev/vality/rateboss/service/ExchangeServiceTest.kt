package dev.vality.rateboss.service

import dev.vality.exrates.events.CurrencyEvent
import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.source.model.ExchangeRates
import org.apache.kafka.clients.producer.ProducerRecord
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.kafka.core.KafkaTemplate
import java.math.BigDecimal
import java.time.Instant

class ExchangeServiceTest : ContainerConfiguration() {

    @Autowired
    lateinit var exchangeService: ExchangeService

    @SpyBean
    lateinit var kafkaTemplate: KafkaTemplate<String, CurrencyEvent>

    @Test
    fun `send exchange rate`() {
        // Given
        val baseCurrencySymbolCode = "USD"
        val baseCurrencyExponent = 2
        val exchangeRates = ExchangeRates(
            rates = mapOf(
                "RUB" to BigDecimal.valueOf(56.761762),
                "AED" to BigDecimal.valueOf(3.593066),
                "AMD" to BigDecimal.valueOf(397.376632)
            ),
            timestamp = Instant.now().epochSecond
        )

        // When
        exchangeService.sendExchangeRates(baseCurrencySymbolCode, baseCurrencyExponent.toByte(), exchangeRates)

        // Then
        verify(kafkaTemplate, times(3)).send(any<ProducerRecord<String, CurrencyEvent>>())
    }

    @Test
    fun `send exchange rate conversion`() {
        // Given
        val baseCurrencySymbolCode = "USD"
        val baseCurrencyExponent = 2
        val rubExchangeRate = BigDecimal.valueOf(56.761762)
        val btcExchangeRate = BigDecimal.valueOf(5.0203877e-05)
        val exchangeRates = ExchangeRates(
            rates = mapOf(
                "RUB" to rubExchangeRate,
                "BTC" to btcExchangeRate
            ),
            timestamp = Instant.now().epochSecond
        )

        // When
        exchangeService.sendExchangeRates(baseCurrencySymbolCode, baseCurrencyExponent.toByte(), exchangeRates)

        // Then
        val argumentCaptor = argumentCaptor<ProducerRecord<String, CurrencyEvent>>()
        verify(kafkaTemplate, times(2)).send(argumentCaptor.capture())
        val firstRecordCurrencyEvent = argumentCaptor.allValues[0].value()
        val secondRecordCurrencyEvent = argumentCaptor.allValues[1].value()
        val firstRecordExchangeRate =
            BigDecimal.valueOf(firstRecordCurrencyEvent.getPayload().exchangeRate.exchange_rate.p)
                .divideAndRemainder(BigDecimal.valueOf(firstRecordCurrencyEvent.getPayload().exchangeRate.exchange_rate.q))
        val secondRecordExchangeRate = BigDecimal.valueOf(secondRecordCurrencyEvent.getPayload().exchangeRate.exchange_rate.p)
            .divide(BigDecimal.valueOf(secondRecordCurrencyEvent.getPayload().exchangeRate.exchange_rate.q))
        assertTrue(BigDecimal("${firstRecordExchangeRate[0]}.${firstRecordExchangeRate[1]}") == rubExchangeRate)
        assertTrue(secondRecordExchangeRate == btcExchangeRate)
    }

}
