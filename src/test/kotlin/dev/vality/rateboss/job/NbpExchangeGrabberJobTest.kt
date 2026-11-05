package dev.vality.rateboss.job

import dev.vality.exrates.service.ConversionRequest
import dev.vality.exrates.service.CurrencyData
import dev.vality.exrates.service.GetCurrencyExchangeRateRequest
import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.dao.domain.tables.ExRate.EX_RATE
import dev.vality.rateboss.service.ExRateServiceHandler
import org.apache.commons.math3.fraction.BigFraction
import org.awaitility.Awaitility.await
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.quartz.CronTrigger
import org.quartz.JobKey
import org.quartz.Scheduler
import org.quartz.TriggerKey
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.HttpMethod
import org.springframework.http.MediaType
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestTemplate
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

@SpringBootTest(
    properties = [
        "rates.fixer-job.jobTriggerName=",
        "rates.cbr-job.jobTriggerName=",
        "rates.bi-job.jobTriggerName=",
        "rates.nbkz-job.jobTriggerName=",
        "rates.nbkr-job.jobTriggerName=",
        "rates.nbuz-job.jobTriggerName=",
        "rates.nbaz-job.jobTriggerName=",
        "retryTemplate.backOffPeriod=1",
    ],
)
@DirtiesContext
class NbpExchangeGrabberJobTest : ContainerConfiguration() {
    @Autowired
    lateinit var scheduler: Scheduler

    @Autowired
    lateinit var ratesProperties: RatesProperties

    @Autowired
    lateinit var restTemplate: RestTemplate

    @MockitoBean(name = "nbpClock")
    lateinit var clock: Clock

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var handler: ExRateServiceHandler

    private lateinit var server: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        whenever(clock.withZone(any())).thenReturn(Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneId.of("Europe/Warsaw")))
        server = MockRestServiceServer.createServer(restTemplate)
        dslContext.deleteFrom(EX_RATE).execute()
    }

    @AfterEach
    fun verifyRequests() {
        server.verify()
    }

    @ParameterizedTest
    @CsvSource("4, 1, 4, 10000, 2500, 1", "4.25, 4, 17, 20001, 80004, 17", "4.25, 4, 17, 0, 0, 1")
    fun `retry fetching Friday's quote and expose the exact Saturday rate through both APIs`(
        mid: BigDecimal,
        numerator: Long,
        denominator: Long,
        amount: Long,
        convertedNumerator: Long,
        convertedDenominator: Long,
    ) {
        val trigger = scheduler.getTrigger(TriggerKey(ratesProperties.nbpJob.jobTriggerName)) as CronTrigger
        assertEquals("0 0 0/1 * * ?", trigger.cronExpression)
        scheduler.pauseTrigger(trigger.key)
        server
            .expect(requestTo(ENDPOINT))
            .andRespond(withServerError())
        server
            .expect(requestTo(ENDPOINT))
            .andExpect(method(HttpMethod.GET))
            .andRespond(
                withSuccess(
                    """{"code":"EUR","rates":[{"mid":$mid,"effectiveDate":"2026-10-30"}]}""",
                    MediaType.APPLICATION_JSON,
                ),
            )

        scheduler.triggerJob(JobKey(ratesProperties.nbpJob.jobKey))

        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            val record = dslContext.selectFrom(EX_RATE).fetchOne()
            assertNotNull(record)
            assertEquals("nbp", record!!.source)
            assertEquals("PLN", record.sourceCurrencySymbolicCode)
            assertEquals("EUR", record.destinationCurrencySymbolicCode)
            assertEquals(2.toShort(), record.sourceCurrencyExponent)
            assertEquals(2.toShort(), record.destinationCurrencyExponent)
            assertEquals(numerator, record.rationalP)
            assertEquals(denominator, record.rationalQ)
            assertEquals(LocalDateTime.of(2026, 10, 31, 0, 0), record.rateTimestamp)
        }

        val rate =
            handler.getExchangeRateData(
                GetCurrencyExchangeRateRequest()
                    .setCurrencyData(CurrencyData("PLN", "EUR"))
                    .setDatetime("2026-10-31T12:00:00Z"),
            )
        assertEquals(CurrencyData("PLN", "EUR"), rate.currencyData)
        assertEquals("2026-10-31T00:00:00Z", rate.timestamp)
        assertEquals(BigFraction(numerator, denominator), BigFraction(rate.exchange_rate.p, rate.exchange_rate.q))

        val converted =
            handler.getConvertedAmount(
                "nbp",
                ConversionRequest()
                    .setSource("PLN")
                    .setDestination("EUR")
                    .setAmount(amount)
                    .setDatetime("2026-10-31T12:00:00Z"),
            )
        assertEquals(BigFraction(convertedNumerator, convertedDenominator), BigFraction(converted.p, converted.q))
    }

    companion object {
        private const val ENDPOINT = "https://api.nbp.pl/api/exchangerates/rates/A/EUR/?format=json"
    }
}
