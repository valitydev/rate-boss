package dev.vality.rateboss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import org.springframework.validation.annotation.Validated

@Validated
@ConstructorBinding
@ConfigurationProperties(prefix = "rates")
data class RatesProperties(
    val jobCron: String,
    val currencies: List<CurrencyProperties>,
    val source: RatesSourceProperties
)

data class CurrencyProperties(
    val symbolCode: String,
    val exponent: Int
)

data class RatesSourceProperties(
    val fixer: FixerProperties
)

data class FixerProperties(
    val rootUrl: String,
    val apiKey: String
)
