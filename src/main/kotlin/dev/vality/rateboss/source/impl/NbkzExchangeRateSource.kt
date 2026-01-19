package dev.vality.rateboss.source.impl

import dev.vality.rateboss.client.nbkz.NbkzApiClient
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
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.ZoneOffset
import javax.xml.parsers.DocumentBuilderFactory

private val log = KotlinLogging.logger {}

@Component
class NbkzExchangeRateSource(
    private val nbkzApiClient: NbkzApiClient,
    private val ratesProperties: RatesProperties,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val timeZone = ratesProperties.source.nbkz.timeZone
        val date = LocalDate.now(timeZone)
        log.info("Trying to get exchange rates from nbkz for currency={}, date={}", currencySymbolCode, date)
        val response =
            try {
                nbkzApiClient.getExchangeRates(date)
            } catch (e: Exception) {
                throw ExchangeRateSourceException("Remote client exception", e)
            }
        val rates = parseRates(response)
        if (rates.isEmpty()) {
            throw ExchangeRateSourceException("Unsuccessful response from NbkzApi")
        }
        val nextDayTimestamp = date.plusDays(1).atStartOfDay().toEpochSecond(ZoneOffset.UTC)
        log.info(
            "Exchange rates from nbkz have been retrieved, date={}, exchangeRates={}, targetTimestamp={}",
            date,
            rates,
            nextDayTimestamp
        )
        return ExchangeRates(
            rates = rates,
            timestamp = nextDayTimestamp,
        )
    }

    override fun getSourceId(): String = ExRateSources.NBKZ

    private fun parseRates(xmlContent: String): Map<String, BigDecimal> {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        val document = builder.parse(ByteArrayInputStream(xmlContent.toByteArray(StandardCharsets.UTF_8)))
        val items = document.getElementsByTagName("item")
        val rates = mutableMapOf<String, BigDecimal>()
        for (i in 0 until items.length) {
            val itemNode = items.item(i)
            if (itemNode.nodeType != Node.ELEMENT_NODE) {
                continue
            }
            val item = itemNode as Element
            val titleNodes = item.getElementsByTagName("title")
            val descriptionNodes = item.getElementsByTagName("description")
            if (titleNodes.length == 0 || descriptionNodes.length == 0) {
                continue
            }
            val currencyCode = titleNodes.item(0).textContent.trim { it <= ' ' }
            val rateStr = descriptionNodes.item(0).textContent.trim { it <= ' ' }
            rates[currencyCode] = BigDecimal(rateStr)
        }
        return rates
    }
}
