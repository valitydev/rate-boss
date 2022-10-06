package dev.vality.rateboss.client

import dev.vality.rateboss.client.model.FixerLatestResponse
import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange
import org.springframework.web.util.UriComponentsBuilder

@Component
class FixerApiClient(
    private val ratesProperties: RatesProperties,
    private val restTemplate: RestTemplate
) {

    fun getLatest(base: String, symbols: String? = null): FixerLatestResponse {
        val urlTemplate = UriComponentsBuilder.fromHttpUrl(ratesProperties.source.fixer.rootUrl).apply {
            path("/latest")
            queryParam("base", base)
            if (symbols != null) {
                queryParam("symbols", symbols)
            }
            encode()
            toUriString()
        }
        val httpHeaders = HttpHeaders().apply {
            set("apiKey", ratesProperties.source.fixer.apiKey)
        }
        return restTemplate.exchange<FixerLatestResponse>(
            urlTemplate.toUriString(),
            HttpMethod.GET,
            HttpEntity(null, httpHeaders)
        ).body!!
    }
}
