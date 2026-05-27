package dev.vality.rateboss.source.impl

import com.fasterxml.jackson.databind.ObjectMapper
import dev.vality.rateboss.client.nbuz.NbuzApiClient
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.job.constant.ExRateSources
import dev.vality.rateboss.model.NbuzRatesResponse
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset

private val log = KotlinLogging.logger {}

@Component
class NbuzExchangeRateSource(
    private val nbuzApiClient: NbuzApiClient,
    private val ratesProperties: RatesProperties,
    private val objectMapper: ObjectMapper,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val timeZone = ratesProperties.source.nbuz.timeZone
        val date = LocalDate.now(timeZone)
        log.info { "Trying to get exchange rates from nbuz for currency=$currencySymbolCode, date=$date" }
        val response =
            try {
                objectMapper.readValue(nbuzApiClient.getExchangeRates(date), NbuzRatesResponse::class.java)
            } catch (e: Exception) {
                throw ExchangeRateSourceException("Failed to get daily rates", e)
            }
        val rates = buildRatesMap(response)
        if (rates.isEmpty()) {
            throw ExchangeRateSourceException("Unsuccessful response from NbuzApi")
        }
        val nextDayTimestamp = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info { "Exchange rates from nbuz have been retrieved, date=$date, exchangeRates=$rates, targetTimestamp=$nextDayTimestamp" }
        return ExchangeRates(
            rates = rates,
            timestamp = nextDayTimestamp,
        )
    }

    override fun getSourceId(): String = ExRateSources.NBUZ

    private fun buildRatesMap(response: NbuzRatesResponse): Map<String, BigDecimal> {
        val rates = mutableMapOf<String, BigDecimal>()
        val items =
            response.data
                ?.firstOrNull()
                ?.rates
                .orEmpty()
        for (item in items) {
            val rateCode = item.rateCode?.trim { it <= ' ' }.orEmpty()
            val nominal = item.rateEquivalent.normalizeDecimal().toBigDecimalOrNull()
            val rate = item.rateSb.normalizeDecimal().toBigDecimalOrNull()
            if (rateCode.isBlank() || nominal == null || rate == null || nominal.compareTo(BigDecimal.ZERO) == 0) {
                log.debug { "Skip malformed NBUZ currency record: $item" }
                continue
            }
            rates[rateCode] = rate.divide(nominal)
        }
        return rates
    }

    private fun String?.normalizeDecimal(): String =
        this
            ?.replace(" ", "")
            ?.replace(",", ".")
            ?.trim()
            .orEmpty()
}
