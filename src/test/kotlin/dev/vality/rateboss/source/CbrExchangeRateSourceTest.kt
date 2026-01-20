package dev.vality.rateboss.source

import dev.vality.rateboss.client.cbr.CbrApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.impl.CbrExchangeRateSource
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
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
@ContextConfiguration(classes = [CbrApiClient::class, CbrExchangeRateSource::class])
@Import(TestConfig::class)
class CbrExchangeRateSourceTest {
    @Autowired
    lateinit var exchangeRateSource: ExchangeRateSource

    @MockitoBean
    lateinit var cbrApiClient: CbrApiClient

    @Test
    fun getFailedExchangeRate() {
        val currencySymbolCode = "RUB"
        whenever(cbrApiClient.getExchangeRates(any())).thenThrow(ResourceAccessException("Error"))

        val exception =
            assertThrows(ExchangeRateSourceException::class.java) {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Remote client exception", exception.message)
    }

    @Test
    fun getEmptyExchangeRate() {
        val currencySymbolCode = "RUB"
        whenever(cbrApiClient.getExchangeRates(any())).thenReturn("<ValCurs Date=\"01.01.2024\"></ValCurs>")

        val exception =
            assertThrows(ExchangeRateSourceException::class.java) {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Unsuccessful response from CbrApi", exception.message)
    }

    @Test
    fun getSuccessExchangeRate() {
        val currencySymbolCode = "RUB"
        whenever(cbrApiClient.getExchangeRates(any())).thenReturn(
            """
            <ValCurs Date="01.01.2024">
              <Valute ID="R01010">
                <NumCode>643</NumCode>
                <CharCode>RUB</CharCode>
                <Nominal>1</Nominal>
                <Name>Russian Ruble</Name>
                <Value>100,00</Value>
              </Valute>
            </ValCurs>
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertNotNull(exchangeRate)
        assertTrue(exchangeRate.rates.isNotEmpty())
        assertTrue(exchangeRate.rates.containsKey(currencySymbolCode))
    }
}
