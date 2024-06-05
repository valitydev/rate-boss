package dev.vality.rateboss.job

import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.extensions.getApplicationContext
import mu.KotlinLogging
import org.quartz.JobExecutionContext
import org.quartz.Scheduler

private val log = KotlinLogging.logger {}

class FixerExchangeGrabberMasterJob : AbstractExchangeGrabberMasterJob() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val ratesProperties = applicationContext.getBean(RatesProperties::class.java)
        val currencies = ratesProperties.fixerJob.currencies
        val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
        launchJob(currencies, schedulerFactoryBean, FixerExchangeGrabberJob::class.java, getJobName())
    }

    override fun getJobName(): String {
        return "fixerJob"
    }
}
