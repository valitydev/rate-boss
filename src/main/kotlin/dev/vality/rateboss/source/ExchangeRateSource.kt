package dev.vality.rateboss.source

import dev.vality.rateboss.source.model.ExchangeRates

fun interface ExchangeRateSource {
    fun getExchangeRate(currencySymbolCode: String): ExchangeRates
}
