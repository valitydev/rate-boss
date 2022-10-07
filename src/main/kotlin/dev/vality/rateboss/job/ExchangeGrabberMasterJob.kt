package dev.vality.rateboss.job

import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.extensions.getApplicationContext
import mu.KotlinLogging
import org.quartz.*
import org.springframework.scheduling.quartz.QuartzJobBean
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean
import java.util.*

private val log = KotlinLogging.logger {}

class ExchangeGrabberMasterJob : QuartzJobBean() {

    override fun executeInternal(context: JobExecutionContext) {
        val applicationContext = context.getApplicationContext()
        val ratesProperties = applicationContext.getBean(RatesProperties::class.java)
        for (currency in ratesProperties.currencies) {
            val jobIdentity = "${currency.symbolCode}-currency-job"
            val jobDetail =
                JobBuilder.newJob(ExchangeGrabberJob::class.java).withIdentity(jobIdentity).build()
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
                val schedulerFactoryBean = applicationContext.getBean(Scheduler::class.java)
                schedulerFactoryBean.scheduleJob(jobDetail, triggerFactoryBean)
            } catch (e: ObjectAlreadyExistsException) {
                log.warn { "Task still in progress: $jobIdentity" }
            } catch (e: Exception) {
                log.error(e) { "Failed to start scheduler job" }
            }
        }
    }
}
