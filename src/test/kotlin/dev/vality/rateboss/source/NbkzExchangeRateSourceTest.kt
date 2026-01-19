package dev.vality.rateboss.source

import dev.vality.rateboss.client.nbkz.NbkzApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.impl.NbkzExchangeRateSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.ResourceAccessException

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [NbkzApiClient::class, NbkzExchangeRateSource::class])
@Import(TestConfig::class)
class NbkzExchangeRateSourceTest {
    @Autowired
    lateinit var exchangeRateSource: ExchangeRateSource

    @MockitoBean
    lateinit var nbkzApiClient: NbkzApiClient

    @Test
    fun getFailedExchangeRate() {
        val currencySymbolCode = "KZT"
        whenever(nbkzApiClient.getExchangeRates(any())).thenThrow(ResourceAccessException("Error"))

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Remote client exception", exception.message)
    }

    @Test
    fun getEmptyExchangeRate() {
        val currencySymbolCode = "KZT"
        whenever(nbkzApiClient.getExchangeRates(any())).thenReturn("<rss></rss>")

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Unsuccessful response from NbkzApi", exception.message)
    }

    @Test
    fun getSuccessExchangeRate() {
        val currencySymbolCode = "KZT"
        whenever(nbkzApiClient.getExchangeRates(any())).thenReturn(
            """
            <rss>
              <channel>
                <item>
                  <title>USD</title>
                  <description>470.12</description>
                </item>
              </channel>
            </rss>
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertNotNull(exchangeRate)
        assertTrue(exchangeRate.rates.isNotEmpty())
        assertTrue(exchangeRate.rates.containsKey("USD"))
    }
}
