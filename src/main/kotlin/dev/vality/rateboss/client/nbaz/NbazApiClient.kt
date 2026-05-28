package dev.vality.rateboss.client.nbaz

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class NbazApiClient(
    private val restTemplate: RestTemplate,
    private val ratesProperties: RatesProperties,
) {
    fun getExchangeRates(date: LocalDate): String {
        val url = buildUrl(date)
        return restTemplate.exchange<String>(url, HttpMethod.GET, HttpEntity.EMPTY).body!!
    }

    private fun buildUrl(date: LocalDate): String {
        val rootUrl = ratesProperties.source.nbaz.rootUrl
        val dateFormat = ratesProperties.source.nbaz.dateFormat
        return "${rootUrl}${date.format(DateTimeFormatter.ofPattern(dateFormat))}.xml"
    }
}
