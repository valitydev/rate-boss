package dev.vality.rateboss.service

import dev.vality.exrates.base.Rational
import dev.vality.exrates.events.Currency
import dev.vality.exrates.events.CurrencyEvent
import dev.vality.exrates.events.CurrencyEventPayload
import dev.vality.exrates.events.CurrencyExchangeRate
import dev.vality.rateboss.extensions.toRational
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class ExchangeService(
    private val kafkaTemplate: KafkaTemplate<String, CurrencyEvent>
) {

    @Value("\${kafka.topic.producer.exchange.name}")
    lateinit var topicName: String

    fun sendExchangeRates(
        baseCurrencySymbolCode: String,
        baseCurrencyExponent: Byte,
        exchangeRates: ExchangeRates
    ) {
        val currencyEvents = exchangeRates.rates.map { exchangeRate ->
            val eventId = UUID.randomUUID().toString()
            val eventTime = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME)
            val payload = CurrencyEventPayload.exchange_rate(
                CurrencyExchangeRate()
                    .setSourceCurrency(
                        Currency(
                            baseCurrencySymbolCode,
                            baseCurrencyExponent
                        )
                    )
                    .setDestinationCurrency(
                        Currency(
                            exchangeRate.key,
                            exchangeRate.value.scale().toByte()
                        )
                    )
                    .setExchangeRate(Rational().apply {
                        val rational = exchangeRate.value.toRational()
                        p = rational.numerator
                        q = rational.denominator
                    })
                    .setTimestamp(Instant.ofEpochSecond(exchangeRates.timestamp).toString())
            )
            CurrencyEvent(eventId, eventTime, payload)
        }
        for (currencyEvent in currencyEvents) {
            val producerRecord = ProducerRecord(topicName, currencyEvent.eventId, currencyEvent)
            kafkaTemplate.send(producerRecord).addCallback(
                { result ->
                    log.debug {
                        "Successfully send currency event. Topic=" + result?.recordMetadata?.topic() + ";" +
                                " Offset=" + result?.recordMetadata?.offset() + ";" +
                                " Partition=" + result?.recordMetadata?.partition()
                    }
                },
                { log.error(it.cause) { "Failed to send event. Topic=${producerRecord.topic()}; Partition=${producerRecord.partition()}" } }
            )
        }
    }
}