package dev.vality.rateboss.config

import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.job.CbrExchangeGrabberMasterJob
import dev.vality.rateboss.job.FixerExchangeGrabberMasterJob
import dev.vality.rateboss.job.NbkzExchangeGrabberMasterJob
import org.quartz.*
import org.quartz.impl.JobDetailImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class JobConfig {
    @Autowired
    private lateinit var schedulerFactoryBean: Scheduler

    @Autowired
    private lateinit var ratesProperties: RatesProperties

    @PostConstruct
    fun init() {
        val fixerJobTriggerName = ratesProperties.fixerJob.jobTriggerName
        if (fixerJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(fixerExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(fixerJobTriggerName))) {
                schedulerFactoryBean.scheduleJob(fixerExchangeRateGrabberMasterTrigger())
            }
        }
        val cbrJobTriggerName = ratesProperties.cbrJob.jobTriggerName
        if (cbrJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(cbrExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(ratesProperties.cbrJob.jobTriggerName))) {
                schedulerFactoryBean.scheduleJob(cbrExchangeRateGrabberMasterTrigger())
            }
        }
        val nbkzJobTriggerName = ratesProperties.nbkzJob.jobTriggerName
        if (nbkzJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(nbkzExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(ratesProperties.nbkzJob.jobTriggerName))) {
                schedulerFactoryBean.scheduleJob(nbkzExchangeRateGrabberMasterTrigger())
            }
        }
    }

    fun fixerExchangeRateGrabberMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(ratesProperties.fixerJob.jobKey)
        jobDetail.jobClass = FixerExchangeGrabberMasterJob::class.java
        return jobDetail
    }

    fun fixerExchangeRateGrabberMasterTrigger(): CronTrigger =
        TriggerBuilder
            .newTrigger()
            .forJob(fixerExchangeRateGrabberMasterJob())
            .withIdentity(ratesProperties.fixerJob.jobTriggerName)
            .withSchedule(CronScheduleBuilder.cronSchedule(ratesProperties.fixerJob.jobCron))
            .build()

    fun cbrExchangeRateGrabberMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(ratesProperties.cbrJob.jobKey)
        jobDetail.jobClass = CbrExchangeGrabberMasterJob::class.java
        return jobDetail
    }

    fun cbrExchangeRateGrabberMasterTrigger(): CronTrigger =
        TriggerBuilder
            .newTrigger()
            .forJob(cbrExchangeRateGrabberMasterJob())
            .withIdentity(ratesProperties.cbrJob.jobTriggerName)
            .withSchedule(CronScheduleBuilder.cronSchedule(ratesProperties.cbrJob.jobCron))
            .build()

    fun nbkzExchangeRateGrabberMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(ratesProperties.nbkzJob.jobKey)
        jobDetail.jobClass = NbkzExchangeGrabberMasterJob::class.java
        return jobDetail
    }

    fun nbkzExchangeRateGrabberMasterTrigger(): CronTrigger =
        TriggerBuilder
            .newTrigger()
            .forJob(nbkzExchangeRateGrabberMasterJob())
            .withIdentity(ratesProperties.nbkzJob.jobTriggerName)
            .withSchedule(CronScheduleBuilder.cronSchedule(ratesProperties.nbkzJob.jobCron))
            .build()
}
