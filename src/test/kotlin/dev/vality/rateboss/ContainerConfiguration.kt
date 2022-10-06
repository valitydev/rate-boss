package dev.vality.rateboss

import org.junit.jupiter.api.BeforeAll
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@SpringBootTest
class ContainerConfiguration {
    companion object {
        @BeforeAll
        @JvmStatic
        fun beforeAll() {
            postgresql.start()
            kafka.start()
        }

        @JvmStatic
        val postgresql: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:14-alpine").apply {
            withDatabaseName("postgresql")
            withUsername("user")
            withPassword("password")
        }

        @JvmStatic
        val kafka: KafkaContainer = KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka").withTag("7.0.1")
        ).withEmbeddedZookeeper()

        @JvmStatic
        @DynamicPropertySource
        fun properties(registry: DynamicPropertyRegistry) {
            registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers)
            registry.add("spring.datasource.url", postgresql::getJdbcUrl)
            registry.add("spring.datasource.password", postgresql::getPassword)
            registry.add("spring.datasource.username", postgresql::getUsername)
            registry.add("spring.flyway.url", postgresql::getJdbcUrl)
            registry.add("spring.flyway.user", postgresql::getUsername)
            registry.add("spring.flyway.password", postgresql::getPassword)
        }
    }
}
