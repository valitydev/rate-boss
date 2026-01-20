package dev.vality.rateboss.client.cbr

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class CbrApiClient(
    private val restTemplate: RestTemplate,
    private val ratesProperties: RatesProperties,
) {
    fun getExchangeRates(time: Instant): String {
        val baseUrl = ratesProperties.source.cbr.rootUrl
        val timezone = ratesProperties.source.cbr.timeZone
        val date = time.atZone(timezone).toLocalDate()
        val url = buildUrl(baseUrl, date)
        return restTemplate.exchange<String>(url, HttpMethod.GET, HttpEntity(null, null)).body!!
    }

    private fun buildUrl(
        endpoint: String,
        date: LocalDate,
    ): String =
        UriComponentsBuilder
            .fromUriString(endpoint)
            .queryParam("date_req", date.format(DATE_TIME_FORMATTER))
            .build()
            .toUriString()

    companion object {
        val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    }
}
