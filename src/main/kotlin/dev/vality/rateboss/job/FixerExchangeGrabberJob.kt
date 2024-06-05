package dev.vality.rateboss.job

import dev.vality.rateboss.extensions.getApplicationContext
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.impl.FixerExchangeRateSource
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.context.ApplicationContext
import org.springframework.retry.support.RetryTemplate

private val log = KotlinLogging.logger {}

class FixerExchangeGrabberJob : AbstractExchangeGrabberJob() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val currencySymbolCode = context.jobDetail.jobDataMap["currencySymbolCode"] as String
        val currencyExponent = context.jobDetail.jobDataMap["currencyExponent"] as Int
        val exchangeRateSource = applicationContext.getBean(FixerExchangeRateSource::class.java)
        val sourceId = exchangeRateSource.getSourceId()
        log.info { "Process FixerExchangeGrabberJob for $sourceId" }
        val exchangeRates = getExchangeRates(applicationContext, exchangeRateSource, currencySymbolCode)
        sendExchangeRates(applicationContext, currencySymbolCode, currencyExponent, exchangeRates)
        saveExchangeRates(applicationContext, currencySymbolCode, currencyExponent, exchangeRates, sourceId)
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
}
