package dev.vality.rateboss.client.ecb

import dev.vality.rateboss.config.TestConfig
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertSame
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
import org.springframework.test.web.client.response.MockRestResponseCreators.withException
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.HttpStatusCodeException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.io.IOException

class EcbApiClientTest {
    private lateinit var server: MockRestServiceServer
    private lateinit var client: EcbApiClient

    @BeforeEach
    fun setUp() {
        val restTemplate = RestTemplate()
        server = MockRestServiceServer.createServer(restTemplate)
        client = EcbApiClient(restTemplate, TestConfig().testRatesProperties())
    }

    @AfterEach
    fun verifyRequests() {
        server.verify()
    }

    @Test
    fun `fetch daily XML from the configured endpoint`() {
        val response = checkNotNull(javaClass.getResource("/exchange-rates/ecb-daily.xml")).readText()
        server
            .expect(requestTo(ENDPOINT))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(response, MediaType.TEXT_XML))

        assertEquals(response, client.getExchangeRates())
    }

    @ParameterizedTest
    @ValueSource(strings = ["", " ", "\n"])
    fun `reject an empty HTTP response`(response: String) {
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(response, MediaType.TEXT_XML))

        val exception = assertThrows<IllegalStateException> { client.getExchangeRates() }

        assertEquals("Empty response from EcbApi", exception.message)
    }

    @ParameterizedTest
    @ValueSource(ints = [404, 500])
    fun `propagate HTTP errors`(status: Int) {
        server.expect(requestTo(ENDPOINT)).andRespond(withStatus(HttpStatus.valueOf(status)))

        val exception = assertThrows<HttpStatusCodeException> { client.getExchangeRates() }

        assertEquals(status, exception.statusCode.value())
    }

    @Test
    fun `preserve the cause of a network failure`() {
        val cause = IOException("Connection failed")
        server.expect(requestTo(ENDPOINT)).andRespond(withException(cause))

        val exception = assertThrows<ResourceAccessException> { client.getExchangeRates() }

        assertSame(cause, exception.cause)
        assertInstanceOf(IOException::class.java, exception.cause)
    }

    companion object {
        private const val ENDPOINT = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    }
}
