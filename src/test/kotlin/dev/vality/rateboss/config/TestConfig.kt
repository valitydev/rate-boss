package dev.vality.rateboss.config

import dev.vality.rateboss.config.properties.*
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.web.client.RestTemplate
import java.time.ZoneId

@TestConfiguration
class TestConfig {
    @Bean
    fun testRestTemplate() = RestTemplate()

    @Bean
    fun testRatesProperties(): RatesProperties =
        RatesProperties(
            JobDescription(
                "fixer-cron",
                "fixer-key",
                "fixer-name",
                listOf(CurrencyProperties("USD", 2)),
            ),
            JobDescription(
                "cbr-cron",
                "cbr-key",
                "cbr-name",
                listOf(CurrencyProperties("RUB", 2)),
            ),
            RatesSourceProperties(
                FixerProperties("url", "key"),
                CbrProperties("https://www.cbr.ru/scripts/XML_daily.asp", ZoneId.of("Europe/Moscow")),
            ),
        )
}
