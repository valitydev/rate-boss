package dev.vality.rateboss.service.model

import java.time.LocalDateTime

data class TimestampExchangeRateRequest(
    val sourceCurrency: String,
    val destinationCurrency: String,
    val rateTimestamp: LocalDateTime,
    val source: String,
)
