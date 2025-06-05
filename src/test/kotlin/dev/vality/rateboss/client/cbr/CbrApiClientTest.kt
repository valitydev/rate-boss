package dev.vality.rateboss.client.cbr

import dev.vality.rateboss.config.TestConfig
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
@ContextConfiguration(classes = [CbrApiClient::class])
@Import(TestConfig::class)
class CbrApiClientTest {
    @Autowired
    lateinit var cbrApiClient: CbrApiClient

    @Test
    fun getExchangeRates() {
        val response = cbrApiClient.getExchangeRates(Instant.now())

        assertNotNull(response)
        assertTrue(response.currencies!!.isNotEmpty())
    }
}
