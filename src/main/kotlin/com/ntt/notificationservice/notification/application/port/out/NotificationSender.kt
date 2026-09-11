package com.ntt.notificationservice.notification.application.port.out

import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationQueueEntity
import com.ntt.notificationservice.notification.domain.model.NotificationChannel

/**
 * Strategy interface for notification channel senders.
 * Each channel (EMAIL, SMS, PUSH, OTT) implements this interface.
 * NotificationDispatcher auto-discovers and routes to the correct sender.
 */
interface NotificationSender {

    /**
     * Which channel this sender handles.
     */
    fun channel(): NotificationChannel

    /**
     * Send notification via this channel.
     * @throws Exception on delivery failure (caught by scheduler for retry)
     */
    fun send(notification: NotificationQueueEntity)

    /**
     * Check if this sender supports the given channel.
     */
    fun supports(channel: NotificationChannel): Boolean = channel() == channel
}
