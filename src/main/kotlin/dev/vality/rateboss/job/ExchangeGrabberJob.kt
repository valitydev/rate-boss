package dev.vality.rateboss.job

import dev.vality.rateboss.extensions.getApplicationContext
import dev.vality.rateboss.service.ExchangeDaoService
import dev.vality.rateboss.service.ExchangeEventService
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.factory.ExchangeRateSourceFactory
import dev.vality.rateboss.source.model.ExchangeRates
import dev.vality.rateboss.source.model.ExchangeRatesData
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.context.ApplicationContext
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.quartz.QuartzJobBean

private val log = KotlinLogging.logger {}

class ExchangeGrabberJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val currencySymbolCode = context.jobDetail.jobDataMap["currencySymbolCode"] as String
        val currencyExponent = context.jobDetail.jobDataMap["currencyExponent"] as Int
        val exchangeRateSourceFactory = applicationContext.getBean(ExchangeRateSourceFactory::class.java)
        val exchangeRateSources = exchangeRateSourceFactory.getSources(applicationContext, currencySymbolCode)
        for (exchangeRateSource in exchangeRateSources) {
            val sourceId = exchangeRateSource.getSourceId()
            log.info { "Process ExchangeGrabberJob for $sourceId" }
            val exchangeRates = getExchangeRates(applicationContext, exchangeRateSource, currencySymbolCode)
            sendExchangeRates(applicationContext, currencySymbolCode, currencyExponent, exchangeRates)
            saveExchangeRates(applicationContext, currencySymbolCode, currencyExponent, exchangeRates, sourceId)
        }
    }

    private fun getExchangeRates(
        applicationContext: ApplicationContext,
        exchangeRateSource: ExchangeRateSource,
        currencySymbolCode: String
    ): ExchangeRates {
        val retryTemplate = applicationContext.getBean(RetryTemplate::class.java)
        val exchangeRates = retryTemplate.execute<ExchangeRates, ExchangeRateSourceException> {
            exchangeRateSource.getExchangeRate(currencySymbolCode)
        }
        return exchangeRates
    }

    private fun sendExchangeRates(
        applicationContext: ApplicationContext,
        currencySymbolCode: String,
        currencyExponent: Int,
        exchangeRates: ExchangeRates
    ) {
        val exchangeEventService = applicationContext.getBean(ExchangeEventService::class.java)
        log.info { "Send exchange rates for currency=$currencySymbolCode" }
        exchangeEventService.sendExchangeRates(currencySymbolCode, currencyExponent.toShort(), exchangeRates)
    }

    private fun saveExchangeRates(
        applicationContext: ApplicationContext,
        currencySymbolCode: String,
        currencyExponent: Int,
        exchangeRates: ExchangeRates,
        sourceId: String
    ) {
        try {
            val exchangeDaoService = applicationContext.getBean(ExchangeDaoService::class.java)
            log.info { "Save exchange rates for currency=$currencySymbolCode" }
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
}
