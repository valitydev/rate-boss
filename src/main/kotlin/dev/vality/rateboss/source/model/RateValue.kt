package dev.vality.rateboss.source.model

import dev.vality.rateboss.extensions.Rational
import dev.vality.rateboss.extensions.toRational
import org.apache.commons.math3.fraction.BigFraction
import java.math.BigDecimal

sealed interface RateValue {
    fun toRational(): Rational

    data class Decimal(
        val value: BigDecimal,
    ) : RateValue {
        override fun toRational(): Rational = value.toRational()
    }

    data class Fraction(
        val value: BigFraction,
    ) : RateValue {
        override fun toRational(): Rational = value.toRational()
    }
}
