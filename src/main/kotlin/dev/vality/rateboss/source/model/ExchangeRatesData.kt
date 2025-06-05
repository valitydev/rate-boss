package dev.vality.rateboss.source.model

data class ExchangeRatesData(
    val destinationCurrencySymbolicCode: String,
    val destinationCurrencyExponent: Short,
    val exchangeRates: ExchangeRates,
    val sourceId: String,
)
