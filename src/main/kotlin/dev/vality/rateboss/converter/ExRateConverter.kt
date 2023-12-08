package dev.vality.rateboss.converter

import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import dev.vality.rateboss.extensions.toRational
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset

@Component
class ExRateConverter {

    fun convert(
        baseCurrencySymbolCode: String,
        baseCurrencyExponent: Short,
        exchangeRateMap: Map.Entry<String, BigDecimal>,
        exchangeRateTimestamp: Long
    ): ExRate {
        return ExRate().apply {
            sourceCurrencySymbolicCode = baseCurrencySymbolCode
            sourceCurrencyExponent = baseCurrencyExponent
            destinationCurrencySymbolicCode = exchangeRateMap.key
            destinationCurrencyExponent = exchangeRateMap.value.scale().toShort()
            val rational = exchangeRateMap.value.toRational()
            rationalP = rational.numerator
            rationalQ = rational.denominator
            rateTimestamp = LocalDateTime.ofEpochSecond(exchangeRateTimestamp, 0, ZoneOffset.UTC)
        }
    }
}
