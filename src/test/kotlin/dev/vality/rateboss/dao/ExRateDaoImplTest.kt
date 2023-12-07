package dev.vality.rateboss.dao

import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.dao.domain.Tables.EX_RATE
import dev.vality.rateboss.dao.domain.tables.pojos.ExRate
import org.jooq.DSLContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.quartz.Scheduler
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.LocalDateTime

@Testcontainers
class ExRateDaoImplTest : ContainerConfiguration() {

    @MockBean
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
        val firstExRate = ExRate()
        firstExRate.sourceCurrencySymbolicCode = "USD"
        firstExRate.sourceCurrencyExponent = 2
        firstExRate.destinationCurrencySymbolicCode = "UZS"
        firstExRate.destinationCurrencyExponent = 6
        firstExRate.rationalP = 11190000264
        firstExRate.rationalQ = 1000000
        firstExRate.rateTimestamp = LocalDateTime.now()

        val secondExRate = ExRate()
        secondExRate.sourceCurrencySymbolicCode = "USD"
        secondExRate.sourceCurrencyExponent = 2
        secondExRate.destinationCurrencySymbolicCode = "UZS"
        secondExRate.destinationCurrencyExponent = 6
        secondExRate.rationalP = 1119000000
        secondExRate.rationalQ = 1000000
        secondExRate.rateTimestamp = LocalDateTime.now()

        val entities = listOf(firstExRate, secondExRate)
        exRateDao.saveBatch(entities)

        val records = dslContext.fetch(EX_RATE)
        assertEquals(1, records.size)
        assertEquals(secondExRate.rationalP, records[0].rationalP)
    }

    @Test
    fun saveBatch() {
        val firstExRate = ExRate()
        firstExRate.sourceCurrencySymbolicCode = "USD"
        firstExRate.sourceCurrencyExponent = 2
        firstExRate.destinationCurrencySymbolicCode = "UZS"
        firstExRate.destinationCurrencyExponent = 6
        firstExRate.rationalP = 11190000264
        firstExRate.rationalQ = 1000000
        firstExRate.rateTimestamp = LocalDateTime.now()

        val secondExRate = ExRate()
        secondExRate.sourceCurrencySymbolicCode = "USD"
        secondExRate.sourceCurrencyExponent = 2
        secondExRate.destinationCurrencySymbolicCode = "RUB"
        secondExRate.destinationCurrencyExponent = 6
        secondExRate.rationalP = 1119000000
        secondExRate.rationalQ = 1000000
        secondExRate.rateTimestamp = LocalDateTime.now()

        val entities = listOf(firstExRate, secondExRate)
        exRateDao.saveBatch(entities)

        val records = dslContext.fetch(EX_RATE)
        assertEquals(entities.size, records.size)
    }

    @Test
    fun getBySymbolicCodes() {
        val exRate = ExRate()
        exRate.sourceCurrencySymbolicCode = "USD"
        exRate.sourceCurrencyExponent = 2
        exRate.destinationCurrencySymbolicCode = "UZS"
        exRate.destinationCurrencyExponent = 6
        exRate.rationalP = 11190000264
        exRate.rationalQ = 1000000
        exRate.rateTimestamp = LocalDateTime.now()

        dslContext.insertInto(EX_RATE)
            .set(dslContext.newRecord(EX_RATE, exRate))
            .execute()

        val result =
            exRateDao.getBySymbolicCodes(exRate.sourceCurrencySymbolicCode, exRate.destinationCurrencySymbolicCode)
        assertEquals(exRate.rationalP, result?.rationalP)
        assertEquals(exRate.rationalQ, result?.rationalQ)
    }
}
