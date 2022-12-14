package dev.vality.rateboss.job

import dev.vality.rateboss.extensions.getApplicationContext
import dev.vality.rateboss.service.ExchangeService
import dev.vality.rateboss.source.ExchangeRateSource
import dev.vality.rateboss.source.ExchangeRateSourceException
import dev.vality.rateboss.source.model.ExchangeRates
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.springframework.retry.support.RetryTemplate
import org.springframework.scheduling.quartz.QuartzJobBean

private val log = KotlinLogging.logger {}

class ExchangeGrabberJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val retryTemplate = applicationContext.getBean(RetryTemplate::class.java)
        val exchangeRateSource = applicationContext.getBean(ExchangeRateSource::class.java)
        val exchangeService = applicationContext.getBean(ExchangeService::class.java)
        val currencySymbolCode = context.jobDetail.jobDataMap["currencySymbolCode"] as String
        val currencyExponent = context.jobDetail.jobDataMap["currencyExponent"] as Int
        val exchangeRates = retryTemplate.execute<ExchangeRates, ExchangeRateSourceException> {
            exchangeRateSource.getExchangeRate(currencySymbolCode)
        }
        log.info { "Send exchange rates for currency=$currencySymbolCode" }
        exchangeService.sendExchangeRates(currencySymbolCode, currencyExponent.toShort(), exchangeRates)
    }
}
