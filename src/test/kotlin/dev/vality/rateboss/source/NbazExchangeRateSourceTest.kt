package dev.vality.rateboss.source

import dev.vality.rateboss.client.nbaz.NbazApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.impl.NbazExchangeRateSource
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
@ContextConfiguration(classes = [NbazApiClient::class, NbazExchangeRateSource::class])
@Import(TestConfig::class)
class NbazExchangeRateSourceTest {
    @Autowired
    lateinit var exchangeRateSource: ExchangeRateSource

    @MockitoBean
    lateinit var nbazApiClient: NbazApiClient

    @Test
    fun getFailedExchangeRate() {
        val currencySymbolCode = "AZN"
        whenever(nbazApiClient.getExchangeRates(any())).thenThrow(ResourceAccessException("Error"))

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Failed to get daily rates", exception.message)
    }

    @Test
    fun getEmptyExchangeRate() {
        val currencySymbolCode = "AZN"
        whenever(nbazApiClient.getExchangeRates(any())).thenReturn(
            """
            <ValCurs Date="26.05.2026" Name="AZN məzənnələri">
              <ValType Type="Bank metalları" />
            </ValCurs>
            """.trimIndent(),
        )

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Unsuccessful response from NbazApi", exception.message)
    }

    @Test
    fun getSuccessExchangeRate() {
        val currencySymbolCode = "AZN"
        whenever(nbazApiClient.getExchangeRates(any())).thenReturn(
            """
            <ValCurs Date="26.05.2026" Name="AZN məzənnələri">
              <ValType Type="Bank metalları">
                <Valute Code="XAU">
                  <Nominal>1 t.u.</Nominal>
                  <Value>7693.044</Value>
                </Valute>
              </ValType>
              <ValType Type="Xarici valyutalar">
                <Valute Code="USD">
                  <Nominal>1</Nominal>
                  <Value>1.7</Value>
                </Valute>
                <Valute Code="KRW">
                  <Nominal>100</Nominal>
                  <Value>0.1128</Value>
                </Valute>
              </ValType>
            </ValCurs>
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertNotNull(exchangeRate)
        assertTrue(exchangeRate.rates.isNotEmpty())
        assertEquals(BigDecimal("1.7"), exchangeRate.rates["USD"])
        assertEquals(BigDecimal("0.001128"), exchangeRate.rates["KRW"])
        assertTrue(!exchangeRate.rates.containsKey("XAU"))
    }

    @Test
    fun getSuccessExchangeRateWithMalformedCurrencyRecord() {
        val currencySymbolCode = "AZN"
        whenever(nbazApiClient.getExchangeRates(any())).thenReturn(
            """
            <ValCurs Date="26.05.2026" Name="AZN məzənnələri">
              <ValType Type="Xarici valyutalar">
                <Valute Code="USD">
                  <Nominal>1</Nominal>
                  <Value>1.7</Value>
                </Valute>
                <Valute Code="BROKEN">
                  <Nominal>0</Nominal>
                  <Value>1.0</Value>
                </Valute>
              </ValType>
            </ValCurs>
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertEquals(BigDecimal("1.7"), exchangeRate.rates["USD"])
        assertTrue(!exchangeRate.rates.containsKey("BROKEN"))
    }
}
