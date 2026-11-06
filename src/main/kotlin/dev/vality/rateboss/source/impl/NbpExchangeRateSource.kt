package dev.vality.rateboss.source.impl

import dev.vality.rateboss.client.nbp.NbpApiClient
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.extensions.toBigFraction
import dev.vality.rateboss.job.constant.ExRateSources
import dev.vality.rateboss.model.NbpRateItem
import dev.vality.rateboss.model.NbpRatesResponse
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.model.ExchangeRates
import dev.vality.rateboss.source.model.RateValue
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneOffset

private val log = KotlinLogging.logger {}

@Component
class NbpExchangeRateSource(
    private val nbpApiClient: NbpApiClient,
    private val ratesProperties: RatesProperties,
    @Qualifier("nbpClock")
    private val clock: Clock,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        if (currencySymbolCode != "PLN") {
            throw ExchangeRateSourceException("Unsupported base currency for NbpApi: $currencySymbolCode")
        }
        val pollingDate = LocalDate.now(clock.withZone(ratesProperties.source.nbp.timeZone))
        log.info { "Trying to get exchange rates from nbp for currency=$currencySymbolCode" }
        val table = parseTable(fetchDailyRates())
        val dayTimestamp = pollingDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info {
            "Exchange rates from nbp have been retrieved, effectiveDate=${table.publicationDate}, " +
                "pollingDate=$pollingDate, exchangeRates=${table.rates}, targetTimestamp=$dayTimestamp"
        }
        return ExchangeRates(rates = table.rates, timestamp = dayTimestamp)
    }

    override fun getSourceId(): String = ExRateSources.NBP

    private fun fetchDailyRates(): List<NbpRatesResponse> =
        try {
            nbpApiClient.getExchangeRates()
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Failed to get daily rates from nbp", e)
        }

    private fun parseTable(response: List<NbpRatesResponse>): ExchangeRateTable =
        try {
            val table = response.single()
            val publicationDate = LocalDate.parse(requireNotNull(table.effectiveDate) { "Missing effectiveDate from NbpApi" })
            val items = requireNotNull(table.rates) { "Missing rates from NbpApi" }
            ExchangeRateTable(buildRatesMap(items), publicationDate)
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Invalid response from NbpApi", e)
        }

    private fun buildRatesMap(items: List<NbpRateItem>): Map<String, RateValue.Fraction> {
        require(items.isNotEmpty()) { "Empty rates from NbpApi" }
        val rates = mutableMapOf<String, RateValue.Fraction>()
        for (item in items) {
            val code = requireNotNull(item.code) { "Missing currency code from NbpApi" }
            require(code.isNotBlank()) { "Blank currency code from NbpApi" }
            require(code !in rates) { "Duplicate currency code from NbpApi: $code" }
            rates[code] = parseRate(item, code)
        }
        return rates
    }

    private fun parseRate(
        item: NbpRateItem,
        code: String,
    ): RateValue.Fraction {
        val mid = requireNotNull(item.mid) { "Missing mid from NbpApi for $code" }
        require(mid > BigDecimal.ZERO) { "Non-positive mid from NbpApi for $code" }
        val exactRate = RateValue.Fraction(mid.toBigFraction().reciprocal())
        // Ensure the numerator and denominator fit in Long before returning the rate.
        exactRate.toRational()
        return exactRate
    }

    private data class ExchangeRateTable(
        val rates: Map<String, RateValue.Fraction>,
        val publicationDate: LocalDate,
    )
}
