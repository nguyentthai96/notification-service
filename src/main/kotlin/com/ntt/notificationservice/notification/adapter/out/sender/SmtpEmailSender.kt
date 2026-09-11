package com.ntt.notificationservice.notification.adapter.out.sender

import com.ntt.notificationservice.notification.adapter.out.persistence.entity.NotificationQueueEntity
import com.ntt.notificationservice.notification.application.port.out.NotificationSender
import com.ntt.notificationservice.notification.config.NotificationProperties
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import io.github.resilience4j.circuitbreaker.CircuitBreaker
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry
import jakarta.mail.internet.MimeMessage
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/**
 * SMTP email sender — Phase 1 implementation.
 * Uses JavaMailSender with Resilience4j circuit breaker.
 */
@Component
@ConditionalOnProperty(
    prefix = "app.notification.channels.email",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = true
)
class SmtpEmailSender(
    private val javaMailSender: JavaMailSender,
    private val properties: NotificationProperties,
    circuitBreakerRegistry: CircuitBreakerRegistry
) : NotificationSender {

    private val log = LoggerFactory.getLogger(SmtpEmailSender::class.java)
    private val circuitBreaker: CircuitBreaker = circuitBreakerRegistry.circuitBreaker("emailCircuitBreaker")

    override fun channel(): NotificationChannel = NotificationChannel.EMAIL

    override fun send(notification: NotificationQueueEntity) {
        circuitBreaker.executeRunnable {
            val message: MimeMessage = javaMailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(properties.mail.from, properties.mail.fromName)
            helper.setTo(notification.recipient)
            helper.setSubject(notification.subject ?: "Notification")
            helper.setText(notification.bodyRendered ?: "", true)

            javaMailSender.send(message)
            log.info("Email sent to={}, templateCode={}", notification.recipient, notification.templateCode)
        }
    }
}
