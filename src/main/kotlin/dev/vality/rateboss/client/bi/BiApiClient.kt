package dev.vality.rateboss.client.bi

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class BiApiClient(
    private val restTemplate: RestTemplate,
    private val ratesProperties: RatesProperties,
) {
    fun getExchangeRates(
        currencySymbolCode: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): String {
        val url = buildUrl(currencySymbolCode, startDate, endDate)
        val requestHeaders = HttpHeaders()
        requestHeaders["User-Agent"] = BROWSER_USER_AGENT
        return restTemplate.exchange<String>(url, HttpMethod.GET, HttpEntity<Any>(requestHeaders)).body!!
    }

    private fun buildUrl(
        currencySymbolCode: String,
        startDate: LocalDate,
        endDate: LocalDate,
    ): String =
        UriComponentsBuilder
            .fromUriString(ratesProperties.source.bi.rootUrl)
            .queryParam("mts", currencySymbolCode)
            .queryParam("startdate", startDate.format(BI_DATE_FORMATTER))
            .queryParam("enddate", endDate.format(BI_DATE_FORMATTER))
            .build()
            .toUriString()

    companion object {
        private val BI_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
    }
}
