package dev.vality.rateboss.service

import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.dao.domain.Tables
import dev.vality.rateboss.dao.domain.tables.ExRate.EX_RATE
import dev.vality.rateboss.dao.domain.tables.records.ExRateRecord
import dev.vality.rateboss.source.model.ExchangeRates
import dev.vality.rateboss.source.model.ExchangeRatesData
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.math.BigDecimal
import java.time.Instant

class ExchangeDaoServiceTest : ContainerConfiguration() {
    @MockitoBean
    lateinit var scheduler: Scheduler

    @Autowired
    lateinit var exchangeDaoService: ExchangeDaoService

    @Autowired
    lateinit var dslContext: DSLContext

    @BeforeEach
    fun setUp() {
        dslContext.deleteFrom(Tables.EX_RATE).execute()
    }

    @Test
    fun `save exchange rate`() {
        // Given
        val baseCurrencySymbolCode = "USD"
        val baseCurrencyExponent = 2
        val exchangeRates =
            ExchangeRates(
                rates =
                    mapOf(
                        "RUB" to BigDecimal.valueOf(56.761762),
                        "AED" to BigDecimal.valueOf(3.593066),
                        "AMD" to BigDecimal.valueOf(397.376632),
                    ),
                timestamp = Instant.now().epochSecond,
            )

        val exchangeRatesData =
            ExchangeRatesData(
                destinationCurrencySymbolicCode = baseCurrencySymbolCode,
                destinationCurrencyExponent = baseCurrencyExponent.toShort(),
                exchangeRates = exchangeRates,
                sourceId = "source",
            )

        // When
        exchangeDaoService.saveExchangeRates(exchangeRatesData)

        // Then
        val records = dslContext.fetch(EX_RATE)

        assertEquals(exchangeRates.rates.size, records.size)
        val codes =
            records
                .stream()
                .map(ExRateRecord::getDestinationCurrencySymbolicCode)
                .toList()
        assertThat(codes, contains("RUB", "AED", "AMD"))
    }
}
