package dev.vality.rateboss.service

import dev.vality.exrates.base.Currency
import dev.vality.exrates.base.Rational
import dev.vality.exrates.events.CurrencyEvent
import dev.vality.exrates.events.CurrencyEventPayload
import dev.vality.exrates.events.CurrencyExchangeRate
import dev.vality.geck.common.util.TypeUtil
import dev.vality.rateboss.extensions.toRational
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class ExchangeEventService(
    private val kafkaTemplate: KafkaTemplate<String, CurrencyEvent>,
) {
    @Value("\${kafka.topic.producer.exchangeTopic}")
    lateinit var topicName: String

    fun sendExchangeRates(
        baseCurrencySymbolCode: String,
        baseCurrencyExponent: Short,
        exchangeRates: ExchangeRates,
    ) {
        val currencyEvents =
            exchangeRates.rates.map { exchangeRatesMap ->
                val eventId = UUID.randomUUID().toString()
                val eventTime = TypeUtil.temporalToString(LocalDateTime.now(), ZoneOffset.UTC)
                val payload =
                    buildCurrencyExchangeRatePayload(
                        baseCurrencySymbolCode,
                        baseCurrencyExponent,
                        exchangeRatesMap,
                        exchangeRates.timestamp,
                    )
                CurrencyEvent(eventId, eventTime, payload)
            }
        for (currencyEvent in currencyEvents) {
            val producerRecord = ProducerRecord(topicName, currencyEvent.eventId, currencyEvent)
            kafkaTemplate
                .send(producerRecord)
                .thenAccept(
                    { result ->
                        log.info {
                            "Successfully send currency event. Topic=" + result?.recordMetadata?.topic() + ";" +
                                " Offset=" + result?.recordMetadata?.offset() + ";" +
                                " Partition=" + result?.recordMetadata?.partition()
                        }
                    }
                ).exceptionally(
                    { err ->
                        log.error(
                            err.cause
                        ) { "Failed to send event. Topic=${producerRecord.topic()}; Partition=${producerRecord.partition()}" }
                        null
                    }
                )
        }
    }

    private fun buildCurrencyExchangeRatePayload(
        baseCurrencySymbolCode: String,
        baseCurrencyExponent: Short,
        exchangeRateMap: Map.Entry<String, BigDecimal>,
        exchangeRateTimestamp: Long,
    ): CurrencyEventPayload? =
        CurrencyEventPayload.exchange_rate(
            CurrencyExchangeRate()
                .setSourceCurrency(
                    Currency(
                        baseCurrencySymbolCode,
                        baseCurrencyExponent,
                    ),
                ).setDestinationCurrency(
                    Currency(
                        exchangeRateMap.key,
                        exchangeRateMap.value.scale().toShort(),
                    ),
                ).setExchangeRate(
                    Rational().apply {
                        val rational = exchangeRateMap.value.toRational()
                        p = rational.numerator
                        q = rational.denominator
                    },
                ).setTimestamp(TypeUtil.temporalToString(Instant.ofEpochSecond(exchangeRateTimestamp))),
        )
}
