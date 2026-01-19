package dev.vality.rateboss.source.impl

import dev.vality.rateboss.client.cbr.CbrApiClient
import dev.vality.rateboss.job.constant.ExRateSources
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.springframework.stereotype.Component
import org.w3c.dom.Element
import org.w3c.dom.Node
import java.io.ByteArrayInputStream
import java.math.BigDecimal
import java.nio.charset.StandardCharsets
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

private val log = KotlinLogging.logger {}

@Component
class CbrExchangeRateSource(
    private val cbrApiClient: CbrApiClient,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val time = Instant.now()
        log.info("Trying to get exchange rates from cbr for currency={}, time={}", currencySymbolCode, time)
        val response =
            try {
                cbrApiClient.getExchangeRates(time)
            } catch (e: Exception) {
                throw ExchangeRateSourceException("Remote client exception", e)
            }
        val rates =
            try {
                parseRates(response)
            } catch (e: Exception) {
                throw ExchangeRateSourceException("Failed to parse response from CbrApi", e)
            }
        if (rates.isEmpty()) {
            throw ExchangeRateSourceException("Unsuccessful response from CbrApi")
        }
        val responseDate =
            try {
                parseDate(response)
            } catch (e: Exception) {
                throw ExchangeRateSourceException("Failed to parse date from CbrApi", e)
            }
        val nextDayTimestamp = responseDate.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info("Exchange rates from cbr have been retrieved, date=$responseDate, exchangeRates=$rates, targetTimestamp=$nextDayTimestamp")
        return ExchangeRates(
            rates = rates,
            timestamp = nextDayTimestamp,
        )
    }

    override fun getSourceId(): String = ExRateSources.CBR

    private fun parseRates(xmlContent: String): Map<String, BigDecimal> {
        val document = parseXml(xmlContent)
        val items = document.getElementsByTagName("Valute")
        val rates = mutableMapOf<String, BigDecimal>()
        for (i in 0 until items.length) {
            val itemNode = items.item(i)
            if (itemNode.nodeType != Node.ELEMENT_NODE) {
                continue
            }
            val item = itemNode as Element
            val charCode =
                item
                    .getElementsByTagName("CharCode")
                    .item(0)
                    ?.textContent
                    ?.trim()
            val nominalStr =
                item
                    .getElementsByTagName("Nominal")
                    .item(0)
                    ?.textContent
                    ?.trim()
            val valueStr =
                item
                    .getElementsByTagName("Value")
                    .item(0)
                    ?.textContent
                    ?.trim()
            if (charCode.isNullOrBlank() || nominalStr.isNullOrBlank() || valueStr.isNullOrBlank()) {
                continue
            }
            val nominal = nominalStr.toBigDecimal()
            val value = valueStr.replace(",", ".").toBigDecimal()
            rates[charCode] = value.divide(nominal)
        }
        return rates
    }

    private fun parseDate(xmlContent: String): LocalDate {
        val document = parseXml(xmlContent)
        val root = document.documentElement
        val dateAttr = root.getAttribute("Date")
        return LocalDate.parse(dateAttr, DATE_FORMATTER)
    }

    private fun parseXml(xmlContent: String) =
        DocumentBuilderFactory
            .newInstance()
            .newDocumentBuilder()
            .parse(ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8)))

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    }
}
