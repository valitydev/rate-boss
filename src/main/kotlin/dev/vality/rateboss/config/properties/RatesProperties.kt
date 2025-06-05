package dev.vality.rateboss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.ZoneId

@Validated
@ConfigurationProperties(prefix = "rates")
data class RatesProperties(
    val fixerJob: JobDescription,
    val cbrJob: JobDescription,
    val source: RatesSourceProperties,
)

data class JobDescription(
    val jobCron: String,
    val jobKey: String,
    val jobTriggerName: String,
    val currencies: List<CurrencyProperties>,
)

data class CurrencyProperties(
    val symbolCode: String,
    val exponent: Int,
)

data class RatesSourceProperties(
    val fixer: FixerProperties,
    val cbr: CbrProperties,
)

data class FixerProperties(
    val rootUrl: String,
    val apiKey: String,
)

data class CbrProperties(
    val rootUrl: String,
    val timeZone: ZoneId,
)
