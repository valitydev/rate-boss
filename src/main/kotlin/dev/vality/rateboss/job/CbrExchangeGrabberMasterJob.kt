package dev.vality.rateboss.job

import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.extensions.getApplicationContext
import org.quartz.JobExecutionContext
import org.quartz.Scheduler

class CbrExchangeGrabberMasterJob : AbstractExchangeGrabberMasterJob() {
    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val ratesProperties = applicationContext.getBean(RatesProperties::class.java)
        val currencies = ratesProperties.cbrJob.currencies
        val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
        launchJob(currencies, schedulerFactoryBean, CbrExchangeGrabberJob::class.java, getJobName())
    }

    override fun getJobName(): String = "cbrJob"
}
