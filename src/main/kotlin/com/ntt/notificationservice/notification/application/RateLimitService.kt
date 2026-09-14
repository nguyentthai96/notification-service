package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.adapter.out.persistence.repository.NotificationQueueRepository
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

/**
 * Rate limit service — prevents notification spam.
 * Rule: Max 1 SMS per user per template per hour.
 */
@Service
class RateLimitService(
    private val queueRepository: NotificationQueueRepository
) {
    private val log = LoggerFactory.getLogger(RateLimitService::class.java)

    /**
     * Check rate limit for SMS channel.
     * @throws NotificationException if rate limit exceeded
     */
    fun checkRateLimit(recipient: String, channel: NotificationChannel, templateCode: String) {
        if (channel != NotificationChannel.SMS) return

        val oneHourAgo = Instant.now().minusSeconds(3600)
        val recentCount = queueRepository.countByRecipientAndTemplateCodeAndChannelAndCreatedAtAfter(
            recipient, templateCode, channel, oneHourAgo
        )

        if (recentCount > 0) {
            log.warn("Rate limit exceeded: recipient={}, channel={}, templateCode={}", recipient, channel, templateCode)
            throw NotificationException(
                NotificationErrorCode.RATE_LIMIT_EXCEEDED,
                "Rate limit exceeded: max 1 SMS per recipient per template per hour"
            )
        }
    }
}
