package dev.vality.rateboss.job

import dev.vality.rateboss.extensions.getApplicationContext
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.impl.CbrExchangeRateSource
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.context.ApplicationContext
import org.springframework.retry.support.RetryTemplate

private val log = KotlinLogging.logger {}

class CbrExchangeGrabberJob : AbstractExchangeGrabberJob() {
    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val currencySymbolCode = context.jobDetail.jobDataMap["currencySymbolCode"] as String
        val currencyExponent = context.jobDetail.jobDataMap["currencyExponent"] as Int
        val exchangeRateSource = applicationContext.getBean(CbrExchangeRateSource::class.java)
        val sourceId = exchangeRateSource.getSourceId()
        log.info { "Process CbrExchangeGrabberJob for $sourceId" }
        val exchangeRates = getExchangeRates(applicationContext, exchangeRateSource, currencySymbolCode)
        saveExchangeRates(applicationContext, currencySymbolCode, currencyExponent, exchangeRates, sourceId)
    }

    private fun getExchangeRates(
        applicationContext: ApplicationContext,
        exchangeRateSource: ExchangeRateSource,
        currencySymbolCode: String,
    ): ExchangeRates {
        val retryTemplate = applicationContext.getBean(RetryTemplate::class.java)
        val exchangeRates =
            retryTemplate.execute<ExchangeRates, ExchangeRateSourceException> {
                exchangeRateSource.getExchangeRate(currencySymbolCode)
            }
        return exchangeRates
    }
}
