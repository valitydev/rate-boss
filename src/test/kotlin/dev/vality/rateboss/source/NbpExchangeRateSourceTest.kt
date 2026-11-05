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
        NbpExchangeRateSource(client, properties, Clock.fixed(Instant.parse("2026-10-31T12:00:00Z"), ZoneOffset.UTC))

    @ParameterizedTest
    @CsvSource("4, 1, 4", "4.25, 4, 17", "4.25000, 4, 17", "4.25E+0, 4, 17", "4E+1, 1, 40")
    fun `invert the EUR quote exactly and apply it to Saturday`(
        mid: BigDecimal,
        numerator: Long,
        denominator: Long,
    ) {
        whenever(client.getExchangeRates()).thenReturn(
            NbpRatesResponse("EUR", listOf(NbpRateItem(mid, "2026-10-30"))),
        )

        val exchangeRates = source.getExchangeRate("PLN")

        assertEquals(mapOf("EUR" to RateValue.Fraction(BigFraction(numerator, denominator))), exchangeRates.rates)
        assertEquals(Instant.parse("2026-10-31T00:00:00Z").epochSecond, exchangeRates.timestamp)
        assertEquals("nbp", source.getSourceId())
    }

    @ParameterizedTest
    @CsvSource("2026-10-30T23:30:00Z, 2026-10-31T00:00:00Z", "2026-10-30T22:30:00Z, 2026-10-30T00:00:00Z")
    fun `determine the polling day in Warsaw`(
        now: String,
        expectedTimestamp: String,
    ) {
        whenever(client.getExchangeRates()).thenReturn(
            NbpRatesResponse("EUR", listOf(NbpRateItem(BigDecimal("4.25"), "2026-10-30"))),
        )
        val sourceWithFixedClock = NbpExchangeRateSource(client, properties, Clock.fixed(Instant.parse(now), ZoneOffset.UTC))

        assertEquals(Instant.parse(expectedTimestamp).epochSecond, sourceWithFixedClock.getExchangeRate("PLN").timestamp)
    }

    @Test
    fun `use the last published quote when Friday was also a holiday`() {
        whenever(client.getExchangeRates()).thenReturn(
            NbpRatesResponse("EUR", listOf(NbpRateItem(BigDecimal("4.25"), "2026-10-29"))),
        )

        assertEquals(Instant.parse("2026-10-31T00:00:00Z").epochSecond, source.getExchangeRate("PLN").timestamp)
    }

    @ParameterizedTest
    @MethodSource("invalidResponses")
    fun `reject invalid quotes`(response: NbpRatesResponse) {
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
        fun invalidResponses(): List<NbpRatesResponse> {
            val valid = NbpRatesResponse("EUR", listOf(NbpRateItem(BigDecimal("4.25"), "2026-10-30")))
            return listOf(
                valid.copy(code = null),
                valid.copy(code = "USD"),
                valid.copy(rates = null),
                valid.copy(rates = emptyList()),
                valid.copy(rates = listOf(NbpRateItem(null, "2026-10-30"))),
                valid.copy(rates = listOf(NbpRateItem(BigDecimal.ZERO, "2026-10-30"))),
                valid.copy(rates = listOf(NbpRateItem(BigDecimal("-4.25"), "2026-10-30"))),
                valid.copy(rates = listOf(NbpRateItem(BigDecimal("1E-19"), "2026-10-30"))),
                valid.copy(rates = listOf(NbpRateItem(BigDecimal("1E+19"), "2026-10-30"))),
                valid.copy(rates = listOf(NbpRateItem(BigDecimal("4.25"), null))),
                valid.copy(rates = listOf(NbpRateItem(BigDecimal("4.25"), ""))),
                valid.copy(rates = listOf(NbpRateItem(BigDecimal("4.25"), "30.10.2026"))),
                valid.copy(rates = listOf(NbpRateItem(BigDecimal("4.25"), "2026-02-30"))),
            )
        }
    }
}
