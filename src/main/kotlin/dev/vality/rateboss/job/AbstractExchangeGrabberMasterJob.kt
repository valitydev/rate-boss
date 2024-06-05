package dev.vality.rateboss.job

import dev.vality.rateboss.config.properties.CurrencyProperties
import mu.KotlinLogging
import org.quartz.*
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*

private val log = KotlinLogging.logger {}

abstract class AbstractExchangeGrabberMasterJob : QuartzJobBean() {

    fun launchJob(
        currencies: List<CurrencyProperties>,
        schedulerFactoryBean: Scheduler,
        jobType: Class<out Job>,
        jobName: String
    ) {
        for (currency in currencies) {
            val jobIdentity = "$jobName-${currency.symbolCode}-currency-job"
            val jobDetail = JobBuilder.newJob(jobType)
                .withIdentity(jobIdentity)
                .build()
            val triggerFactoryBean = SimpleTriggerFactoryBean().apply {
                setPriority(Int.MAX_VALUE)
                setName(jobIdentity)
                setStartTime(Date())
                setRepeatInterval(0L)
                setRepeatCount(0)
                setMisfireInstruction(SimpleTrigger.MISFIRE_INSTRUCTION_FIRE_NOW)
                afterPropertiesSet()
            }.getObject()
            jobDetail.jobDataMap["currencySymbolCode"] = currency.symbolCode
            jobDetail.jobDataMap["currencyExponent"] = currency.exponent
            try {
                schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
            } catch (e: ObjectAlreadyExistsException) {
                log.warn { "Task $jobName still in progress: $jobIdentity" }
            } catch (e: Exception) {
                log.error(e) { "Failed to start scheduler job: $jobName" }
            }
        }
    }

    abstract fun getJobName(): String
}
