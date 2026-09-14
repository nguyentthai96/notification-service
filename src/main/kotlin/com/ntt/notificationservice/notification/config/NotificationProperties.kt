package com.ntt.notificationservice.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Notification service configuration properties.
 * Maps to `app.notification.*` in application.yml.
 */
@ConfigurationProperties(prefix = "app.notification")
data class NotificationProperties(
    val queue: QueueProperties = QueueProperties(),
    val channels: ChannelProperties = ChannelProperties(),
    val mail: MailSenderProperties = MailSenderProperties(),
    val sms: SmsProperties = SmsProperties(),
    val push: PushProperties = PushProperties(),
    val ott: OttProperties = OttProperties(),
    val kafka: KafkaTransportProperties = KafkaTransportProperties(),
    val grpc: GrpcTransportProperties = GrpcTransportProperties()
) {
    data class QueueProperties(
        /** Max notifications to process per scheduler tick. */
        val batchSize: Int = 10,
        /** Polling interval in milliseconds. */
        val pollIntervalMs: Long = 5000,
        /** Max retry attempts before marking FAILED. */
        val maxRetries: Int = 3,
        /** Base delay in seconds for exponential backoff (delay = base * 2^retryCount). */
        val baseRetryDelaySeconds: Long = 30,
        /** Delete processed notifications older than this many days. */
        val cleanupAfterDays: Long = 30
    )

    data class ChannelProperties(
        val email: ChannelConfig = ChannelConfig(enabled = true),
        val sms: ChannelConfig = ChannelConfig(enabled = false),
        val push: ChannelConfig = ChannelConfig(enabled = false),
        val ott: ChannelConfig = ChannelConfig(enabled = false)
    )

    data class ChannelConfig(
        val enabled: Boolean = false
    )

    data class MailSenderProperties(
        val from: String = "noreply@example.com",
        val fromName: String = "Notification Service"
    )

    /** SMS channel properties — Twilio provider. */
    data class SmsProperties(
        val twilio: TwilioProperties = TwilioProperties()
    )

    data class TwilioProperties(
        val accountSid: String = "",
        val authToken: String = "",
        val fromNumber: String = ""
    )

    /** Push channel properties — Firebase Cloud Messaging. */
    data class PushProperties(
        val fcm: FcmProperties = FcmProperties()
    )

    data class FcmProperties(
        val credentialsFile: String = ""
    )

    /** OTT channel properties — Telegram, Zalo (future). */
    data class OttProperties(
        val telegram: TelegramProperties = TelegramProperties()
    )

    data class TelegramProperties(
        val botToken: String = "",
        val webhookSecret: String = ""
    )

    /** Kafka transport (optional — plug-and-play). */
    data class KafkaTransportProperties(
        val enabled: Boolean = false,
        val bootstrapServers: String = "localhost:9092",
        val consumer: KafkaConsumerProperties = KafkaConsumerProperties()
    )

    data class KafkaConsumerProperties(
        val groupId: String = "notification-service-group",
        val concurrency: Int = 3
    )

    /** gRPC transport (optional — plug-and-play). */
    data class GrpcTransportProperties(
        val enabled: Boolean = false
    )
}
