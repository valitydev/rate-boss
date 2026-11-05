package dev.vality.rateboss.service

import dev.vality.exrates.service.ConversionRequest
import dev.vality.exrates.service.CurrencyData
import dev.vality.exrates.service.ExRateNotFound
import dev.vality.exrates.service.GetCurrencyExchangeRateRequest
import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.converter.Constants
import dev.vality.rateboss.dao.domain.Tables
import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import org.apache.commons.math3.fraction.BigFraction
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Testcontainers
class ExRateServiceHandlerTest : ContainerConfiguration() {
    @MockitoBean
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
        val request =
            GetCurrencyExchangeRateRequest()
                .setCurrencyData(
                    CurrencyData()
                        .setSourceCurrency(sourceCurrency)
                        .setDestinationCurrency(destinationCurrency),
                )
        assertThrows<ExRateNotFound> { exRateServiceHandler.getExchangeRateData(request) }
    }

    @Test
    fun getExchangeRateData() {
        val sourceCurrency = "USD"
        val destinationCurrency = "UZS"
        val sourceId = "sourceId"
        val exRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 6
                rationalP = 11190000264
                rationalQ = 1000000
                rateTimestamp = LocalDateTime.now()
                source = sourceId
            }
        dslContext
            .insertInto(Tables.EX_RATE)
            .set(dslContext.newRecord(Tables.EX_RATE, exRate))
            .execute()
        val request =
            GetCurrencyExchangeRateRequest()
                .setCurrencyData(
                    CurrencyData()
                        .setSourceCurrency(sourceCurrency)
                        .setDestinationCurrency(destinationCurrency),
                )

        val result = exRateServiceHandler.getExchangeRateData(request)

        assertEquals(exRate.rationalQ, result.exchange_rate.q)
        assertEquals(exRate.rationalP, result.exchange_rate.p)
    }

    @Test
    fun getExchangeRateDataFromNbkz() {
        val sourceCurrency = "KZT"
        val destinationCurrency = "USD"
        val sourceId = "nbkz"
        val exRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 2
                rationalP = 4701200
                rationalQ = 10000
                rateTimestamp = LocalDateTime.now()
                source = sourceId
            }
        dslContext
            .insertInto(Tables.EX_RATE)
            .set(dslContext.newRecord(Tables.EX_RATE, exRate))
            .execute()
        val request =
            GetCurrencyExchangeRateRequest()
                .setCurrencyData(
                    CurrencyData()
                        .setSourceCurrency(sourceCurrency)
                        .setDestinationCurrency(destinationCurrency),
                )

        val result = exRateServiceHandler.getExchangeRateData(request)

        assertEquals(exRate.rationalQ, result.exchange_rate.q)
        assertEquals(exRate.rationalP, result.exchange_rate.p)
    }

    @Test
    fun getEmptyExRateForConvertedAmount() {
        val conversionRequest =
            ConversionRequest()
                .setAmount(100L)
                .setDatetime(
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)),
                ).setDestination("USD")
                .setSource("UZS")

        assertThrows(ExRateNotFound::class.java) {
            exRateServiceHandler.getConvertedAmount("sourceId", conversionRequest)
        }
    }

    @Test
    fun getSuccessConvertedAmount() {
        val sourceCurrency = "UZS"
        val destinationCurrency = "USD"
        val sourceId = "sourceId"
        val exRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 2
                rationalP = 1398750033
                rationalQ = 125000
                rateTimestamp = LocalDateTime.now().minusDays(1)
                source = sourceId
            }
        dslContext
            .insertInto(Tables.EX_RATE)
            .set(dslContext.newRecord(Tables.EX_RATE, exRate))
            .execute()
        val conversionRequest =
            ConversionRequest()
                .setAmount(100L)
                .setDatetime(
                    exRate.rateTimestamp.plusMinutes(10).format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)),
                ).setDestination(destinationCurrency)
                .setSource(sourceCurrency)

        val result = exRateServiceHandler.getConvertedAmount(sourceId, conversionRequest)

        assertNotNull(result)
        assertEquals(exRate.rationalP, result.p)
        assertEquals(exRate.rationalQ / conversionRequest.amount, result.q)
    }

    @Test
    fun getSuccessConvertedAmountFromNbkz() {
        val sourceCurrency = "KZT"
        val destinationCurrency = "USD"
        val sourceId = "nbkz"
        val exRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 2
                rationalP = 4701200
                rationalQ = 10000
                rateTimestamp = LocalDateTime.now().minusDays(1)
                source = sourceId
            }
        dslContext
            .insertInto(Tables.EX_RATE)
            .set(dslContext.newRecord(Tables.EX_RATE, exRate))
            .execute()
        val conversionRequest =
            ConversionRequest()
                .setAmount(100L)
                .setDatetime(
                    exRate.rateTimestamp.plusMinutes(10).format(DateTimeFormatter.ofPattern(Constants.DATE_TIME_FORMAT)),
                ).setDestination(destinationCurrency)
                .setSource(sourceCurrency)

        val result = exRateServiceHandler.getConvertedAmount(sourceId, conversionRequest)

        val expected =
            BigFraction(conversionRequest.amount)
                .multiply(BigFraction(exRate.rationalP, exRate.rationalQ))

        assertNotNull(result)
        assertEquals(expected.numeratorAsLong, result.p)
        assertEquals(expected.denominatorAsLong, result.q)
    }

    @ParameterizedTest
    @CsvSource("9223372036854775807, 1, 2", "9223372036854775807, 1, -2", "2352941176470588, 10000000000000000, 20001")
    fun `fail explicitly when the converted result exceeds the wire format`(
        numerator: Long,
        denominator: Long,
        amount: Long,
    ) {
        val exRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = "PLN"
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = "EUR"
                destinationCurrencyExponent = 2
                rationalP = numerator
                rationalQ = denominator
                rateTimestamp = LocalDateTime.of(2026, 10, 31, 0, 0)
                source = "nbp"
            }
        dslContext.insertInto(Tables.EX_RATE).set(dslContext.newRecord(Tables.EX_RATE, exRate)).execute()
        val request =
            ConversionRequest()
                .setSource("PLN")
                .setDestination("EUR")
                .setAmount(amount)
                .setDatetime("2026-10-31T12:00:00Z")

        val exception = assertThrows<ArithmeticException> { exRateServiceHandler.getConvertedAmount("nbp", request) }

        assertTrue(exception.message!!.contains("64-bit"))
    }
}
