package dev.vality.rateboss.extensions

import org.apache.commons.math3.fraction.BigFraction
import java.math.BigDecimal
import java.math.BigInteger

fun BigDecimal.toRational(): Rational {
    val denominator: BigInteger = if (this.scale() > 0) BigInteger.TEN.pow(this.scale()) else BigInteger.ONE
    val numerator: BigInteger =
        this
            .remainder(BigDecimal.ONE)
            .movePointRight(this.scale())
            .toBigInteger()
            .add(this.toBigInteger().multiply(denominator))

    if (numerator > BigInteger.valueOf(Long.MAX_VALUE)) {
        throw ArithmeticException("Numerator out of long range: $numerator")
    }
    if (denominator > BigInteger.valueOf(Long.MAX_VALUE)) {
        throw ArithmeticException("Denominator out of long range: $denominator")
    }

    return Rational(numerator.longValueExact(), denominator.longValueExact())
}

data class Rational(
    val numerator: Long,
    val denominator: Long,
)

fun BigDecimal.toBigFraction(): BigFraction {
    val normalized = stripTrailingZeros()
    val scale = normalized.scale()
    return if (scale >= 0) {
        BigFraction(normalized.unscaledValue(), BigInteger.TEN.pow(scale))
    } else {
        BigFraction(normalized.unscaledValue().multiply(BigInteger.TEN.pow(-scale)), BigInteger.ONE)
    }
}

fun BigFraction.toRational(): Rational =
    try {
        Rational(numerator.longValueExact(), denominator.longValueExact())
    } catch (e: ArithmeticException) {
        throw ArithmeticException("Rational value does not fit in signed 64-bit integers: $this").apply { initCause(e) }
    }
