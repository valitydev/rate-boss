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
        val exRate = ExRate()
        exRate.sourceCurrencySymbolicCode = baseCurrencySymbolCode
        exRate.sourceCurrencyExponent = baseCurrencyExponent
        exRate.destinationCurrencySymbolicCode = exchangeRateMap.key
        exRate.destinationCurrencyExponent = exchangeRateMap.value.scale().toShort()
        val rational = exchangeRateMap.value.toRational()
        exRate.rationalP = rational.numerator
        exRate.rationalQ = rational.denominator
        exRate.rateTimestamp = LocalDateTime.ofEpochSecond(exchangeRateTimestamp, 0, ZoneOffset.UTC)
        return exRate
    }
}
