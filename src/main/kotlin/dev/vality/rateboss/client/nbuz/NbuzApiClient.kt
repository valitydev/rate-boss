package dev.vality.rateboss.client.nbuz

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder
import java.time.LocalDate

@Component
class NbuzApiClient(
    private val restTemplate: RestTemplate,
    private val ratesProperties: RatesProperties,
) {
    fun getExchangeRates(date: LocalDate): String {
        val url = buildUrl(date)
        return restTemplate.exchange<String>(url, HttpMethod.GET, HttpEntity.EMPTY).body!!
    }

    private fun buildUrl(date: LocalDate): String =
        UriComponentsBuilder
            .fromUriString(ratesProperties.source.nbuz.rootUrl)
            .queryParam("filter[locale:contains]", ratesProperties.source.nbuz.locale)
            .queryParam("filter[data_sozdaniya:contains]", date)
            .build()
            .toUriString()
}
