package com.ntt.notificationservice.notification.adapter.`in`.web.admin

import com.ntt.notificationservice.notification.adapter.out.persistence.entity.DeviceTokenEntity
import com.ntt.notificationservice.notification.adapter.out.persistence.repository.DeviceTokenRepository
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.DeviceTokenResponse
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.RegisterDeviceTokenRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Admin API for device token management (FCM push notifications).
 */
@RestController
@RequestMapping("/api/v1/admin/device-tokens")
class DeviceTokenController(
    private val deviceTokenRepository: DeviceTokenRepository
) {

    @PostMapping
    fun register(@Valid @RequestBody request: RegisterDeviceTokenRequest): ResponseEntity<DeviceTokenResponse> {
        val existing = deviceTokenRepository.findByToken(request.token)
        if (existing != null) {
            existing.active = true
            existing.userId = request.userId
            existing.platform = request.platform
            val saved = deviceTokenRepository.save(existing)
            return ResponseEntity.ok(saved.toResponse())
        }

        val entity = DeviceTokenEntity().apply {
            userId = request.userId
            token = request.token
            platform = request.platform
            active = true
        }
        val saved = deviceTokenRepository.save(entity)
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.toResponse())
    }

    @GetMapping("/{userId}")
    fun listByUser(@PathVariable userId: Long): ResponseEntity<List<DeviceTokenResponse>> {
        val tokens = deviceTokenRepository.findByUserId(userId)
        return ResponseEntity.ok(tokens.map { it.toResponse() })
    }

    @PutMapping("/{id}")
    fun update(@PathVariable id: Long, @Valid @RequestBody request: RegisterDeviceTokenRequest): ResponseEntity<DeviceTokenResponse> {
        val entity = deviceTokenRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        entity.userId = request.userId
        entity.token = request.token
        entity.platform = request.platform
        val saved = deviceTokenRepository.save(entity)
        return ResponseEntity.ok(saved.toResponse())
    }

    @DeleteMapping("/{id}")
    fun deactivate(@PathVariable id: Long): ResponseEntity<Void> {
        val entity = deviceTokenRepository.findById(id).orElse(null)
            ?: return ResponseEntity.notFound().build()
        entity.active = false
        deviceTokenRepository.save(entity)
        return ResponseEntity.noContent().build()
    }

    private fun DeviceTokenEntity.toResponse() = DeviceTokenResponse(
        id = id!!,
        userId = userId,
        token = token,
        platform = platform,
        active = active,
        createdAt = createdAt
    )
}
