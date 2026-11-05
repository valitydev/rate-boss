package dev.vality.rateboss.source.model

import dev.vality.rateboss.extensions.Rational
import dev.vality.rateboss.extensions.toBigFraction
import org.apache.commons.math3.fraction.BigFraction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal
import java.math.BigInteger

class RateValueTest {
    @ParameterizedTest
    @CsvSource("4.2500, 17, 4", "4E+1, 40, 1", "1.25E-2, 1, 80", "0, 0, 1")
    fun `convert decimal notation to an exact fraction`(
        value: BigDecimal,
        numerator: Long,
        denominator: Long,
    ) {
        assertEquals(BigFraction(numerator, denominator), value.toBigFraction())
    }

    @Test
    fun `preserve the existing decimal numerator and scale`() {
        assertEquals(Rational(1700, 1000), RateValue.Decimal(BigDecimal("1.700")).toRational())
    }

    @Test
    fun `reduce fractions before checking the 64 bit limit`() {
        val large = BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TWO)

        assertEquals(Rational(Long.MAX_VALUE, 1), RateValue.Fraction(BigFraction(large, BigInteger.TWO)).toRational())
    }

    @Test
    fun `reject an unrepresentable numerator or denominator`() {
        val beyondLong = BigInteger.valueOf(Long.MAX_VALUE).add(BigInteger.ONE)
        for (fraction in listOf(BigFraction(beyondLong, BigInteger.ONE), BigFraction(BigInteger.ONE, beyondLong))) {
            val exception = assertThrows<ArithmeticException> { RateValue.Fraction(fraction).toRational() }

            assertTrue(exception.message!!.contains("64-bit"))
        }
    }
}
