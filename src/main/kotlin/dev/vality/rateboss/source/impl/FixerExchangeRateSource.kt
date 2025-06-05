package dev.vality.rateboss.source.impl

import dev.vality.rateboss.client.fixer.FixerApiClient
import dev.vality.rateboss.job.constant.ExRateSources
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.model.ExchangeRates
import org.springframework.stereotype.Component

@Component
class FixerExchangeRateSource(
    private val fixerApiClient: FixerApiClient,
) : ExchangeRateSource {
    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val fixerLatestResponse =
            try {
                fixerApiClient.getLatest(currencySymbolCode)
            } catch (e: Exception) {
                throw ExchangeRateSourceException("Remote client exception", e)
            }
        if (!fixerLatestResponse.success) {
            throw ExchangeRateSourceException("Unsuccessful response from FixerApi")
        }

        return ExchangeRates(
            rates = fixerLatestResponse.rates!!,
            timestamp = fixerLatestResponse.timestamp!!,
        )
    }

    override fun getSourceId(): String = ExRateSources.FIXER
}
