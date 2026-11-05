package dev.vality.rateboss.config

import com.sun.net.httpserver.HttpServer
import dev.vality.rateboss.client.nbp.NbpApiClient
import dev.vality.rateboss.config.properties.HttpProperties
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.web.client.ResourceAccessException
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HttpConfigurationTest {
    @Test
    fun `stop waiting for a delayed HTTP response and preserve the timeout cause`() {
        val received = CountDownLatch(1)
        val release = CountDownLatch(1)
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/") { exchange ->
            received.countDown()
            try {
                release.await(5, TimeUnit.SECONDS)
            } finally {
                exchange.close()
            }
        }
        server.start()
        try {
            val defaults = TestConfig().testRatesProperties()
            val properties =
                defaults.copy(
                    http = HttpProperties(readTimeout = Duration.ofMillis(150)),
                    source =
                        defaults.source.copy(
                            nbp =
                                defaults.source.nbp.copy(
                                    rootUrl = "http://127.0.0.1:${server.address.port}/",
                                ),
                        ),
                )
            val client = NbpApiClient(AppConfig().restTemplate(properties), properties)

            val exception = assertThrows<ResourceAccessException> { client.getExchangeRates() }

            assertTrue(received.await(1, TimeUnit.SECONDS))
            assertInstanceOf(SocketTimeoutException::class.java, exception.cause)
        } finally {
            release.countDown()
            server.stop(0)
        }
    }
}
