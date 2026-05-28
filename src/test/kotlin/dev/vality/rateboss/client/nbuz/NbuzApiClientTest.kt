package dev.vality.rateboss.client.nbuz

import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.impl.NbuzExchangeRateSource
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
@ContextConfiguration(classes = [NbuzApiClient::class, NbuzExchangeRateSource::class])
@Import(TestConfig::class)
class NbuzApiClientTest {
    @Autowired
    lateinit var nbuzApiClient: NbuzApiClient

    @Autowired
    lateinit var nbuzExchangeRateSource: ExchangeRateSource

    @Test
    fun getExchangeRates() {
        val response = nbuzApiClient.getExchangeRates(LocalDate.now())

        assertNotNull(response)
        assertNotNull(response.data)
    }

    @Test
    fun getExchangeRatesViaSource() {
        val exchangeRates = nbuzExchangeRateSource.getExchangeRate("UZS")

        assertNotNull(exchangeRates)
        assertTrue(exchangeRates.rates.isNotEmpty())
    }
}
