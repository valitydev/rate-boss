package dev.vality.rateboss.source.model

import java.math.BigDecimal

data class ExchangeRates(
    val rates: Map<String, BigDecimal>,
    val timestamp: Long,
)
