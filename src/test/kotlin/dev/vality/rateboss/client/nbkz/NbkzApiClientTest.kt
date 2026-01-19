package dev.vality.rateboss.client.nbkz

import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.impl.NbkzExchangeRateSource
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate

@Disabled("integration test")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [NbkzApiClient::class, NbkzExchangeRateSource::class])
@Import(TestConfig::class)
class NbkzApiClientTest {
    @Autowired
    lateinit var nbkzApiClient: NbkzApiClient

    @Autowired
    lateinit var nbkzExchangeRateSource: ExchangeRateSource

    @Test
    fun getExchangeRates() {
        val response = nbkzApiClient.getExchangeRates(LocalDate.now())

        assertNotNull(response)
        assertTrue(response.isNotBlank())
    }

    @Test
    fun getExchangeRatesViaSource() {
        val exchangeRates = nbkzExchangeRateSource.getExchangeRate("KZT")

        assertNotNull(exchangeRates)
        assertTrue(exchangeRates.rates.isNotEmpty())
    }
}
