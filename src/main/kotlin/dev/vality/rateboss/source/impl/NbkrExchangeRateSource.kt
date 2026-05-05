package dev.vality.rateboss.source.impl

import com.fasterxml.jackson.dataformat.xml.XmlMapper
import dev.vality.rateboss.client.nbkr.NbkrApiClient
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.job.constant.ExRateSources
import dev.vality.rateboss.model.NbkrDailyRatesXml
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
class NbkrExchangeRateSource(
    private val nbkrApiClient: NbkrApiClient,
    private val ratesProperties: RatesProperties,
    private val xmlMapper: XmlMapper,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val timeZone = ratesProperties.source.nbkr.timeZone
        val date = LocalDate.now(timeZone)
        log.info { "Trying to get exchange rates from nbkr for currency=$currencySymbolCode, date=$date" }
        val parsed = fetchDailyRates()
        val rates = buildRatesMap(parsed)
        if (rates.isEmpty()) {
            throw ExchangeRateSourceException("Unsuccessful response from NbkrApi")
        }
        val responseDate =
            try {
                parseDate(parsed)
            } catch (e: Exception) {
                throw ExchangeRateSourceException("Failed to parse date from NbkrApi", e)
            }
        val nextDayTimestamp = responseDate.atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info {
            "Exchange rates from nbkr have been retrieved, date=$responseDate, " +
                "exchangeRates=$rates, targetTimestamp=$nextDayTimestamp"
        }
        return ExchangeRates(
            rates = rates,
            timestamp = nextDayTimestamp,
        )
    }

    override fun getSourceId(): String = ExRateSources.NBKR

    private fun fetchDailyRates(): NbkrDailyRatesXml =
        try {
            xmlMapper.readValue(nbkrApiClient.getExchangeRates(), NbkrDailyRatesXml::class.java)
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Failed to get daily rates", e)
        }

    private fun buildRatesMap(root: NbkrDailyRatesXml): Map<String, BigDecimal> {
        val rates = mutableMapOf<String, BigDecimal>()
        for (item in root.currencies.orEmpty()) {
            val isoCode = item.isoCode?.trim { it <= ' ' }.orEmpty()
            val nominal = item.nominal?.trim { it <= ' ' }?.toBigDecimalOrNull()
            val value =
                item.value
                    ?.trim { it <= ' ' }
                    ?.replace(",", ".")
                    ?.toBigDecimalOrNull()
            if (isoCode.isBlank() || nominal == null || value == null || nominal.compareTo(BigDecimal.ZERO) == 0) {
                log.debug { "Skip malformed NBKR currency record: $item" }
                continue
            }
            rates[isoCode] = value.divide(nominal)
        }
        return rates
    }

    private fun parseDate(root: NbkrDailyRatesXml): LocalDate {
        val dateAttr = root.date?.trim { it <= ' ' }.orEmpty()
        require(dateAttr.isNotBlank()) { "Missing Date on CurrencyRates" }
        return LocalDate.parse(dateAttr, DATE_FORMATTER)
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
