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
        if (!schedulerFactoryBean.checkExists(TriggerKey(RATES_GRABBER_TRIGGER_KEY))) {
            schedulerFactoryBean.scheduleJob(exchangeRateGrabberMasterTrigger())
        }
    }

    fun exchangeRateGrabberMasterJob(): JobDetailImpl {
        val jobDetail = JobDetailImpl()
        jobDetail.key = JobKey("exchange-rate-grabber-master-job")
        jobDetail.jobClass = ExchangeGrabberMasterJob::class.java

        return jobDetail
    }

    fun exchangeRateGrabberMasterTrigger(): CronTrigger {
        return TriggerBuilder.newTrigger()
            .forJob(exchangeRateGrabberMasterJob())
            .withIdentity(RATES_GRABBER_TRIGGER_KEY)
            .withSchedule(CronScheduleBuilder.cronSchedule(ratesProperties.jobCron))
            .build()
    }

    private companion object {
        const val RATES_GRABBER_TRIGGER_KEY = "rate-grabber-master-trigger"
    }
}
