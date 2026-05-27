package dev.vality.rateboss.source

import dev.vality.rateboss.client.nbuz.NbuzApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.impl.NbuzExchangeRateSource
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
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [NbuzApiClient::class, NbuzExchangeRateSource::class])
@Import(TestConfig::class)
class NbuzExchangeRateSourceTest {
    @Autowired
    lateinit var exchangeRateSource: ExchangeRateSource

    @MockitoBean
    lateinit var nbuzApiClient: NbuzApiClient

    @Test
    fun getFailedExchangeRate() {
        val currencySymbolCode = "UZS"
        whenever(nbuzApiClient.getExchangeRates(any())).thenThrow(ResourceAccessException("Error"))

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Failed to get daily rates", exception.message)
    }

    @Test
    fun getEmptyExchangeRate() {
        val currencySymbolCode = "UZS"
        whenever(nbuzApiClient.getExchangeRates(any())).thenReturn("""{"data": []}""")

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Unsuccessful response from NbuzApi", exception.message)
    }

    @Test
    fun getSuccessExchangeRate() {
        val currencySymbolCode = "UZS"
        whenever(nbuzApiClient.getExchangeRates(any())).thenReturn(
            """
            {
              "data": [
                {
                  "rates": [
                    {
                      "rate_code": "USD",
                      "rate_sb": "11979,64",
                      "rate_equivalent": "1"
                    },
                    {
                      "rate_code": "JPY",
                      "rate_sb": "150,58",
                      "rate_equivalent": "2"
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertNotNull(exchangeRate)
        assertTrue(exchangeRate.rates.isNotEmpty())
        assertEquals(BigDecimal("11979.64"), exchangeRate.rates["USD"])
        assertEquals(BigDecimal("75.29"), exchangeRate.rates["JPY"])
    }

    @Test
    fun getSuccessExchangeRateWithMalformedCurrencyRecord() {
        val currencySymbolCode = "UZS"
        whenever(nbuzApiClient.getExchangeRates(any())).thenReturn(
            """
            {
              "data": [
                {
                  "rates": [
                    {
                      "rate_code": "USD",
                      "rate_sb": "11979,64",
                      "rate_equivalent": "1"
                    },
                    {
                      "rate_code": "BROKEN",
                      "rate_sb": "100",
                      "rate_equivalent": "0"
                    }
                  ]
                }
              ]
            }
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertEquals(BigDecimal("11979.64"), exchangeRate.rates["USD"])
        assertTrue(!exchangeRate.rates.containsKey("BROKEN"))
    }
}
