package dev.vality.rateboss.client.bi

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
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
        val url = buildUrl(currencySymbolCode, startDate, endDate)
        return restTemplate.exchange<String>(url, HttpMethod.GET, HttpEntity.EMPTY).body!!
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
}
