package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.application.port.out.NotificationSender
import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationQueueEntity
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Notification dispatcher — Strategy Pattern router.
 * Auto-discovers all NotificationSender beans and routes by channel.
 */
@Component
class NotificationDispatcher(
    senders: List<NotificationSender>
) {
    private val log = LoggerFactory.getLogger(NotificationDispatcher::class.java)

    private val senderMap: Map<NotificationChannel, NotificationSender> =
        senders.associateBy { it.channel() }

    init {
        log.info("Registered notification senders: {}", senderMap.keys)
    }

    /**
     * Dispatch notification to the appropriate channel sender.
     * @throws NotificationException if channel is not supported
     */
    fun dispatch(notification: NotificationQueueEntity) {
        val sender = senderMap[notification.channel]
            ?: throw NotificationException(
                NotificationErrorCode.CHANNEL_NOT_SUPPORTED,
                "Channel not supported: ${notification.channel}"
            )
        sender.send(notification)
    }
}
