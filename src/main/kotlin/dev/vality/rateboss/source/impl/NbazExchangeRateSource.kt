package dev.vality.rateboss.source.impl

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import dev.vality.rateboss.client.nbaz.NbazApiClient
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.job.constant.ExRateSources
import dev.vality.rateboss.model.NbazDailyRatesXml
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private val log = KotlinLogging.logger {}

@Component
class NbazExchangeRateSource(
    private val nbazApiClient: NbazApiClient,
    private val ratesProperties: RatesProperties,
    private val xmlMapper: XmlMapper,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val timeZone = ratesProperties.source.nbaz.timeZone
        val date = LocalDate.now(timeZone)
        log.info { "Trying to get exchange rates from nbaz for currency=$currencySymbolCode, date=$date" }
        val parsed = fetchDailyRates(date)
        val rates = buildRatesMap(parsed)
        if (rates.isEmpty()) {
            throw ExchangeRateSourceException("Unsuccessful response from NbazApi")
        }
        val dayTimestamp = date.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info {
            "Exchange rates from nbaz have been retrieved, date=$date, " +
                "exchangeRates=$rates, targetTimestamp=$dayTimestamp"
        }
        return ExchangeRates(
            rates = rates,
            timestamp = dayTimestamp,
        )
    }

    override fun getSourceId(): String = ExRateSources.NBAZ

    private fun fetchDailyRates(date: LocalDate): NbazDailyRatesXml =
        try {
            xmlMapper.readValue(nbazApiClient.getExchangeRates(date), NbazDailyRatesXml::class.java)
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Failed to get daily rates", e)
        }

    private fun buildRatesMap(root: NbazDailyRatesXml): Map<String, BigDecimal> {
        val rates = mutableMapOf<String, BigDecimal>()
        val currencies =
            root.valTypes
                .orEmpty()
                .firstOrNull { it.type?.trim { ch -> ch <= ' ' } == FOREIGN_CURRENCIES_TYPE }
                ?.valutes
                .orEmpty()
        for (item in currencies) {
            val code = item.code?.trim { it <= ' ' }.orEmpty()
            val nominal = item.nominal?.extractDecimal()?.toBigDecimalOrNull()
            val value = item.value?.extractDecimal()?.toBigDecimalOrNull()
            if (code.isBlank() || nominal == null || value == null || nominal.compareTo(BigDecimal.ZERO) == 0) {
                log.debug { "Skip malformed NBAZ currency record: $item" }
                continue
            }
            rates[code] = value.divide(nominal)
        }
        return rates
    }

    private fun String.extractDecimal(): String =
        trim()
            .replace(",", ".")
            .substringBefore(' ')

    companion object {
        private const val FOREIGN_CURRENCIES_TYPE = "Xarici valyutalar"
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
