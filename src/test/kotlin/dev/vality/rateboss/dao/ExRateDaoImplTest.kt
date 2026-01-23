package dev.vality.rateboss.dao

import dev.vality.exrates.service.CurrencyData
import dev.vality.exrates.service.GetCurrencyExchangeRateRequest
import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.converter.Constants.Companion.DATE_TIME_FORMAT
import dev.vality.rateboss.dao.domain.Tables.EX_RATE
import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import dev.vality.rateboss.service.model.TimestampExchangeRateRequest
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Testcontainers
class ExRateDaoImplTest : ContainerConfiguration() {
    @MockitoBean
    lateinit var scheduler: Scheduler

    @Autowired
    lateinit var exRateDao: ExRateDao

    @Autowired
    lateinit var dslContext: DSLContext

    @BeforeEach
    fun setUp() {
        dslContext.deleteFrom(EX_RATE).execute()
    }

    @Test
    fun saveBatchWithSameCodes() {
        val firstExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = "USD"
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = "UZS"
                destinationCurrencyExponent = 6
                rationalP = 11190000264
                rationalQ = 1000000
                rateTimestamp = LocalDateTime.now()
                source = "source"
            }
        val secondExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = "USD"
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = "UZS"
                destinationCurrencyExponent = 6
                rationalP = 1119000000
                rationalQ = 1000000
                rateTimestamp = LocalDateTime.now()
                source = "source"
            }
        val entities = listOf(firstExRate, secondExRate)

        exRateDao.saveBatch(entities)

        val records = dslContext.fetch(EX_RATE)
        assertEquals(entities.size, records.size)
        assertTrue(records.any { exRateRecord -> exRateRecord.rationalP.equals(firstExRate.rationalP) })
        assertTrue(records.any { exRateRecord -> exRateRecord.rationalP.equals(secondExRate.rationalP) })
    }

    @Test
    fun saveBatch() {
        val firstExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = "USD"
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = "UZS"
                destinationCurrencyExponent = 6
                rationalP = 11190000264
                rationalQ = 1000000
                rateTimestamp = LocalDateTime.now()
                source = "source"
            }
        val secondExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = "USD"
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = "RUB"
                destinationCurrencyExponent = 6
                rationalP = 1119000000
                rationalQ = 1000000
                rateTimestamp = LocalDateTime.now()
                source = "source"
            }
        val entities = listOf(firstExRate, secondExRate)

        exRateDao.saveBatch(entities)

        val records = dslContext.fetch(EX_RATE)
        assertEquals(entities.size, records.size)
    }

    @Test
    fun getRecentBySymbolicCodes() {
        val sourceCurrency = "USD"
        val destinationCurrency = "UZS"
        val oldExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 6
                rationalP = 11190000264
                rationalQ = 1000000
                rateTimestamp = LocalDateTime.now().minusHours(1)
                source = "source"
            }
        val recentExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 6
                rationalP = 1119000000
                rationalQ = 1000000
                rateTimestamp = LocalDateTime.now()
                source = "source"
            }
        dslContext
            .insertInto(EX_RATE)
            .set(dslContext.newRecord(EX_RATE, oldExRate))
            .newRecord()
            .set(dslContext.newRecord(EX_RATE, recentExRate))
            .execute()

        val request =
            GetCurrencyExchangeRateRequest(
                CurrencyData(sourceCurrency, destinationCurrency)
            ).setDatetime(
                recentExRate.rateTimestamp
                    .toLocalDate()
                    .atStartOfDay()
                    .format(DateTimeFormatter.ofPattern(DATE_TIME_FORMAT))
            )
        val result = exRateDao.getRecentBySymbolicCodes(request)

        assertEquals(recentExRate.rationalP, result?.rationalP)
        assertEquals(recentExRate.rationalQ, result?.rationalQ)
    }

    @Test
    fun getExRateByTimestamp() {
        val destinationCurrency = "USD"
        val sourceCurrency = "UZS"
        val sourceId = "source"
        val firstExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 2
                rationalP = 11190000264
                rationalQ = 1000055
                rateTimestamp = LocalDateTime.now().minusDays(3)
                source = sourceId
            }
        val secondExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 2
                rationalP = 1119000546
                rationalQ = 1000066
                rateTimestamp = LocalDateTime.now().minusDays(2)
                source = sourceId
            }
        val thirdExRate =
            ExRate().apply {
                sourceCurrencySymbolicCode = sourceCurrency
                sourceCurrencyExponent = 2
                destinationCurrencySymbolicCode = destinationCurrency
                destinationCurrencyExponent = 2
                rationalP = 1119000229
                rationalQ = 1000077
                rateTimestamp = LocalDateTime.now().minusDays(1)
                source = sourceId
            }
        dslContext
            .insertInto(EX_RATE)
            .set(dslContext.newRecord(EX_RATE, firstExRate))
            .newRecord()
            .set(dslContext.newRecord(EX_RATE, secondExRate))
            .newRecord()
            .set(dslContext.newRecord(EX_RATE, thirdExRate))
            .execute()

        val requestTimestampBeforeFirstExRate =
            TimestampExchangeRateRequest(
                sourceCurrency = sourceCurrency,
                destinationCurrency = destinationCurrency,
                source = sourceId,
                rateTimestamp = firstExRate.rateTimestamp.minusDays(1),
            )
        val emptyResult = exRateDao.getByCodesAndTimestamp(requestTimestampBeforeFirstExRate)

        assertNull(emptyResult)

        val requestTimestampBetweenSecondAndThirdExRate =
            TimestampExchangeRateRequest(
                sourceCurrency = sourceCurrency,
                destinationCurrency = destinationCurrency,
                source = sourceId,
                rateTimestamp = secondExRate.rateTimestamp.plusMinutes(10),
            )
        val secondResult = exRateDao.getByCodesAndTimestamp(requestTimestampBetweenSecondAndThirdExRate)!!

        assertEquals(secondExRate.rationalP, secondResult.rationalP)
        assertEquals(secondExRate.rationalQ, secondResult.rationalQ)

        val requestTimestampBetweenAfterThirdExRate =
            TimestampExchangeRateRequest(
                sourceCurrency = sourceCurrency,
                destinationCurrency = destinationCurrency,
                source = sourceId,
                rateTimestamp = thirdExRate.rateTimestamp.plusMinutes(10),
            )
        val thirdResult = exRateDao.getByCodesAndTimestamp(requestTimestampBetweenAfterThirdExRate)!!

        assertEquals(thirdExRate.rationalP, thirdResult.rationalP)
        assertEquals(thirdExRate.rationalQ, thirdResult.rationalQ)
    }
}
