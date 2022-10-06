package dev.vality.rateboss.extensions

import java.math.BigDecimal
import java.math.BigInteger


fun BigDecimal.toRational(): Rational {
    val denominator: BigInteger = if (this.scale() > 0) BigInteger.TEN.pow(this.scale()) else BigInteger.ONE
    val numerator: BigInteger = this.remainder(BigDecimal.ONE)
        .movePointRight(this.scale()).toBigInteger()
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
    val denominator: Long
)
