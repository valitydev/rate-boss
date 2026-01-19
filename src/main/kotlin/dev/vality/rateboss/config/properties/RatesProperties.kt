package dev.vality.rateboss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.ZoneId

@Validated
@ConfigurationProperties(prefix = "rates")
data class RatesProperties(
    val fixerJob: JobDescription,
    val cbrJob: JobDescription,
    val nbkzJob: JobDescription,
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
    val nbkz: NbkzProperties,
)

data class FixerProperties(
    val rootUrl: String,
    val apiKey: String,
)

data class CbrProperties(
    val rootUrl: String,
    val timeZone: ZoneId,
)

data class NbkzProperties(
    val rootUrl: String,
    val dateFormat: String,
    val timeZone: ZoneId,
)
