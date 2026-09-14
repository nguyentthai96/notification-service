package com.ntt.notificationservice.notification.config

import com.ntt.notificationservice.notification.adapter.`in`.kafka.SmartNotificationDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory
import org.springframework.kafka.listener.DefaultErrorHandler
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.util.backoff.FixedBackOff

/**
 * Kafka consumer configuration — optional transport.
 * Only active when app.notification.kafka.enabled = true.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "app.notification.kafka",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class KafkaConfig(
    private val properties: NotificationProperties
) {

    @Bean
    fun notificationConsumerFactory(): ConsumerFactory<String, ByteArray> {
        val config = mutableMapOf<String, Any>(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to properties.kafka.bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to properties.kafka.consumer.groupId,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to org.apache.kafka.common.serialization.ByteArrayDeserializer::class.java,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to "earliest",
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to false
        )
        return DefaultKafkaConsumerFactory(config)
    }

    @Bean
    fun kafkaListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, ByteArray>,
        kafkaTemplate: KafkaTemplate<String, ByteArray>
    ): ConcurrentKafkaListenerContainerFactory<String, ByteArray> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, ByteArray>()
        factory.setConsumerFactory(consumerFactory)
        factory.setConcurrency(properties.kafka.consumer.concurrency)

        // DLT: publish failed messages to notification-inbound-dlt
        val recoverer = DeadLetterPublishingRecoverer(kafkaTemplate)
        val errorHandler = DefaultErrorHandler(recoverer, FixedBackOff(1000L, 2L))
        factory.setCommonErrorHandler(errorHandler)

        return factory
    }
}
