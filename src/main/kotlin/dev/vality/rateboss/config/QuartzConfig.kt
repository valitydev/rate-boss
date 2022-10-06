package dev.vality.rateboss.config

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.quartz.QuartzProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.quartz.SchedulerFactoryBean
import java.util.*
import javax.sql.DataSource

@Configuration
class QuartzConfig {

    @Autowired
    private lateinit var quartzProperties: QuartzProperties

    fun getAllProperties(): Properties {
        val props = Properties()
        props.putAll(quartzProperties.properties)
        return props
    }

    @Bean
    fun schedulerFactoryBean(dataSource: DataSource): SchedulerFactoryBean {
        val schedulerFactoryBean = SchedulerFactoryBean()
        schedulerFactoryBean.setQuartzProperties(getAllProperties())
        schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true)
        schedulerFactoryBean.setDataSource(dataSource)
        schedulerFactoryBean.setApplicationContextSchedulerContextKey("applicationContext")

        return schedulerFactoryBean
    }
}
