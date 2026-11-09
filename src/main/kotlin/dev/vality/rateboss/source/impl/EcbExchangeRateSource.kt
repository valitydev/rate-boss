package dev.vality.rateboss.source.impl

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import dev.vality.rateboss.client.ecb.EcbApiClient
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.extensions.toBigFraction
import dev.vality.rateboss.job.constant.ExRateSources
import dev.vality.rateboss.model.EcbDailyRatesXml
import dev.vality.rateboss.model.EcbRateItemXml
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
class EcbExchangeRateSource(
    private val ecbApiClient: EcbApiClient,
    private val ratesProperties: RatesProperties,
    private val xmlMapper: XmlMapper,
    @Qualifier("ecbClock")
    private val clock: Clock,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        if (currencySymbolCode != "EUR") {
            throw ExchangeRateSourceException("Unsupported base currency for EcbApi: $currencySymbolCode")
        }
        val pollingDate = LocalDate.now(clock.withZone(ratesProperties.source.ecb.timeZone))
        log.info { "Trying to get exchange rates from ecb for currency=$currencySymbolCode" }
        val table = parseTable(fetchDailyRates())
        val dayTimestamp = pollingDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info {
            "Exchange rates from ecb have been retrieved, publicationDate=${table.publicationDate}, " +
                "pollingDate=$pollingDate, exchangeRates=${table.rates}, targetTimestamp=$dayTimestamp"
        }
        return ExchangeRates(rates = table.rates, timestamp = dayTimestamp)
    }

    override fun getSourceId(): String = ExRateSources.ECB

    private fun fetchDailyRates(): String =
        try {
            ecbApiClient.getExchangeRates()
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Failed to get daily rates from ecb", e)
        }

    private fun parseTable(xml: String): ExchangeRateTable =
        try {
            val response = xmlMapper.readValue(xml, EcbDailyRatesXml::class.java)
            val tables = requireNotNull(response.cube?.tables) { "Missing tables from EcbApi" }
            val table = tables.single()
            val publicationDate = LocalDate.parse(requireNotNull(table.date) { "Missing publication date from EcbApi" })
            val items = requireNotNull(table.rates) { "Missing rates from EcbApi" }
            ExchangeRateTable(buildRatesMap(items), publicationDate)
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Invalid response from EcbApi", e)
        }

    private fun buildRatesMap(items: List<EcbRateItemXml>): Map<String, RateValue.Fraction> {
        require(items.isNotEmpty()) { "Empty rates from EcbApi" }
        val rates = mutableMapOf<String, RateValue.Fraction>()
        for (item in items) {
            val code = requireNotNull(item.code) { "Missing currency code from EcbApi" }
            require(code.isNotBlank()) { "Blank currency code from EcbApi" }
            require(code !in rates) { "Duplicate currency code from EcbApi: $code" }
            rates[code] = parseRate(item, code)
        }
        return rates
    }

    private fun parseRate(
        item: EcbRateItemXml,
        code: String,
    ): RateValue.Fraction {
        val rate = requireNotNull(item.rate) { "Missing rate from EcbApi for $code" }
        require(rate > BigDecimal.ZERO) { "Non-positive rate from EcbApi for $code" }
        val exactRate = RateValue.Fraction(rate.toBigFraction())
        // Ensure the numerator and denominator fit in Long before returning the rate.
        exactRate.toRational()
        return exactRate
    }

    private data class ExchangeRateTable(
        val rates: Map<String, RateValue.Fraction>,
        val publicationDate: LocalDate,
    )
}
