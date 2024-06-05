package dev.vality.rateboss.source

import dev.vality.rateboss.source.model.ExchangeRates

interface ExchangeRateSource {
    fun getExchangeRate(currencySymbolCode: String): ExchangeRates

    fun getSourceId(): String
}
