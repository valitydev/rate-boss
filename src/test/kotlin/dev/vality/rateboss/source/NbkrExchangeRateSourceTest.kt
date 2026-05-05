package dev.vality.rateboss.source

import dev.vality.rateboss.client.nbkr.NbkrApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.impl.NbkrExchangeRateSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.web.client.ResourceAccessException
import java.math.BigDecimal

@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [NbkrApiClient::class, NbkrExchangeRateSource::class])
@Import(TestConfig::class)
class NbkrExchangeRateSourceTest {
    @Autowired
    lateinit var exchangeRateSource: ExchangeRateSource

    @MockitoBean
    lateinit var nbkrApiClient: NbkrApiClient

    @Test
    fun getFailedExchangeRate() {
        val currencySymbolCode = "KGS"
        whenever(nbkrApiClient.getExchangeRates()).thenThrow(ResourceAccessException("Error"))

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Failed to get daily rates", exception.message)
    }

    @Test
    fun getEmptyExchangeRate() {
        val currencySymbolCode = "KGS"
        whenever(nbkrApiClient.getExchangeRates()).thenReturn(
            """
            <?xml version="1.0" encoding="windows-1251" ?>
            <CurrencyRates Name="Daily Exchange Rates" Date="05.05.2026">
            </CurrencyRates>
            """.trimIndent(),
        )

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Unsuccessful response from NbkrApi", exception.message)
    }

    @Test
    fun getSuccessExchangeRate() {
        val currencySymbolCode = "KGS"
        whenever(nbkrApiClient.getExchangeRates()).thenReturn(
            """
            <?xml version="1.0" encoding="windows-1251" ?>
            <CurrencyRates Name="Daily Exchange Rates" Date="05.05.2026">
            <Currency ISOCode="USD">
            <Nominal>1</Nominal>
            <Value>87,4205</Value>
            </Currency>
            <Currency ISOCode="EUR">
            <Nominal>10</Nominal>
            <Value>1021,028</Value>
            </Currency>
            </CurrencyRates>
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertNotNull(exchangeRate)
        assertTrue(exchangeRate.rates.isNotEmpty())
        assertTrue(exchangeRate.rates.containsKey("USD"))
        assertEquals(BigDecimal("87.4205"), exchangeRate.rates["USD"])
        assertEquals(BigDecimal("102.1028"), exchangeRate.rates["EUR"])
    }

    @Test
    fun getSuccessExchangeRateWithMalformedCurrencyRecord() {
        val currencySymbolCode = "KGS"
        whenever(nbkrApiClient.getExchangeRates()).thenReturn(
            """
            <?xml version="1.0" encoding="windows-1251" ?>
            <CurrencyRates Name="Daily Exchange Rates" Date="05.05.2026">
            <Currency ISOCode="USD">
            <Nominal>1</Nominal>
            <Value>87,4205</Value>
            </Currency>
            <Currency ISOCode="BROKEN">
            <Nominal>0</Nominal>
            <Value>100,0000</Value>
            </Currency>
            </CurrencyRates>
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertEquals(BigDecimal("87.4205"), exchangeRate.rates["USD"])
        assertTrue(!exchangeRate.rates.containsKey("BROKEN"))
    }
}
