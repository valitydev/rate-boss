package dev.vality.rateboss.client.nbkz

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class NbkzApiClient(
    private val restTemplate: RestTemplate,
    private val ratesProperties: RatesProperties,
) {
    fun getExchangeRates(date: LocalDate): String {
        val url = buildUrl(ratesProperties.source.nbkz.rootUrl, date, ratesProperties.source.nbkz.dateFormat)
        return restTemplate.exchange<String>(url, HttpMethod.GET, HttpEntity(null, null)).body!!
    }

    private fun buildUrl(
        endpoint: String,
        date: LocalDate,
        dateFormat: String,
    ): String =
        UriComponentsBuilder
            .fromUriString(endpoint)
            .queryParam("fdate", date.format(DateTimeFormatter.ofPattern(dateFormat)))
            .build()
            .toUriString()
}
