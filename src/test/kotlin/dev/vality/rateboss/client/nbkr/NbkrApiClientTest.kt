package dev.vality.rateboss.client.nbkr

import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.impl.NbkrExchangeRateSource
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension

@Disabled("integration test")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [NbkrApiClient::class, NbkrExchangeRateSource::class])
@Import(TestConfig::class)
class NbkrApiClientTest {
    @Autowired
    lateinit var nbkrApiClient: NbkrApiClient

    @Autowired
    lateinit var nbkrExchangeRateSource: ExchangeRateSource

    @Test
    fun getExchangeRates() {
        val response = nbkrApiClient.getExchangeRates()

        assertNotNull(response)
        assertTrue(response.isNotBlank())
    }

    @Test
    fun getExchangeRatesViaSource() {
        val exchangeRates = nbkrExchangeRateSource.getExchangeRate("KGS")

        assertNotNull(exchangeRates)
        assertTrue(exchangeRates.rates.isNotEmpty())
    }
}
