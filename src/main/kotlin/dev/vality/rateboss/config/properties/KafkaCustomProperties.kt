package dev.vality.rateboss.config.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kafka")
data class KafkaCustomProperties(
    val producer: KafkaProducerProperties,
    val topic: KafkaTopicProperties,
)

data class KafkaProducerProperties(
    val maxRetries: Int,
    val retryBackoffMs: Long,
    val batchSize: Int,
    val deliveryTimeoutMs: Int,
)

data class KafkaTopicProperties(
    val producer: KafkaTopicProducerProperties,
)

data class KafkaTopicProducerProperties(
    val exchangeTopic: String,
)
