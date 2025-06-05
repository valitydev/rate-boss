package dev.vality.rateboss.service

import org.apache.commons.math3.fraction.BigFraction
import org.springframework.stereotype.Service

@Service
class ConversionService {
    fun convertAmount(
        amount: Long,
        rate: BigFraction,
    ): BigFraction =
        BigFraction(amount)
            .multiply(BigFraction(rate.numerator, rate.denominator))
}
