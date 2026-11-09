package dev.vality.rateboss.config

import dev.vality.rateboss.config.properties.HttpProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource
import java.time.Duration

class HttpPropertiesTest {
    @Test
    fun `bind custom HTTP timeouts and retain defaults for omitted values`() {
        val source =
            MapConfigurationPropertySource(
                mapOf("rates.http.connect-timeout" to "2s", "rates.http.read-timeout" to "150ms"),
            )
        val configured = Binder(source).bind("rates.http", Bindable.of(HttpProperties::class.java)).get()

        assertEquals(Duration.ofSeconds(2), configured.connectTimeout)
        assertEquals(Duration.ofMillis(150), configured.readTimeout)

        val partialSource = MapConfigurationPropertySource(mapOf("rates.http.read-timeout" to "150ms"))
        val partial = Binder(partialSource).bind("rates.http", Bindable.of(HttpProperties::class.java)).get()

        assertEquals(Duration.ofSeconds(5), partial.connectTimeout)
        assertEquals(Duration.ofMillis(150), partial.readTimeout)
    }

    @Test
    fun `use default HTTP timeouts`() {
        val properties = HttpProperties()

        assertEquals(Duration.ofSeconds(5), properties.connectTimeout)
        assertEquals(Duration.ofSeconds(15), properties.readTimeout)
    }

    @ParameterizedTest
    @CsvSource(
        "PT0S, PT15S",
        "PT5S, PT0S",
        "PT-1S, PT15S",
        "PT5S, PT-1S",
        "PT0.0001S, PT15S",
        "PT5S, PT0.0001S",
        "PT60.001S, PT15S",
        "PT5S, PT60.001S",
    )
    fun `reject timeouts outside the allowed range`(
        connect: String,
        read: String,
    ) {
        assertThrows<IllegalArgumentException> {
            HttpProperties(connectTimeout = Duration.parse(connect), readTimeout = Duration.parse(read))
        }
    }

    @ParameterizedTest
    @CsvSource("PT0.001S, PT60S", "PT60S, PT0.001S")
    fun `accept timeouts at both range boundaries`(
        connect: String,
        read: String,
    ) {
        val properties = HttpProperties(connectTimeout = Duration.parse(connect), readTimeout = Duration.parse(read))

        assertEquals(Duration.parse(connect), properties.connectTimeout)
        assertEquals(Duration.parse(read), properties.readTimeout)
    }
}
