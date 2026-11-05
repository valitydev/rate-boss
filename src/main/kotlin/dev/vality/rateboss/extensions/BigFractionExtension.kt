package dev.vality.rateboss.extensions

import org.apache.commons.math3.fraction.BigFraction

fun BigFraction.toRational(): Rational =
    try {
        Rational(numerator.longValueExact(), denominator.longValueExact())
    } catch (e: ArithmeticException) {
        throw ArithmeticException("Rational value does not fit in signed 64-bit integers: $this").apply { initCause(e) }
    }
