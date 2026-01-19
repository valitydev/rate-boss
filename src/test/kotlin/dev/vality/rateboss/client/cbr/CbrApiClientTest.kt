package dev.vality.rateboss.client.cbr

import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.impl.CbrExchangeRateSource
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.Instant

@Disabled("integration test")
@ExtendWith(SpringExtension::class)
@ContextConfiguration(classes = [CbrApiClient::class, CbrExchangeRateSource::class])
@Import(TestConfig::class)
class CbrApiClientTest {
    @Autowired
    lateinit var cbrApiClient: CbrApiClient

    @Autowired
    lateinit var cbrExchangeRateSource: ExchangeRateSource

    @Test
    fun getExchangeRates() {
        val response = cbrApiClient.getExchangeRates(Instant.now())

        assertNotNull(response)
        assertTrue(response.isNotBlank())
    }

    @Test
    fun getExchangeRatesViaSource() {
        val exchangeRates = cbrExchangeRateSource.getExchangeRate("RUB")

        assertNotNull(exchangeRates)
        assertTrue(exchangeRates.rates.isNotEmpty())
    }
}
