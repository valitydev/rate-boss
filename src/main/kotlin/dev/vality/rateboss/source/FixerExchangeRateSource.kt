package dev.vality.rateboss.source

import dev.vality.rateboss.client.FixerApiClient
import dev.vality.rateboss.source.model.ExchangeRates
import org.springframework.stereotype.Component

@Component
class FixerExchangeRateSource(
    private val fixerApiClient: FixerApiClient
) : ExchangeRateSource {

    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val fixerLatestResponse = try {
            fixerApiClient.getLatest(currencySymbolCode)
        } catch (e: Exception) {
            throw ExchangeRateSourceException(currencySymbolCode, "Remote client exception", e)
        }
        if (!fixerLatestResponse.success) {
            throw ExchangeRateSourceException(currencySymbolCode, "Unsuccessful response from FixerApi")
        }

        return ExchangeRates(
            rates = fixerLatestResponse.rates!!,
            timestamp = fixerLatestResponse.timestamp!!,

        )
    }
}
