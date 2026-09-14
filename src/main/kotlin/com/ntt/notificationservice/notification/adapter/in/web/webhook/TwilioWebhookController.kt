package com.ntt.notificationservice.notification.adapter.`in`.web.webhook

import com.ntt.notificationservice.notification.application.DeliveryTrackingService
import com.ntt.notificationservice.notification.config.NotificationProperties
import com.twilio.security.RequestValidator
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Twilio SMS delivery status webhook receiver.
 * Validates X-Twilio-Signature (HMAC-SHA1) for security.
 */
@RestController
@RequestMapping("/api/v1/webhooks/twilio")
@ConditionalOnProperty(
    prefix = "app.notification.channels.sms",
    name = ["enabled"],
    havingValue = "true"
)
class TwilioWebhookController(
    private val deliveryTrackingService: DeliveryTrackingService,
    private val properties: NotificationProperties
) {
    private val log = LoggerFactory.getLogger(TwilioWebhookController::class.java)

    @PostMapping
    fun handleCallback(
        @RequestParam params: Map<String, String>,
        request: HttpServletRequest
    ): ResponseEntity<Void> {
        // Validate Twilio signature
        val signature = request.getHeader("X-Twilio-Signature")
        if (signature != null && properties.sms.twilio.authToken.isNotBlank()) {
            val validator = RequestValidator(properties.sms.twilio.authToken)
            val requestUrl = request.requestURL.toString()
            if (!validator.validate(requestUrl, params, signature)) {
                log.warn("Invalid Twilio signature for webhook callback")
                return ResponseEntity.status(403).build()
            }
        }

        val messageSid = params["MessageSid"] ?: params["SmsSid"]
        val messageStatus = params["MessageStatus"] ?: params["SmsStatus"]

        if (messageSid == null || messageStatus == null) {
            log.warn("Missing required Twilio webhook params: MessageSid={}, MessageStatus={}", messageSid, messageStatus)
            return ResponseEntity.badRequest().build()
        }

        deliveryTrackingService.updateDeliveryStatus(messageSid, messageStatus)

        return ResponseEntity.ok().build()
    }
}
