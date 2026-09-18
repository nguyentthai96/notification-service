package com.ntt.notificationservice.shared.exception

import com.ntt.basecore.exception.base.ErrorCodeBase
import org.springframework.http.HttpStatus

/**
 * Notification error codes — mirrors AuthErrorCode pattern.
 * Bridge to base-core ErrorCodeBase for unified error handling.
 */
enum class NotificationErrorCode(
    private val code: String,
    private val msgCode: String,
    private val description: String,
    val httpStatus: HttpStatus = HttpStatus.INTERNAL_SERVER_ERROR
) {
    ENQUEUE_FAILED(
        "NOTIF-001", "notification.enqueue.failed",
        "Failed to enqueue notification", HttpStatus.INTERNAL_SERVER_ERROR
    ),
    DUPLICATE(
        "NOTIF-002", "notification.duplicate",
        "Duplicate correlation_id", HttpStatus.CONFLICT
    ),
    TEMPLATE_NOT_FOUND(
        "NOTIF-004", "notification.template.not_found",
        "Template code not found or inactive", HttpStatus.NOT_FOUND
    ),
    CHANNEL_NOT_SUPPORTED(
        "NOTIF-005", "notification.channel.not_supported",
        "Channel type not supported", HttpStatus.BAD_REQUEST
    ),
    INVALID_STATUS_TRANSITION(
        "NOTIF-013", "notification.status.invalid_transition",
        "Invalid status transition", HttpStatus.CONFLICT
    ),
    NOT_FOUND(
        "NOTIF-014", "notification.not_found",
        "Notification not found", HttpStatus.NOT_FOUND
    ),
    INVALID_REQUEST(
        "NOTIF-022", "notification.request.invalid",
        "Invalid notification request", HttpStatus.BAD_REQUEST
    ),
    SMS_DELIVERY_FAILED(
        "NOTIF-006", "notification.sms.delivery_failed",
        "SMS delivery failed", HttpStatus.BAD_GATEWAY
    ),
    PUSH_DELIVERY_FAILED(
        "NOTIF-007", "notification.push.delivery_failed",
        "Push notification delivery failed", HttpStatus.BAD_GATEWAY
    ),
    OTT_DELIVERY_FAILED(
        "NOTIF-008", "notification.ott.delivery_failed",
        "OTT message delivery failed", HttpStatus.BAD_GATEWAY
    ),
    INVALID_PHONE_NUMBER(
        "NOTIF-009", "notification.phone.invalid",
        "Phone number not in E.164 format", HttpStatus.BAD_REQUEST
    ),
    INVALID_DEVICE_TOKEN(
        "NOTIF-010", "notification.device_token.invalid",
        "FCM device token invalid or expired", HttpStatus.BAD_REQUEST
    ),
    DLQ_ENTRY_NOT_FOUND(
        "NOTIF-011", "notification.dlq.not_found",
        "DLQ entry not found", HttpStatus.NOT_FOUND
    ),
    RATE_LIMIT_EXCEEDED(
        "NOTIF-012", "notification.rate_limit.exceeded",
        "Rate limit exceeded for channel", HttpStatus.TOO_MANY_REQUESTS
    ),
    KAFKA_DESERIALIZATION_ERROR(
        "NOTIF-015", "notification.kafka.deserialization_error",
        "Invalid Kafka message format", HttpStatus.BAD_REQUEST
    ),
    GRPC_VALIDATION_ERROR(
        "NOTIF-016", "notification.grpc.validation_error",
        "Invalid gRPC request", HttpStatus.BAD_REQUEST
    ),
    PROVIDER_UNAVAILABLE(
        "NOTIF-017", "notification.provider.unavailable",
        "External provider unavailable (circuit open)", HttpStatus.SERVICE_UNAVAILABLE
    ),

    // --- In-App Notification / Inbox ---
    INBOX_NOT_FOUND(
        "NOTIF-INB-001", "notification.inbox.not_found",
        "Inbox notification not found", HttpStatus.NOT_FOUND
    ),
    INBOX_ACCESS_DENIED(
        "NOTIF-INB-002", "notification.inbox.access_denied",
        "Access denied to this notification", HttpStatus.FORBIDDEN
    ),
    INBOX_RATE_LIMIT(
        "NOTIF-INB-003", "notification.inbox.rate_limit",
        "Inbox rate limit exceeded", HttpStatus.TOO_MANY_REQUESTS
    ),

    // --- SSE Stream ---
    SSE_AUTH_FAILED(
        "NOTIF-SSE-001", "notification.sse.auth_failed",
        "SSE authentication failed — invalid or missing ticket", HttpStatus.UNAUTHORIZED
    ),
    SSE_TICKET_EXPIRED(
        "NOTIF-SSE-002", "notification.sse.ticket_expired",
        "SSE ticket expired or already consumed", HttpStatus.UNAUTHORIZED
    ),
    SSE_CONNECTION_LIMIT(
        "NOTIF-SSE-003", "notification.sse.connection_limit",
        "SSE connection limit reached", HttpStatus.TOO_MANY_REQUESTS
    );

    /**
     * Bridge to base-core ErrorCodeBase for unified exception handling.
     */
    fun toErrorCodeBase(): ErrorCodeBase {
        return object : ErrorCodeBase(
            this@NotificationErrorCode.code,
            this@NotificationErrorCode.msgCode,
            this@NotificationErrorCode.description
        ) {}
    }
}
