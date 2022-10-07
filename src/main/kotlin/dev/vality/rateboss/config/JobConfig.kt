package dev.vality.rateboss.config

import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.job.ExchangeGrabberMasterJob
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
        schedulerFactoryBean.addJob(exchangeRateGrabberMasterJob(), true, true)
        if (!schedulerFactoryBean.checkExists(TriggerKey(ratesProperties.jobTriggerName))) {
            schedulerFactoryBean.scheduleJob(exchangeRateGrabberMasterTrigger())
        }
    }

    fun exchangeRateGrabberMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey(ratesProperties.jobKey)
        jobDetail.jobClass = ExchangeGrabberMasterJob::class.java

        return jobDetail
    }

    fun exchangeRateGrabberMasterTrigger(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(exchangeRateGrabberMasterJob())
            .withIdentity(ratesProperties.jobTriggerName)
            .withSchedule(CronScheduleBuilder.cronSchedule(ratesProperties.jobCron))
            .build()
    }
}
