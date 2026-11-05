package dev.vality.rateboss.client.nbp

import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.impl.NbpExchangeRateSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.Clock

class NbpApiClientTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var client: NbpApiClient
    private lateinit var source: NbpExchangeRateSource

    @BeforeEach
    fun setUp() {
        val restTemplate = RestTemplate()
        server = MockRestServiceServer.createServer(restTemplate)
        client = NbpApiClient(restTemplate, TestConfig().testRatesProperties())
        source = NbpExchangeRateSource(client, TestConfig().testRatesProperties(), Clock.systemUTC())
    }

    @AfterEach
    fun verifyRequests() {
        server.verify()
    }

    @Test
    fun `fetch EUR quote as a decimal and ignore extra response fields`() {
        server
            .expect(requestTo("https://api.nbp.pl/api/exchangerates/rates/A/EUR/?format=json"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    {
                      "table": "A", "currency": "euro", "code": "EUR",
                      "rates": [{"no": "211/A/NBP/2026", "effectiveDate": "2026-10-30", "mid": 4.1234567890123456789}]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val response = client.getExchangeRates()

        assertEquals("EUR", response.code)
        assertEquals(BigDecimal("4.1234567890123456789"), response.rates!!.single().mid)
        assertEquals("2026-10-30", response.rates.single().effectiveDate)
    }

    @Test
    fun `wrap an empty HTTP response through the source`() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess())

        val exception = assertThrows<ExchangeRateSourceException> { source.getExchangeRate("PLN") }

        assertEquals("Failed to get daily rates from nbp", exception.message)
        assertInstanceOf(IllegalStateException::class.java, exception.cause)
    }

    @ParameterizedTest
    @ValueSource(ints = [404, 500])
    fun `wrap HTTP errors through the source`(status: Int) {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.valueOf(status)))

        val exception = assertThrows<ExchangeRateSourceException> { source.getExchangeRate("PLN") }

        assertEquals(status, assertInstanceOf(HttpStatusCodeException::class.java, exception.cause).statusCode.value())
    }

    @Test
    fun `wrap malformed JSON through the source`() {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess("{", MediaType.APPLICATION_JSON))

        val exception = assertThrows<ExchangeRateSourceException> { source.getExchangeRate("PLN") }

        assertInstanceOf(RestClientException::class.java, exception.cause)
    }

    companion object {
        private const val ENDPOINT = "https://api.nbp.pl/api/exchangerates/rates/A/EUR/?format=json"
    }
}
