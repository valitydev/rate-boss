package dev.vality.rateboss.client.nbaz

import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.impl.NbazExchangeRateSource
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
@ContextConfiguration(classes = [NbazApiClient::class, NbazExchangeRateSource::class])
@Import(TestConfig::class)
class NbazApiClientTest {
    @Autowired
    lateinit var nbazApiClient: NbazApiClient

    @Autowired
    lateinit var nbazExchangeRateSource: ExchangeRateSource

    @Test
    fun getExchangeRates() {
        val response = nbazApiClient.getExchangeRates(LocalDate.now())

        assertNotNull(response)
        assertTrue(response.isNotBlank())
    }

    @Test
    fun getExchangeRatesViaSource() {
        val exchangeRates = nbazExchangeRateSource.getExchangeRate("AZN")

        assertNotNull(exchangeRates)
        assertTrue(exchangeRates.rates.isNotEmpty())
    }
}
