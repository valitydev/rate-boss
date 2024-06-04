package dev.vality.rateboss.source.factory

import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.impl.CbrExchangeRateSource
import dev.vality.rateboss.source.impl.FixerExchangeRateSource
import org.springframework.context.ApplicationContext
import org.springframework.stereotype.Component

@Component
class ExchangeRateSourceFactory {

    fun getSources(applicationContext: ApplicationContext, destinationCurrencyCode: String): List<ExchangeRateSource> {
        return when (destinationCurrencyCode) {
            "RUB" -> listOf(applicationContext.getBean(CbrExchangeRateSource::class.java))
            "USD" -> listOf(applicationContext.getBean(FixerExchangeRateSource::class.java))
            else -> throw IllegalArgumentException("Currency: %s not supported".format(destinationCurrencyCode))
        }
    }
}
