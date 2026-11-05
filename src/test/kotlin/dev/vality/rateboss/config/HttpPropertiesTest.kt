package dev.vality.rateboss.config

import dev.vality.rateboss.config.properties.HttpProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration

class HttpPropertiesTest {
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
