package dev.vality.rateboss.job

import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.extensions.getApplicationContext
import org.quartz.JobExecutionContext
import org.quartz.Scheduler

class NbazExchangeGrabberMasterJob : AbstractExchangeGrabberMasterJob() {
    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val ratesProperties = applicationContext.getBean(RatesProperties::class.java)
        val currencies = ratesProperties.nbazJob.currencies
        val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
        launchJob(currencies, schedulerFactoryBean, NbazExchangeGrabberJob::class.java, getJobName())
    }

    override fun getJobName(): String = "nbazJob"
}
