package dev.vality.rateboss.source.impl

import dev.vality.rateboss.client.cbr.CbrApiClient
import dev.vality.rateboss.job.constant.ExRateSources
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.Instant

private val log = KotlinLogging.logger {}

@Component
class CbrExchangeRateSource(
    private val cbrApiClient: CbrApiClient
) : ExchangeRateSource {

    override fun getExchangeRate(currencySymbolCode: String): ExchangeRates {
        val time = Instant.now()
        log.info("Trying to get exchange rates from cbr for currency={}, time={}", currencySymbolCode, time)
        val response = try {
            cbrApiClient.getExchangeRates(time)
        } catch (e: Exception) {
            throw ExchangeRateSourceException("Remote client exception", e)
        }
        if (response.currencies.isNullOrEmpty()) {
            throw ExchangeRateSourceException("Unsuccessful response from CbrApi")
        }

        val rates: Map<String, BigDecimal> = response.currencies!!.associate {
            it.charCode!! to it.value!!.divide(it.nominal!!.toBigDecimal())
        }
        log.info("Exchange rates from cbr have been retrieved, time=$time, exchangeRates=$rates")
        return ExchangeRates(
            rates = rates,
            timestamp = time.toEpochMilli()
        )
    }

    override fun getSourceId(): String {
        return ExRateSources.CBR
    }
}
