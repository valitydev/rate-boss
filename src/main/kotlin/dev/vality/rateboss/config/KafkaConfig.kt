package dev.vality.rateboss.config

import dev.vality.exrates.events.CurrencyEvent
import dev.vality.kafka.common.serialization.ThriftSerializer
import dev.vality.rateboss.config.properties.KafkaCustomProperties
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class KafkaConfig {

    @Autowired
    lateinit var kafkaProperties: KafkaProperties

    @Autowired
    lateinit var kafkaCustomProperties: KafkaCustomProperties

    fun producerConfigs(
        maxRetries: Int,
        retryBackoffMs: Long,
        batchSize: Int,
        acks: String,
        deliveryTimeout: Int
    ): Map<String, Any> {
        val props: MutableMap<String, Any> = HashMap(kafkaProperties.buildProducerProperties())
        props[ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG] = StringSerializer::class.java
        props[ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG] = ThriftSerializer::class.java
        props[ProducerConfig.BATCH_SIZE_CONFIG] = batchSize
        props[ProducerConfig.ACKS_CONFIG] = ACKS_CONFIG
        props[ProducerConfig.RETRIES_CONFIG] = maxRetries
        props[ProducerConfig.RETRY_BACKOFF_MS_CONFIG] = retryBackoffMs
        props[ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG] = deliveryTimeout
        return props
    }

    @Bean
    fun producerFactory(): ProducerFactory<String, CurrencyEvent> {
        return DefaultKafkaProducerFactory(
            producerConfigs(
                kafkaCustomProperties.producer.maxRetries,
                kafkaCustomProperties.producer.retryBackoffMs,
                kafkaCustomProperties.producer.batchSize,
                ACKS_CONFIG,
                kafkaCustomProperties.producer.deliveryTimeoutMs
            )
        )
    }

    @Bean
    fun kafkaTemplate(producerFactory: ProducerFactory<String, CurrencyEvent>): KafkaTemplate<String, CurrencyEvent> {
        return KafkaTemplate(producerFactory)
    }

    private companion object {
        const val ACKS_CONFIG = "1"
    }
}
