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

    @ParameterizedTest
    @ValueSource(strings = ["4.3916", "4.1234567890123456789"])
    fun `fetch table quotes as exact decimals and ignore extra response fields`(mid: String) {
        server
            .expect(requestTo("https://api.nbp.pl/api/exchangerates/tables/A/?format=json"))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """
                    [{
                      "table": "A", "no": "216/A/NBP/2026", "effectiveDate": "2026-11-05",
                      "rates": [
                        {"currency": "euro", "code": "EUR", "mid": $mid},
                        {"currency": "dolar amerykański", "code": "USD", "mid": 3.9101}
                      ]
                    }]
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val response = client.getExchangeRates()

        val table = response.single()
        assertEquals("2026-11-05", table.effectiveDate)
        assertEquals(listOf("EUR", "USD"), table.rates!!.map { it.code })
        assertEquals(listOf(BigDecimal(mid), BigDecimal("3.9101")), table.rates.map { it.mid })
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
        private const val ENDPOINT = "https://api.nbp.pl/api/exchangerates/tables/A/?format=json"
    }
}
