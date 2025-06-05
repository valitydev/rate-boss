package dev.vality.rateboss.service.model

import java.time.LocalDateTime

data class ExchangeRateData(
    val sourceCurrencySymbolicCode: String,
    val destinationCurrencySymbolicCode: String,
    val rationalP: Long,
    val rationalQ: Long,
    val rateTimestamp: LocalDateTime,
    val source: String,
)
