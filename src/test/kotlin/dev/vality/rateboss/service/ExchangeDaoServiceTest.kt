package dev.vality.rateboss.service

import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.dao.domain.Tables
import dev.vality.rateboss.dao.domain.tables.ExRate.EX_RATE
import dev.vality.rateboss.dao.domain.tables.records.ExRateRecord
import dev.vality.rateboss.source.model.ExchangeRates
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.contains
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.Instant

class ExchangeDaoServiceTest : ContainerConfiguration() {

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
        val exchangeRates = ExchangeRates(
            rates = mapOf(
                "RUB" to BigDecimal.valueOf(56.761762),
                "AED" to BigDecimal.valueOf(3.593066),
                "AMD" to BigDecimal.valueOf(397.376632)
            ),
            timestamp = Instant.now().epochSecond
        )

        // When
        exchangeDaoService.saveExchangeRates(baseCurrencySymbolCode, baseCurrencyExponent.toShort(), exchangeRates)

        // Then
        val records = dslContext.fetch(EX_RATE)

        assertEquals(exchangeRates.rates.size, records.size)
        val codes = records.stream()
            .map(ExRateRecord::getDestinationCurrencySymbolicCode)
            .toList()
        assertThat(codes, contains("RUB", "AED", "AMD"))
    }
}
