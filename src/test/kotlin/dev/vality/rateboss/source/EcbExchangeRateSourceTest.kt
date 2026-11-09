package dev.vality.rateboss.source

import dev.vality.rateboss.client.ecb.EcbApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.source.impl.EcbExchangeRateSource
import dev.vality.rateboss.source.model.RateValue
import org.apache.commons.math3.fraction.BigFraction
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.springframework.web.client.ResourceAccessException
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class EcbExchangeRateSourceTest {
    private val client: EcbApiClient = mock()
    private val config = TestConfig()
    private val properties = config.testRatesProperties()
    private val source = sourceAt("2026-11-07T12:00:00Z")

    @Test
    fun `parse all currencies from the namespaced daily XML and apply them to Saturday`() {
        whenever(client.getExchangeRates()).thenReturn(checkNotNull(javaClass.getResource("/exchange-rates/ecb-daily.xml")).readText())

        val exchangeRates = source.getExchangeRate("EUR")

        assertEquals(
            "USD JPY CZK DKK GBP HUF PLN RON SEK CHF ISK NOK TRY AUD BRL CAD CNY HKD IDR ILS INR KRW MXN MYR NZD PHP SGD THB ZAR"
                .split(" ")
                .toSet(),
            exchangeRates.rates.keys,
        )
        assertEquals(RateValue.Fraction(BigFraction(2233, 2000)), exchangeRates.rates["USD"])
        assertEquals(RateValue.Fraction(BigFraction(17677, 100)), exchangeRates.rates["JPY"])
        assertEquals(RateValue.Fraction(BigFraction(1757, 400)), exchangeRates.rates["PLN"])
        assertEquals(Instant.parse("2026-11-07T00:00:00Z").epochSecond, exchangeRates.timestamp)
        assertEquals("ecb", source.getSourceId())
    }

    @ParameterizedTest
    @CsvSource("1.1165, 2233, 2000", "1.1165000, 2233, 2000", "1.1165E+0, 2233, 2000", "4E+1, 40, 1")
    fun `preserve exact direct rates without inversion`(
        rate: String,
        numerator: Long,
        denominator: Long,
    ) {
        whenever(client.getExchangeRates()).thenReturn(envelope("<Cube time='2026-11-06'><Cube currency='USD' rate='$rate'/></Cube>"))

        assertEquals(
            mapOf("USD" to RateValue.Fraction(BigFraction(numerator, denominator))),
            source.getExchangeRate("EUR").rates,
        )
    }

    @ParameterizedTest
    @CsvSource(
        "2026-11-06T23:30:00Z, 2026-11-07T00:00:00Z",
        "2026-11-06T22:30:00Z, 2026-11-06T00:00:00Z",
        "2026-07-10T22:30:00Z, 2026-07-11T00:00:00Z",
        "2026-07-10T21:30:00Z, 2026-07-10T00:00:00Z",
        "2026-03-29T00:30:00Z, 2026-03-29T00:00:00Z",
        "2026-03-29T01:30:00Z, 2026-03-29T00:00:00Z",
        "2026-10-25T00:30:00Z, 2026-10-25T00:00:00Z",
        "2026-10-25T01:30:00Z, 2026-10-25T00:00:00Z",
    )
    fun `determine the polling day in Berlin including daylight saving time`(
        now: String,
        expectedTimestamp: String,
    ) {
        whenever(client.getExchangeRates()).thenReturn(envelope("<Cube time='2026-03-27'><Cube currency='USD' rate='1.1'/></Cube>"))

        assertEquals(Instant.parse(expectedTimestamp).epochSecond, sourceAt(now).getExchangeRate("EUR").timestamp)
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    fun `reject invalid tables including an invalid currency after a valid one`(response: String) {
        whenever(client.getExchangeRates()).thenReturn(response)

        val exception = assertThrows<ExchangeRateSourceException> { source.getExchangeRate("EUR") }

        assertEquals("Invalid response from EcbApi", exception.message)
        assertNotNull(exception.cause)
    }

    @Test
    fun `preserve the cause of a network failure`() {
        val cause = ResourceAccessException("Connection failed")
        whenever(client.getExchangeRates()).thenThrow(cause)

        val exception = assertThrows<ExchangeRateSourceException> { source.getExchangeRate("EUR") }

        assertEquals("Failed to get daily rates from ecb", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `reject unsupported base currencies before making a request`() {
        assertThrows<ExchangeRateSourceException> { source.getExchangeRate("PLN") }

        verifyNoInteractions(client)
    }

    private fun sourceAt(now: String): EcbExchangeRateSource =
        EcbExchangeRateSource(client, properties, config.xmlMapper(), Clock.fixed(Instant.parse(now), ZoneOffset.UTC))

    companion object {
        private fun envelope(tables: String): String = "<Envelope><Cube>$tables</Cube></Envelope>"

        @JvmStatic
        fun invalidResponses(): List<String> {
            val validItem = "<Cube currency='USD' rate='1.1'/>"
            val invalidItems =
                listOf(
                    "<Cube rate='1.1'/>",
                    "<Cube currency='' rate='1.1'/>",
                    "<Cube currency=' ' rate='1.1'/>",
                    "<Cube currency='JPY'/>",
                    "<Cube currency='JPY' rate=''/>",
                    "<Cube currency='JPY' rate='invalid'/>",
                    "<Cube currency='JPY' rate='0'/>",
                    "<Cube currency='JPY' rate='-1.1'/>",
                    "<Cube currency='JPY' rate='1E-19'/>",
                    "<Cube currency='JPY' rate='1E+19'/>",
                    validItem,
                )
            return listOf(
                "",
                "<",
                "<Envelope/>",
                "<Envelope><Cube/></Envelope>",
                envelope("<Cube time='2026-11-06'/>"),
                envelope("<Cube>$validItem</Cube>"),
                envelope("<Cube time=''>$validItem</Cube>"),
                envelope("<Cube time='06.11.2026'>$validItem</Cube>"),
                envelope("<Cube time='2026-02-30'>$validItem</Cube>"),
                envelope("<Cube time='2026-11-06'>$validItem</Cube><Cube time='2026-11-05'>$validItem</Cube>"),
            ) + invalidItems.map { envelope("<Cube time='2026-11-06'>$validItem$it</Cube>") }
        }
    }
}
