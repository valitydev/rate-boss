package dev.vality.rateboss.source

import dev.vality.rateboss.client.cbr.CbrApiClient
import dev.vality.rateboss.client.cbr.model.CbrCurrencyData
import dev.vality.rateboss.client.cbr.model.CbrExchangeRateData
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.impl.CbrExchangeRateSource
import org.junit.jupiter.api.Assertions.*
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
import java.time.LocalDate

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
        val cbrExchangeRateData = buildCbrExchangeRateData()
        cbrExchangeRateData.currencies = emptyList()
        whenever(cbrApiClient.getExchangeRates(any())).thenReturn(cbrExchangeRateData)

        val exception =
            assertThrows(ExchangeRateSourceException::class.java) {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Unsuccessful response from CbrApi", exception.message)
    }

    @Test
    fun getSuccessExchangeRate() {
        val currencySymbolCode = "RUB"
        val cbrExchangeRateData = buildCbrExchangeRateData()
        cbrExchangeRateData.currencies = listOf(buildCbrCurrencyData(currencySymbolCode))
        whenever(cbrApiClient.getExchangeRates(any())).thenReturn(cbrExchangeRateData)

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertNotNull(exchangeRate)
        assertTrue(exchangeRate.rates.isNotEmpty())
        assertTrue(exchangeRate.rates.containsKey(currencySymbolCode))
    }

    private fun buildCbrExchangeRateData(): CbrExchangeRateData {
        val cbrExchangeRateData = CbrExchangeRateData()
        cbrExchangeRateData.name = "Foreign Currency Market"
        cbrExchangeRateData.date = LocalDate.now()
        return cbrExchangeRateData
    }

    private fun buildCbrCurrencyData(currencySymbolCode: String): CbrCurrencyData {
        val currencyData = CbrCurrencyData()
        currencyData.value = BigDecimal.valueOf(100L)
        currencyData.nominal = 1
        currencyData.charCode = currencySymbolCode
        currencyData.numCode = 100
        currencyData.id = "R01010"
        return currencyData
    }
}
