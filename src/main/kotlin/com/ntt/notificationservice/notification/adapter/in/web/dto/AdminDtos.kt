package com.ntt.notificationservice.notification.adapter.`in`.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant

/** DLQ entry response */
data class DlqEntryResponse(
    val id: Long,
    val notificationId: Long,
    val channel: String,
    val errorCode: String?,
    val errorMessage: String?,
    val resolved: Boolean,
    val resolvedAt: Instant?,
    val createdAt: Instant?
)

/** Register device token request */
data class RegisterDeviceTokenRequest(
    @field:NotNull(message = "userId is required")
    val userId: Long,

    @field:NotBlank(message = "token is required")
    val token: String,

    @field:NotBlank(message = "platform is required (ANDROID/IOS/WEB)")
    val platform: String
)

/** Device token response */
data class DeviceTokenResponse(
    val id: Long,
    val userId: Long,
    val token: String,
    val platform: String,
    val active: Boolean,
    val createdAt: Instant?
)
