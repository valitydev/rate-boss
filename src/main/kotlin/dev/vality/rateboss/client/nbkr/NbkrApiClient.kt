package dev.vality.rateboss.client.nbkr

import dev.vality.rateboss.config.properties.RatesProperties
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.stereotype.Component
import org.springframework.web.client.RestTemplate
import java.nio.charset.Charset

@Component
class NbkrApiClient(
    private val restTemplate: RestTemplate,
    private val ratesProperties: RatesProperties,
) {
    fun getExchangeRates(): String {
        val url = ratesProperties.source.nbkr.rootUrl
        val bytes =
            restTemplate
                .exchange(
                    url,
                    HttpMethod.GET,
                    HttpEntity.EMPTY,
                    ByteArray::class.java,
                ).body ?: byteArrayOf()
        return String(bytes, NBKR_XML_CHARSET)
    }

    companion object {
        private val NBKR_XML_CHARSET: Charset = Charset.forName("Windows-1251")
    }
}
