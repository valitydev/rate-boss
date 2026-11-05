package dev.vality.rateboss.source.impl

import dev.vality.rateboss.client.nbp.NbpApiClient
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.extensions.toBigFraction
import dev.vality.rateboss.job.constant.ExRateSources
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
        val quote = parseQuote(fetchDailyRates())
        val rates = mapOf("EUR" to quote.rate)
        val dayTimestamp = pollingDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info {
            "Exchange rates from nbp have been retrieved, effectiveDate=${quote.publicationDate}, " +
                "pollingDate=$pollingDate, exchangeRates=$rates, targetTimestamp=$dayTimestamp"
        }
        return ExchangeRates(rates = rates, timestamp = dayTimestamp)
    }

    override fun getSourceId(): String = ExRateSources.NBP

    private fun fetchDailyRates(): NbpRatesResponse =
        try {
            nbpApiClient.getExchangeRates()
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Failed to get daily rates from nbp", e)
        }

    private fun parseQuote(response: NbpRatesResponse): Quote =
        try {
            require(response.code == "EUR") { "Expected EUR rate from NbpApi" }
            val rate = requireNotNull(response.rates?.firstOrNull()) { "Missing rate from NbpApi" }
            val mid = requireNotNull(rate.mid) { "Missing mid from NbpApi" }
            require(mid > BigDecimal.ZERO) { "Non-positive mid from NbpApi" }
            val publicationDate = LocalDate.parse(requireNotNull(rate.effectiveDate) { "Missing effectiveDate from NbpApi" })
            val exactRate = RateValue.Fraction(mid.toBigFraction().reciprocal())
            // Ensure the numerator and denominator fit in Long before returning the rate.
            exactRate.toRational()
            Quote(exactRate, publicationDate)
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Invalid response from NbpApi", e)
        }

    private data class Quote(
        val rate: RateValue.Fraction,
        val publicationDate: LocalDate,
    )
}
