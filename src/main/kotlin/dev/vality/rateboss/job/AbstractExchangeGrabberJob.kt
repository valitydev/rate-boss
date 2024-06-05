package dev.vality.rateboss.job

import dev.vality.rateboss.service.ExchangeDaoService
import dev.vality.rateboss.service.ExchangeEventService
import dev.vality.rateboss.source.model.ExchangeRates
import dev.vality.rateboss.source.model.ExchangeRatesData
import mu.KotlinLogging
import org.springframework.context.ApplicationContext
import org.springframework.scheduling.quartz.QuartzJobBean

private val log = KotlinLogging.logger {}

abstract class AbstractExchangeGrabberJob : QuartzJobBean() {

    fun saveExchangeRates(
        applicationContext: ApplicationContext,
        currencySymbolCode: String,
        currencyExponent: Int,
        exchangeRates: ExchangeRates,
        sourceId: String
    ) {
        try {
            val exchangeDaoService = applicationContext.getBean(ExchangeDaoService::class.java)
            log.info { "Save exchange rates for currency=$currencySymbolCode, sourceId=$sourceId" }
            val exchangeRatesData = ExchangeRatesData(
                destinationCurrencySymbolicCode = currencySymbolCode,
                destinationCurrencyExponent = currencyExponent.toShort(),
                exchangeRates = exchangeRates,
                sourceId = sourceId
            )
            exchangeDaoService.saveExchangeRates(exchangeRatesData)
        } catch (e: Exception) {
            log.error("Couldn't save exchange rates for currency={} ", currencySymbolCode, e)
        }
    }

    fun sendExchangeRates(
        applicationContext: ApplicationContext,
        currencySymbolCode: String,
        currencyExponent: Int,
        exchangeRates: ExchangeRates
    ) {
        val exchangeEventService = applicationContext.getBean(ExchangeEventService::class.java)
        log.info { "Send exchange rates for currency=$currencySymbolCode" }
        exchangeEventService.sendExchangeRates(currencySymbolCode, currencyExponent.toShort(), exchangeRates)
    }
}
