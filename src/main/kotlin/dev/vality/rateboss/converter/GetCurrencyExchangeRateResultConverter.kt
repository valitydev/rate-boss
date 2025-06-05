package dev.vality.rateboss.converter

import dev.vality.exrates.base.Rational
import dev.vality.exrates.service.CurrencyData
import dev.vality.exrates.service.GetCurrencyExchangeRateResult
import dev.vality.rateboss.converter.Constants.Companion.DATE_TIME_FORMAT
import dev.vality.rateboss.service.model.ExchangeRateData
import org.springframework.stereotype.Component
import java.time.format.DateTimeFormatter

@Component
class GetCurrencyExchangeRateResultConverter {
    fun convert(exchangeRateData: ExchangeRateData): GetCurrencyExchangeRateResult =
        GetCurrencyExchangeRateResult().apply {
            currencyData = CurrencyData()
            currencyData.sourceCurrency = exchangeRateData.sourceCurrencySymbolicCode
            currencyData.destinationCurrency = exchangeRateData.destinationCurrencySymbolicCode
            exchange_rate = Rational()
            exchange_rate.p = exchangeRateData.rationalP
            exchange_rate.q = exchangeRateData.rationalQ
            timestamp = exchangeRateData.rateTimestamp.format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
        }
}

class Constants {
    companion object {
        const val DATE_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss'Z'"
    }
}
