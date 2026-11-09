package dev.vality.rateboss.client.ecb

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.exchange

@Component
class EcbApiClient(
    private val restTemplate: RestTemplate,
    private val ratesProperties: RatesProperties,
) {
    fun getExchangeRates(): String {
        val bytes =
            checkNotNull(
                restTemplate.exchange<ByteArray>(ratesProperties.source.ecb.rootUrl, HttpMethod.GET, HttpEntity.EMPTY).body,
            ) { "Empty response from EcbApi" }
        val xml = bytes.toString(Charsets.UTF_8)
        check(xml.isNotBlank()) { "Empty response from EcbApi" }
        return xml
    }
}
