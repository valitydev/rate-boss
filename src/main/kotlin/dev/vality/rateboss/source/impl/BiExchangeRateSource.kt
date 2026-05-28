package dev.vality.rateboss.source.impl

import dev.vality.rateboss.client.bi.BiApiClient
import dev.vality.rateboss.config.properties.RatesProperties
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
import java.math.MathContext
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import javax.xml.parsers.DocumentBuilderFactory

private val log = KotlinLogging.logger {}

@Component
class BiExchangeRateSource(
    private val biApiClient: BiApiClient,
    private val ratesProperties: RatesProperties,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val timeZone = ratesProperties.source.bi.timeZone
        val date = LocalDate.now(timeZone)
        val lookbackDays = ratesProperties.source.bi.lookbackDays
        val targetCurrencies = ratesProperties.source.bi.targetCurrencies
        val startDate = date.minusDays(lookbackDays)
        log.info { "Trying to get exchange rates from bi for currency=$currencySymbolCode, date=$date" }

        val rates = mutableMapOf<String, BigDecimal>()
        for (targetCurrency in targetCurrencies) {
            val response =
                try {
                    biApiClient.getExchangeRates(
                        currencySymbolCode = targetCurrency,
                        startDate = startDate,
                        endDate = date,
                    )
                } catch (e: Exception) {
                    throw ExchangeRateSourceException("Remote client exception", e)
                }

            val parsedRate =
                try {
                    parseRate(response, targetCurrency)
                } catch (e: Exception) {
                    throw ExchangeRateSourceException("Failed to parse response from BiApi", e)
                }
            if (parsedRate != null) {
                rates[targetCurrency] = parsedRate
            }
        }

        if (rates.isEmpty()) {
            throw ExchangeRateSourceException("Unsuccessful response from BiApi for period $startDate..$date")
        }

        val nextDayTimestamp = date.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info { "Exchange rates from bi have been retrieved, date=$date, exchangeRates=$rates, targetTimestamp=$nextDayTimestamp" }

        return ExchangeRates(
            rates = rates,
            timestamp = nextDayTimestamp,
        )
    }

    override fun getSourceId(): String = ExRateSources.BI

    private fun parseRate(
        xmlContent: String,
        targetCurrency: String,
    ): BigDecimal? {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .newDocumentBuilder()
                .parse(ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8)))

        val tables = document.getElementsByTagName("Table")
        var lastTimestamp: OffsetDateTime? = null
        var idrPerTargetCurrency: BigDecimal? = null

        for (i in 0 until tables.length) {
            val tableNode = tables.item(i)
            if (tableNode.nodeType != Node.ELEMENT_NODE) {
                continue
            }
            val table = tableNode as Element
            val parsedDate =
                extractTextByTag(table, "tgl_subkurslokal")
                    ?.let { OffsetDateTime.parse(it.trim()) }
                    ?: continue
            val nominal = extractDecimalByTag(table, "nil_subkurslokal")
            val sell = extractDecimalByTag(table, "jual_subkurslokal")
            if (nominal == null || nominal <= BigDecimal.ZERO || sell == null || sell <= BigDecimal.ZERO) {
                continue
            }
            val normalizedSell = sell.divide(nominal, MathContext.DECIMAL64)
            if (lastTimestamp == null || parsedDate.isAfter(lastTimestamp)) {
                lastTimestamp = parsedDate
                idrPerTargetCurrency = normalizedSell
            }
        }

        val rate = idrPerTargetCurrency ?: return null
        log.debug { "BI rate parsed for targetCurrency=$targetCurrency: idrPerTargetCurrency=$rate" }
        return BigDecimal.ONE.divide(rate, MathContext.DECIMAL64)
    }

    private fun extractDecimalByTag(
        parent: Element,
        tag: String,
    ): BigDecimal? =
        extractTextByTag(parent, tag)
            ?.normalizeDecimal()
            ?.toBigDecimalOrNull()

    private fun extractTextByTag(
        parent: Element,
        tag: String,
    ): String? = parent.getElementsByTagName(tag).item(0)?.textContent

    private fun String?.normalizeDecimal(): String =
        this
            ?.replace(" ", "")
            ?.replace(",", ".")
            ?.trim()
            .orEmpty()
}
