package dev.vality.rateboss.job

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import dev.vality.exrates.service.ConversionRequest
import dev.vality.exrates.service.CurrencyData
import dev.vality.exrates.service.GetCurrencyExchangeRateRequest
import dev.vality.rateboss.ContainerConfiguration
import dev.vality.rateboss.config.properties.RatesProperties
import dev.vality.rateboss.dao.domain.tables.ExRate.EX_RATE
import dev.vality.rateboss.model.NbpRatesResponse
import dev.vality.rateboss.service.ExRateServiceHandler
import org.apache.commons.math3.fraction.BigFraction
import org.awaitility.Awaitility.await
import org.jooq.DSLContext
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
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
import java.math.BigInteger
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
        whenever(clock.withZone(any())).thenReturn(Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneId.of("Europe/Warsaw")))
        server = MockRestServiceServer.createServer(restTemplate)
        dslContext.deleteFrom(EX_RATE).execute()
    }

    @AfterEach
    fun verifyRequests() {
        server.verify()
    }

    @ParameterizedTest
    @CsvSource("10000, 25000000, 10979", "20001, 50002500, 10979", "0, 0, 1")
    fun `retry fetching the full table and expose exact Saturday rates through both APIs`(
        amount: Long,
        convertedNumerator: Long,
        convertedDenominator: Long,
    ) {
        val response = checkNotNull(javaClass.getResource("/nbp-table-a.json")).readText()
        val table = jacksonObjectMapper().readValue<List<NbpRatesResponse>>(response).single()
        val expectedRates =
            table.rates!!.associate { item ->
                val mid = checkNotNull(item.mid)
                checkNotNull(item.code) to BigFraction(BigInteger.TEN.pow(mid.scale()), mid.unscaledValue())
            }
        assertEquals(32, expectedRates.size)
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
                    response,
                    MediaType.APPLICATION_JSON,
                ),
            )

        scheduler.triggerJob(JobKey(ratesProperties.nbpJob.jobKey))

        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            val records = dslContext.selectFrom(EX_RATE).fetch()
            assertEquals(expectedRates.keys, records.map { it.destinationCurrencySymbolicCode }.toSet())
            assertEquals(expectedRates.size, records.size)
            for (record in records) {
                assertEquals("nbp", record.source)
                assertEquals("PLN", record.sourceCurrencySymbolicCode)
                assertEquals(2.toShort(), record.sourceCurrencyExponent)
                assertEquals(expectedRates[record.destinationCurrencySymbolicCode], BigFraction(record.rationalP, record.rationalQ))
                assertEquals(LocalDateTime.of(2026, 11, 7, 0, 0), record.rateTimestamp)
            }
            assertEquals(2.toShort(), records.single { it.destinationCurrencySymbolicCode == "EUR" }.destinationCurrencyExponent)
            assertEquals(0.toShort(), records.single { it.destinationCurrencySymbolicCode == "JPY" }.destinationCurrencyExponent)
        }

        for (code in listOf("EUR", "USD", "JPY", "XDR")) {
            val rate =
                handler.getExchangeRateData(
                    GetCurrencyExchangeRateRequest()
                        .setCurrencyData(CurrencyData("PLN", code))
                        .setDatetime("2026-11-07T12:00:00Z"),
                )
            assertEquals(CurrencyData("PLN", code), rate.currencyData)
            assertEquals("2026-11-07T00:00:00Z", rate.timestamp)
            assertEquals(expectedRates[code], BigFraction(rate.exchange_rate.p, rate.exchange_rate.q))

            val converted =
                handler.getConvertedAmount(
                    "nbp",
                    ConversionRequest()
                        .setSource("PLN")
                        .setDestination(code)
                        .setAmount(amount)
                        .setDatetime("2026-11-07T12:00:00Z"),
                )
            assertEquals(expectedRates.getValue(code).multiply(amount), BigFraction(converted.p, converted.q))
            if (code == "EUR") {
                assertEquals(BigFraction(convertedNumerator, convertedDenominator), BigFraction(converted.p, converted.q))
            }
        }
    }

    companion object {
        private const val ENDPOINT = "https://api.nbp.pl/api/exchangerates/tables/A/?format=json"
    }
}
