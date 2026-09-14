package com.ntt.notificationservice.notification.adapter.`in`.web.webhook

import com.ntt.notificationservice.notification.application.DeliveryTrackingService
import com.ntt.notificationservice.notification.config.NotificationProperties
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Telegram Bot API webhook receiver.
 * Validates X-Telegram-Bot-Api-Secret-Token header.
 * Mode: Webhook for production (DD-010).
 */
@RestController
@RequestMapping("/api/v1/webhooks/telegram")
@ConditionalOnProperty(
    prefix = "app.notification.channels.ott",
    name = ["enabled"],
    havingValue = "true"
)
class TelegramWebhookController(
    private val deliveryTrackingService: DeliveryTrackingService,
    private val properties: NotificationProperties
) {
    private val log = LoggerFactory.getLogger(TelegramWebhookController::class.java)

    @PostMapping
    fun handleUpdate(
        @RequestBody body: Map<String, Any>,
        @RequestHeader("X-Telegram-Bot-Api-Secret-Token", required = false) secretToken: String?
    ): ResponseEntity<Void> {
        // Validate secret token
        val expectedSecret = properties.ott.telegram.webhookSecret
        if (expectedSecret.isNotBlank() && secretToken != expectedSecret) {
            log.warn("Invalid Telegram webhook secret token")
            return ResponseEntity.status(403).build()
        }

        // Parse Telegram Update for delivery confirmations
        // Telegram doesn't provide read receipts natively (ISS-001)
        // Handle callback_query for inline button "read" confirmation workaround
        val callbackQuery = body["callback_query"] as? Map<*, *>
        if (callbackQuery != null) {
            val data = callbackQuery["data"] as? String
            if (data?.startsWith("read:") == true) {
                val messageId = data.removePrefix("read:")
                deliveryTrackingService.updateDeliveryStatus(messageId, "READ")
            }
        }

        return ResponseEntity.ok().build()
    }
}
