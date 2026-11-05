package dev.vality.rateboss.source.model

import java.math.BigDecimal

data class ExchangeRates(
    val rates: Map<String, RateValue>,
    val timestamp: Long,
) {
    companion object {
        fun fromDecimals(
            rates: Map<String, BigDecimal>,
            timestamp: Long,
        ): ExchangeRates = ExchangeRates(rates.mapValues { RateValue.Decimal(it.value) }, timestamp)
    }
}
