package dev.vality.rateboss.client.nbp

import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.model.NbpRatesResponse
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder

@Component
class NbpApiClient(
    private val restTemplate: RestTemplate,
    private val ratesProperties: RatesProperties,
) {
    fun getExchangeRates(): NbpRatesResponse {
        val url =
            UriComponentsBuilder
                .fromUriString(ratesProperties.source.nbp.rootUrl)
                .queryParam("format", "json")
                .build()
                .toUriString()
        return checkNotNull(restTemplate.exchange<NbpRatesResponse>(url, HttpMethod.GET, HttpEntity.EMPTY).body) {
            "Empty response from NbpApi"
        }
    }
}
