package dev.vality.rateboss.source

import dev.vality.rateboss.client.nbuz.NbuzApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.model.NbuzRateItem
import dev.vality.rateboss.model.NbuzRatesEntry
import dev.vality.rateboss.model.NbuzRatesResponse
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

        assertEquals("Failed to get daily rates from nbuz", exception.message)
    }

    @Test
    fun getEmptyExchangeRate() {
        val currencySymbolCode = "UZS"
        whenever(nbuzApiClient.getExchangeRates(any())).thenReturn(NbuzRatesResponse(data = emptyList()))

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
            NbuzRatesResponse(
                data =
                    listOf(
                        NbuzRatesEntry(
                            rates =
                                listOf(
                                    NbuzRateItem(
                                        rateCode = "USD",
                                        rateSb = "11979,64",
                                        rateEquivalent = "1",
                                    ),
                                    NbuzRateItem(
                                        rateCode = "JPY",
                                        rateSb = "150,58",
                                        rateEquivalent = "2",
                                    ),
                                ),
                        ),
                    ),
            ),
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
            NbuzRatesResponse(
                data =
                    listOf(
                        NbuzRatesEntry(
                            rates =
                                listOf(
                                    NbuzRateItem(
                                        rateCode = "USD",
                                        rateSb = "11979,64",
                                        rateEquivalent = "1",
                                    ),
                                    NbuzRateItem(
                                        rateCode = "BROKEN",
                                        rateSb = "100",
                                        rateEquivalent = "0",
                                    ),
                                ),
                        ),
                    ),
            ),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertEquals(BigDecimal("11979.64"), exchangeRate.rates["USD"])
        assertTrue(!exchangeRate.rates.containsKey("BROKEN"))
    }
}
