package dev.vality.rateboss.config

import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.job.CbrExchangeGrabberMasterJob
import dev.vality.rateboss.job.FixerExchangeGrabberMasterJob
import dev.vality.rateboss.job.NbazExchangeGrabberMasterJob
import dev.vality.rateboss.job.NbkrExchangeGrabberMasterJob
import dev.vality.rateboss.job.NbkzExchangeGrabberMasterJob
import dev.vality.rateboss.job.NbuzExchangeGrabberMasterJob
import org.quartz.*
import org.quartz.impl.JobDetailImpl
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import javax.annotation.PostConstruct

@Configuration
class JobConfig {
    @Autowired
    private lateinit var schedulerFactoryBean: Scheduler

    @Autowired
    private lateinit var ratesProperties: RatesProperties

    @Value("\${rates.run-on-startup:true}")
    private var runOnStartup: Boolean = true

    @PostConstruct
    fun init() {
        val fixerJobTriggerName = ratesProperties.fixerJob.jobTriggerName
        if (fixerJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(fixerExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(fixerJobTriggerName))) {
                schedulerFactoryBean.scheduleJob(fixerExchangeRateGrabberMasterTrigger())
            }
            if (runOnStartup) {
                schedulerFactoryBean.triggerJob(JobKey(ratesProperties.fixerJob.jobKey))
            }
        }
        val cbrJobTriggerName = ratesProperties.cbrJob.jobTriggerName
        if (cbrJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(cbrExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(ratesProperties.cbrJob.jobTriggerName))) {
                schedulerFactoryBean.scheduleJob(cbrExchangeRateGrabberMasterTrigger())
            }
            if (runOnStartup) {
                schedulerFactoryBean.triggerJob(JobKey(ratesProperties.cbrJob.jobKey))
            }
        }
        val nbkzJobTriggerName = ratesProperties.nbkzJob.jobTriggerName
        if (nbkzJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(nbkzExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(ratesProperties.nbkzJob.jobTriggerName))) {
                schedulerFactoryBean.scheduleJob(nbkzExchangeRateGrabberMasterTrigger())
            }
            if (runOnStartup) {
                schedulerFactoryBean.triggerJob(JobKey(ratesProperties.nbkzJob.jobKey))
            }
        }
        val nbkrJobTriggerName = ratesProperties.nbkrJob.jobTriggerName
        if (nbkrJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(nbkrExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(ratesProperties.nbkrJob.jobTriggerName))) {
                schedulerFactoryBean.scheduleJob(nbkrExchangeRateGrabberMasterTrigger())
            }
            if (runOnStartup) {
                schedulerFactoryBean.triggerJob(JobKey(ratesProperties.nbkrJob.jobKey))
            }
        }
        val nbuzJobTriggerName = ratesProperties.nbuzJob.jobTriggerName
        if (nbuzJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(nbuzExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(ratesProperties.nbuzJob.jobTriggerName))) {
                schedulerFactoryBean.scheduleJob(nbuzExchangeRateGrabberMasterTrigger())
            }
            if (runOnStartup) {
                schedulerFactoryBean.triggerJob(JobKey(ratesProperties.nbuzJob.jobKey))
            }
        }
        val nbazJobTriggerName = ratesProperties.nbazJob.jobTriggerName
        if (nbazJobTriggerName.isNotEmpty()) {
            schedulerFactoryBean.addJob(nbazExchangeRateGrabberMasterJob(), true, true)
            if (!schedulerFactoryBean.checkExists(TriggerKey(ratesProperties.nbazJob.jobTriggerName))) {
                schedulerFactoryBean.scheduleJob(nbazExchangeRateGrabberMasterTrigger())
            }
            if (runOnStartup) {
                schedulerFactoryBean.triggerJob(JobKey(ratesProperties.nbazJob.jobKey))
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

    fun nbkrExchangeRateGrabberMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(ratesProperties.nbkrJob.jobKey)
        jobDetail.jobClass = NbkrExchangeGrabberMasterJob::class.java
        return jobDetail
    }

    fun nbkrExchangeRateGrabberMasterTrigger(): CronTrigger =
        TriggerBuilder
            .newTrigger()
            .forJob(nbkrExchangeRateGrabberMasterJob())
            .withIdentity(ratesProperties.nbkrJob.jobTriggerName)
            .withSchedule(CronScheduleBuilder.cronSchedule(ratesProperties.nbkrJob.jobCron))
            .build()

    fun nbuzExchangeRateGrabberMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(ratesProperties.nbuzJob.jobKey)
        jobDetail.jobClass = NbuzExchangeGrabberMasterJob::class.java
        return jobDetail
    }

    fun nbuzExchangeRateGrabberMasterTrigger(): CronTrigger =
        TriggerBuilder
            .newTrigger()
            .forJob(nbuzExchangeRateGrabberMasterJob())
            .withIdentity(ratesProperties.nbuzJob.jobTriggerName)
            .withSchedule(CronScheduleBuilder.cronSchedule(ratesProperties.nbuzJob.jobCron))
            .build()

    fun nbazExchangeRateGrabberMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(ratesProperties.nbazJob.jobKey)
        jobDetail.jobClass = NbazExchangeGrabberMasterJob::class.java
        return jobDetail
    }

    fun nbazExchangeRateGrabberMasterTrigger(): CronTrigger =
        TriggerBuilder
            .newTrigger()
            .forJob(nbazExchangeRateGrabberMasterJob())
            .withIdentity(ratesProperties.nbazJob.jobTriggerName)
            .withSchedule(CronScheduleBuilder.cronSchedule(ratesProperties.nbazJob.jobCron))
            .build()
}
