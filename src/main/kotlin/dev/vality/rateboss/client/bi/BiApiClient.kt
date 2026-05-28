package dev.vality.rateboss.client.bi

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate

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
        val opUrl = "${ratesProperties.source.bi.rootUrl}?op=getSubKursLokal3"
        val warmupHeaders = HttpHeaders()
        warmupHeaders.accept = listOf(MediaType.TEXT_HTML, MediaType.APPLICATION_XHTML_XML, MediaType.APPLICATION_XML)
        warmupHeaders["Accept-Language"] = "en-US,en;q=0.9"
        warmupHeaders["User-Agent"] = BROWSER_USER_AGENT
        val warmupResponse =
            restTemplate.exchange<String>(
                opUrl,
                HttpMethod.GET,
                HttpEntity<Any>(warmupHeaders),
            )
        val cookieHeader = warmupResponse.headers["Set-Cookie"]?.joinToString("; ") { it.substringBefore(";") }

        val url = buildUrl(currencySymbolCode, startDate, endDate)
        val requestHeaders = HttpHeaders()
        requestHeaders.accept = listOf(MediaType.APPLICATION_XML, MediaType.TEXT_XML, MediaType.ALL)
        requestHeaders["Referer"] = opUrl
        requestHeaders["User-Agent"] = BROWSER_USER_AGENT
        if (!cookieHeader.isNullOrBlank()) {
            requestHeaders["Cookie"] = cookieHeader
        }
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
            .queryParam("startdate", startDate)
            .queryParam("enddate", endDate)
            .build()
            .toUriString()

    companion object {
        private const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36"
    }
}
