package com.ntt.notificationservice.notification.config

import com.ntt.notificationservice.notification.application.SseConnectionManager
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.Message
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.listener.PatternTopic
import org.springframework.data.redis.listener.RedisMessageListenerContainer

/**
 * Redis configuration for inbox Pub/Sub.
 * Subscribes to notification:inbox:* channels for multi-instance SSE broadcast.
 */
@Configuration
@ConditionalOnProperty("app.notification.inApp.enabled", havingValue = "true", matchIfMissing = true)
class RedisInboxConfig {

    private val log = LoggerFactory.getLogger(RedisInboxConfig::class.java)

    companion object {
        private const val CHANNEL_PATTERN = "notification:inbox:*"
    }

    @Bean
    fun inboxMessageListenerContainer(
        connectionFactory: RedisConnectionFactory,
        sseConnectionManager: SseConnectionManager
    ): RedisMessageListenerContainer {
        val container = RedisMessageListenerContainer()
        container.setConnectionFactory(connectionFactory)

        val listener = object : MessageListener {
            override fun onMessage(message: Message, pattern: ByteArray?) {
                try {
                    val channel = String(message.channel)
                    val body = String(message.body)

                    // Extract userId from channel: notification:inbox:{userId}
                    val userId = channel.substringAfterLast(":").toLongOrNull()
                    if (userId != null) {
                        sseConnectionManager.onRedisMessage(userId, body)
                    } else {
                        log.warn("Invalid Redis channel format: {}", channel)
                    }
                } catch (e: Exception) {
                    log.error("Error processing Redis inbox message", e)
                }
            }
        }

        container.addMessageListener(listener, PatternTopic(CHANNEL_PATTERN))
        log.info("Redis Pub/Sub listener registered: pattern={}", CHANNEL_PATTERN)
        return container
    }
}
