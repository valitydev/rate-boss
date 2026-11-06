package dev.vality.rateboss.source

import dev.vality.rateboss.client.nbp.NbpApiClient
import dev.vality.rateboss.config.TestConfig
import dev.vality.rateboss.model.NbpRateItem
import dev.vality.rateboss.model.NbpRatesResponse
import dev.vality.rateboss.source.impl.NbpExchangeRateSource
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
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset

class NbpExchangeRateSourceTest {
    private val client: NbpApiClient = mock()
    private val properties = TestConfig().testRatesProperties()
    private val source =
        NbpExchangeRateSource(client, properties, Clock.fixed(Instant.parse("2026-11-07T12:00:00Z"), ZoneOffset.UTC))

    @ParameterizedTest
    @CsvSource("4, 1, 4", "4.3916, 2500, 10979", "4.3916000, 2500, 10979", "4.3916E+0, 2500, 10979", "4E+1, 1, 40")
    fun `invert each currency quote exactly and apply it to Saturday`(
        mid: BigDecimal,
        numerator: Long,
        denominator: Long,
    ) {
        whenever(client.getExchangeRates()).thenReturn(
            listOf(NbpRatesResponse("2026-11-05", listOf(NbpRateItem(mid, "EUR"), NbpRateItem(BigDecimal("4"), "USD")))),
        )

        val exchangeRates = source.getExchangeRate("PLN")

        assertEquals(
            mapOf("EUR" to RateValue.Fraction(BigFraction(numerator, denominator)), "USD" to RateValue.Fraction(BigFraction(1, 4))),
            exchangeRates.rates,
        )
        assertEquals(Instant.parse("2026-11-07T00:00:00Z").epochSecond, exchangeRates.timestamp)
        assertEquals("nbp", source.getSourceId())
    }

    @ParameterizedTest
    @CsvSource("2026-11-06T23:30:00Z, 2026-11-07T00:00:00Z", "2026-11-06T22:30:00Z, 2026-11-06T00:00:00Z")
    fun `determine the polling day in Warsaw`(
        now: String,
        expectedTimestamp: String,
    ) {
        whenever(client.getExchangeRates()).thenReturn(
            listOf(NbpRatesResponse("2026-11-06", listOf(NbpRateItem(BigDecimal("4.3916"), "EUR")))),
        )
        val sourceWithFixedClock = NbpExchangeRateSource(client, properties, Clock.fixed(Instant.parse(now), ZoneOffset.UTC))

        assertEquals(Instant.parse(expectedTimestamp).epochSecond, sourceWithFixedClock.getExchangeRate("PLN").timestamp)
    }

    @Test
    fun `use the last published quote when no quote was published on Friday`() {
        whenever(client.getExchangeRates()).thenReturn(
            listOf(NbpRatesResponse("2026-11-05", listOf(NbpRateItem(BigDecimal("4.3916"), "EUR")))),
        )

        assertEquals(Instant.parse("2026-11-07T00:00:00Z").epochSecond, source.getExchangeRate("PLN").timestamp)
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    fun `reject invalid tables`(response: List<NbpRatesResponse>) {
        whenever(client.getExchangeRates()).thenReturn(response)

        val exception = assertThrows<ExchangeRateSourceException> { source.getExchangeRate("PLN") }

        assertEquals("Invalid response from NbpApi", exception.message)
        assertNotNull(exception.cause)
    }

    @Test
    fun `preserve the cause of a network failure`() {
        val cause = ResourceAccessException("Connection failed")
        whenever(client.getExchangeRates()).thenThrow(cause)

        val exception = assertThrows<ExchangeRateSourceException> { source.getExchangeRate("PLN") }

        assertEquals("Failed to get daily rates from nbp", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun `reject unsupported base currencies before making a request`() {
        assertThrows<ExchangeRateSourceException> { source.getExchangeRate("EUR") }

        verifyNoInteractions(client)
    }

    companion object {
        @JvmStatic
        fun invalidResponses(): List<List<NbpRatesResponse>> {
            val item = NbpRateItem(BigDecimal("4.3916"), "EUR")
            val valid = NbpRatesResponse("2026-11-05", listOf(item))
            val invalidTables =
                listOf(
                    valid.copy(rates = null),
                    valid.copy(rates = emptyList()),
                    valid.copy(effectiveDate = null),
                    valid.copy(effectiveDate = ""),
                    valid.copy(effectiveDate = "05.11.2026"),
                    valid.copy(effectiveDate = "2026-02-30"),
                )
            val invalidItems =
                listOf(
                    item.copy(code = null),
                    item.copy(code = ""),
                    item.copy(code = " "),
                    item.copy(mid = null),
                    item.copy(mid = BigDecimal.ZERO),
                    item.copy(mid = BigDecimal("-4.3916")),
                    item.copy(mid = BigDecimal("1E-19")),
                    item.copy(mid = BigDecimal("1E+19")),
                )
            return listOf(emptyList(), listOf(valid, valid)) +
                invalidTables.map { listOf(it) } +
                invalidItems.map { listOf(valid.copy(rates = listOf(item.copy(code = "USD"), it))) } +
                listOf(listOf(valid.copy(rates = listOf(item, item))))
        }
    }
}
