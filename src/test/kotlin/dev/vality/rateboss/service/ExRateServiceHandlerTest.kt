package dev.vality.rateboss.service

import dev.vality.exrates.service.CurrencyData
import dev.vality.exrates.service.ExRateNotFound
import dev.vality.exrates.service.GetCurrencyExchangeRateRequest
import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.dao.domain.Tables
import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@Testcontainers
class ExRateServiceHandlerTest : ContainerConfiguration() {

    @MockBean
    lateinit var scheduler: Scheduler

    @Autowired
    lateinit var exRateServiceHandler: ExRateServiceHandler

    @Autowired
    lateinit var dslContext: DSLContext

    @BeforeEach
    fun setUp() {
        dslContext.deleteFrom(Tables.EX_RATE).execute()
    }

    @Test
    fun getExchangeRateWithoutData() {
        val sourceCurrency = "USD"
        val destinationCurrency = "UZS"
        val request = GetCurrencyExchangeRateRequest()
            .setCurrencyData(
                CurrencyData()
                    .setSourceCurrency(sourceCurrency)
                    .setDestinationCurrency(destinationCurrency)
            )
        assertThrows<ExRateNotFound> { exRateServiceHandler.getExchangeRateData(request) }
    }

    @Test
    fun getExchangeRateData() {
        val sourceCurrency = "USD"
        val destinationCurrency = "UZS"
        val exRate = ExRate().apply {
            sourceCurrencySymbolicCode = sourceCurrency
            sourceCurrencyExponent = 2
            destinationCurrencySymbolicCode = destinationCurrency
            destinationCurrencyExponent = 6
            rationalP = 11190000264
            rationalQ = 1000000
            rateTimestamp = LocalDateTime.now()
        }
        dslContext.insertInto(Tables.EX_RATE)
            .set(dslContext.newRecord(Tables.EX_RATE, exRate))
            .execute()
        val request = GetCurrencyExchangeRateRequest()
            .setCurrencyData(
                CurrencyData()
                    .setSourceCurrency(sourceCurrency)
                    .setDestinationCurrency(destinationCurrency)
            )

        val result = exRateServiceHandler.getExchangeRateData(request)

        assertEquals(exRate.rationalQ, result.exchange_rate.q)
        assertEquals(exRate.rationalP, result.exchange_rate.p)
    }
}
