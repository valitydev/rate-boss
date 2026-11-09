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
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
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
import org.w3c.dom.Element
import java.math.BigInteger
import java.time.Clock
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit
import javax.xml.parsers.DocumentBuilderFactory

@SpringBootTest(
    properties = [
        "rates.fixer-job.jobTriggerName=",
        "rates.cbr-job.jobTriggerName=",
        "rates.bi-job.jobTriggerName=",
        "rates.nbkz-job.jobTriggerName=",
        "rates.nbkr-job.jobTriggerName=",
        "rates.nbuz-job.jobTriggerName=",
        "rates.nbaz-job.jobTriggerName=",
        "rates.nbp-job.jobTriggerName=",
        "retryTemplate.backOffPeriod=1",
    ],
)
@DirtiesContext
class EcbExchangeGrabberJobTest : ContainerConfiguration() {
    @Autowired
    lateinit var scheduler: Scheduler

    @Autowired
    lateinit var ratesProperties: RatesProperties

    @Autowired
    lateinit var restTemplate: RestTemplate

    @MockitoBean(name = "ecbClock")
    lateinit var clock: Clock

    @Autowired
    lateinit var dslContext: DSLContext

    @Autowired
    lateinit var handler: ExRateServiceHandler

    private lateinit var server: MockRestServiceServer

    @BeforeEach
    fun setUp() {
        scheduler.pauseTrigger(TriggerKey(ratesProperties.ecbJob.jobTriggerName))
        whenever(clock.withZone(any())).thenReturn(Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneId.of("Europe/Berlin")))
        server = MockRestServiceServer.createServer(restTemplate)
        dslContext.deleteFrom(EX_RATE).execute()
    }

    @AfterEach
    fun verifyRequests() {
        server.verify()
    }

    @ParameterizedTest
    @ValueSource(longs = [10000, 20001, 0])
    fun `retry fetching the full XML and expose exact Saturday rates through both APIs`(amount: Long) {
        val response = checkNotNull(javaClass.getResource("/exchange-rates/ecb-daily.xml")).readText()
        val expectedRates = expectedRates(response)
        assertEquals(29, expectedRates.size)
        val trigger = scheduler.getTrigger(TriggerKey(ratesProperties.ecbJob.jobTriggerName)) as CronTrigger
        assertEquals("0 0 0/1 * * ?", trigger.cronExpression)
        server.expect(requestTo(ENDPOINT)).andRespond(withServerError())
        server
            .expect(requestTo(ENDPOINT))
            .andExpect(method(HttpMethod.GET))
            .andRespond(withSuccess(response, MediaType.TEXT_XML))

        scheduler.triggerJob(JobKey(ratesProperties.ecbJob.jobKey))

        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            val records = dslContext.selectFrom(EX_RATE).fetch()
            assertEquals(expectedRates.keys, records.map { it.destinationCurrencySymbolicCode }.toSet())
            assertEquals(expectedRates.size, records.size)
            for (record in records) {
                assertEquals("ecb", record.source)
                assertEquals("EUR", record.sourceCurrencySymbolicCode)
                assertEquals(2.toShort(), record.sourceCurrencyExponent)
                assertEquals(expectedRates[record.destinationCurrencySymbolicCode], BigFraction(record.rationalP, record.rationalQ))
                assertEquals(LocalDateTime.of(2026, 11, 7, 0, 0), record.rateTimestamp)
            }
            assertEquals(2.toShort(), records.single { it.destinationCurrencySymbolicCode == "USD" }.destinationCurrencyExponent)
            assertEquals(0.toShort(), records.single { it.destinationCurrencySymbolicCode == "JPY" }.destinationCurrencyExponent)
        }

        for (code in listOf("USD", "JPY", "PLN")) {
            val rate =
                handler.getExchangeRateData(
                    GetCurrencyExchangeRateRequest()
                        .setCurrencyData(CurrencyData("EUR", code))
                        .setDatetime("2026-11-07T12:00:00Z"),
                )
            assertEquals(CurrencyData("EUR", code), rate.currencyData)
            assertEquals("2026-11-07T00:00:00Z", rate.timestamp)
            assertEquals(expectedRates[code], BigFraction(rate.exchange_rate.p, rate.exchange_rate.q))

            val converted =
                handler.getConvertedAmount(
                    "ecb",
                    ConversionRequest()
                        .setSource("EUR")
                        .setDestination(code)
                        .setAmount(amount)
                        .setDatetime("2026-11-07T12:00:00Z"),
                )
            assertEquals(expectedRates.getValue(code).multiply(amount), BigFraction(converted.p, converted.q))
        }
    }

    @Test
    fun `expose the second publication loaded on the same polling day through both APIs`() {
        val response = checkNotNull(javaClass.getResource("/exchange-rates/ecb-daily.xml")).readText()
        val refreshedResponse = response.replace("rate='1.1165'", "rate='1.12'").replace("time='2026-11-06'", "time='2026-11-07'")
        assertNotEquals(response, refreshedResponse)
        val currencyCount = expectedRates(response).size
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(response, MediaType.TEXT_XML))
        server.expect(requestTo(ENDPOINT)).andRespond(withSuccess(refreshedResponse, MediaType.TEXT_XML))

        scheduler.triggerJob(JobKey(ratesProperties.ecbJob.jobKey))

        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            assertEquals(currencyCount, dslContext.fetchCount(EX_RATE))
            assertFalse(scheduler.checkExists(JobKey("ecbJob-EUR-currency-job")))
        }
        assertUsdRate(BigFraction(2233, 2000))

        scheduler.triggerJob(JobKey(ratesProperties.ecbJob.jobKey))

        await().atMost(30, TimeUnit.SECONDS).untilAsserted {
            assertEquals(currencyCount * 2, dslContext.fetchCount(EX_RATE))
        }
        assertUsdRate(BigFraction(28, 25))
    }

    private fun assertUsdRate(expectedRate: BigFraction) {
        val rate =
            handler.getExchangeRateData(
                GetCurrencyExchangeRateRequest()
                    .setCurrencyData(CurrencyData("EUR", "USD"))
                    .setDatetime("2026-11-07T12:00:00Z"),
            )
        assertEquals("2026-11-07T00:00:00Z", rate.timestamp)
        assertEquals(expectedRate, BigFraction(rate.exchange_rate.p, rate.exchange_rate.q))

        val converted =
            handler.getConvertedAmount(
                "ecb",
                ConversionRequest()
                    .setSource("EUR")
                    .setDestination("USD")
                    .setAmount(10000)
                    .setDatetime("2026-11-07T12:00:00Z"),
            )
        assertEquals(expectedRate.multiply(10000), BigFraction(converted.p, converted.q))
    }

    private fun expectedRates(xml: String): Map<String, BigFraction> {
        val document =
            DocumentBuilderFactory
                .newInstance()
                .apply { isNamespaceAware = true }
                .newDocumentBuilder()
                .parse(xml.byteInputStream())
        val cubes = document.getElementsByTagNameNS("*", "Cube")
        return (0 until cubes.length)
            .map { cubes.item(it) as Element }
            .filter { it.hasAttribute("currency") }
            .associate { item ->
                val rate = item.getAttribute("rate").toBigDecimal()
                item.getAttribute("currency") to BigFraction(rate.unscaledValue(), BigInteger.TEN.pow(rate.scale()))
            }
    }

    companion object {
        private const val ENDPOINT = "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml"
    }
}
