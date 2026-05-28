package dev.vality.rateboss.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.module.kotlin.kotlinModule
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
    fun nbkrXmlMapper(): XmlMapper =
        XmlMapper
            .builder()
            .addModule(kotlinModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .build()

    @Bean
    fun objectMapper(): ObjectMapper = ObjectMapper().registerModule(kotlinModule())

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
            JobDescription(
                "nbkz-cron",
                "nbkz-key",
                "nbkz-name",
                listOf(CurrencyProperties("KZT", 2)),
            ),
            JobDescription(
                "nbkr-cron",
                "nbkr-key",
                "nbkr-name",
                listOf(CurrencyProperties("KGS", 2)),
            ),
            JobDescription(
                "nbuz-cron",
                "nbuz-key",
                "nbuz-name",
                listOf(CurrencyProperties("UZS", 2)),
            ),
            JobDescription(
                "nbaz-cron",
                "nbaz-key",
                "nbaz-name",
                listOf(CurrencyProperties("AZN", 2)),
            ),
            RatesSourceProperties(
                FixerProperties("url", "key"),
                CbrProperties("https://www.cbr.ru/scripts/XML_daily.asp", ZoneId.of("Europe/Moscow")),
                NbkzProperties("https://nationalbank.kz/rss/get_rates.cfm", "dd.MM.yyyy", ZoneId.of("Asia/Almaty")),
                NbkrProperties("https://www.nbkr.kg/XML/daily.xml", ZoneId.of("Asia/Bishkek")),
                NbuzProperties(
                    "https://nbu.uz/api/collections/individuals_exchange_rates_bankomats/entries",
                    "ru",
                    ZoneId.of("Asia/Tashkent"),
                ),
                NbazProperties(
                    "https://www.cbar.az/currencies/",
                    "dd.MM.yyyy",
                    ZoneId.of("Asia/Baku"),
                ),
            ),
        )
}
