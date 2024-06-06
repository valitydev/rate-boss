package dev.vality.rateboss.converter

import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import dev.vality.rateboss.extensions.toRational
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.money.Monetary

private const val DEFAULT_EXPONENT = 2

@Component
class ExRateConverter {

    fun convert(
        baseCurrencySymbolCode: String,
        baseCurrencyExponent: Short,
        exchangeRateMapEntry: Map.Entry<String, BigDecimal>,
        exchangeRateTimestamp: Long,
        sourceId: String
    ): ExRate {
        val exponent = try {
            Monetary.getCurrency(exchangeRateMapEntry.key).defaultFractionDigits
        } catch (e: Exception) {
            DEFAULT_EXPONENT
        }
        return ExRate().apply {
            destinationCurrencySymbolicCode = baseCurrencySymbolCode
            destinationCurrencyExponent = baseCurrencyExponent
            sourceCurrencySymbolicCode = exchangeRateMapEntry.key
            sourceCurrencyExponent = exponent.toShort()
            val rational = exchangeRateMapEntry.value.toRational()
            rationalP = rational.numerator
            rationalQ = rational.denominator
            rateTimestamp = LocalDateTime.ofEpochSecond(exchangeRateTimestamp, 0, ZoneOffset.UTC)
            source = sourceId
        }
    }
}
