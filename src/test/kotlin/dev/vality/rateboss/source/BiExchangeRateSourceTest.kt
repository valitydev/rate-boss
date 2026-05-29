package dev.vality.rateboss.source

import dev.vality.rateboss.client.bi.BiApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.impl.BiExchangeRateSource
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
@ContextConfiguration(classes = [BiApiClient::class, BiExchangeRateSource::class])
@Import(TestConfig::class)
class BiExchangeRateSourceTest {
    @Autowired
    lateinit var exchangeRateSource: ExchangeRateSource

    @MockitoBean
    lateinit var biApiClient: BiApiClient

    @Test
    fun getFailedExchangeRate() {
        val currencySymbolCode = "IDR"
        whenever(biApiClient.getExchangeRates(any(), any(), any())).thenThrow(ResourceAccessException("Error"))

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertEquals("Remote client exception", exception.message)
    }

    @Test
    fun getEmptyExchangeRate() {
        val currencySymbolCode = "IDR"
        whenever(biApiClient.getExchangeRates(any(), any(), any())).thenReturn("<string></string>")

        val exception =
            org.junit.jupiter.api.assertThrows<ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }

        assertTrue(exception.message!!.startsWith("Unsuccessful response from BiApi for period"))
    }

    @Test
    fun getSuccessExchangeRate() {
        val currencySymbolCode = "IDR"
        whenever(biApiClient.getExchangeRates(any(), any(), any())).thenReturn(
            """
            <DataSet xmlns="http://tempuri.org/">
              <diffgr:diffgram xmlns:msdata="urn:schemas-microsoft-com:xml-msdata" xmlns:diffgr="urn:schemas-microsoft-com:xml-diffgram-v1">
                <NewDataSet xmlns="">
                  <Table>
                    <nil_subkurslokal>1.00</nil_subkurslokal>
                    <beli_subkurslokal>17654.28</beli_subkurslokal>
                    <jual_subkurslokal>17831.72</jual_subkurslokal>
                    <tgl_subkurslokal>2026-05-26T00:00:00+07:00</tgl_subkurslokal>
                  </Table>
                </NewDataSet>
              </diffgr:diffgram>
            </DataSet>
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertNotNull(exchangeRate)
        assertTrue(exchangeRate.rates.isNotEmpty())
        assertTrue(exchangeRate.rates.containsKey("USD"))
        assertEquals(BigDecimal("0.05607983974624994"), exchangeRate.rates["USD"])
    }

    @Test
    fun getSuccessExchangeRateWhenCurrentDayMissing() {
        val currencySymbolCode = "IDR"
        whenever(biApiClient.getExchangeRates(any(), any(), any())).thenReturn(
            """
            <DataSet xmlns="http://tempuri.org/">
              <diffgr:diffgram xmlns:msdata="urn:schemas-microsoft-com:xml-msdata" xmlns:diffgr="urn:schemas-microsoft-com:xml-diffgram-v1">
                <NewDataSet xmlns="">
                  <Table>
                    <nil_subkurslokal>1.00</nil_subkurslokal>
                    <beli_subkurslokal></beli_subkurslokal>
                    <jual_subkurslokal></jual_subkurslokal>
                    <tgl_subkurslokal>2026-05-27T00:00:00+07:00</tgl_subkurslokal>
                  </Table>
                  <Table>
                    <nil_subkurslokal>1.00</nil_subkurslokal>
                    <beli_subkurslokal>17654.28</beli_subkurslokal>
                    <jual_subkurslokal>17831.72</jual_subkurslokal>
                    <tgl_subkurslokal>2026-05-26T00:00:00+07:00</tgl_subkurslokal>
                  </Table>
                </NewDataSet>
              </diffgr:diffgram>
            </DataSet>
            """.trimIndent(),
        )

        val exchangeRate = exchangeRateSource.getExchangeRate(currencySymbolCode)

        assertEquals(BigDecimal("0.05607983974624994"), exchangeRate.rates["USD"])
    }
}
