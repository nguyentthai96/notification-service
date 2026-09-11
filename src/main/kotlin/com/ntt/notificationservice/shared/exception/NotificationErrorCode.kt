package com.ntt.notificationservice.shared.exception

import com.ntt.basecore.exception.ErrorCodeBase
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
    );

    /**
     * Bridge to base-core ErrorCodeBase for unified exception handling.
     */
    fun toErrorCodeBase(): ErrorCodeBase {
        return object : ErrorCodeBase {
            override fun getCode(): String = this@NotificationErrorCode.code
            override fun getMsgCode(): String = this@NotificationErrorCode.msgCode
            override fun getDesc(): String = this@NotificationErrorCode.description
        }
    }
}
