package com.ntt.notificationservice.notification.application

import com.ntt.notificationservice.notification.adapter.`in`.web.dto.SseTicketResponse
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import org.slf4j.LoggerFactory
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.script.DefaultRedisScript
import org.springframework.stereotype.Service
import java.util.*
import java.time.Duration

/**
 * Two-Phase SSE authentication ticket service.
 * Phase 1: Exchange JWT for short-lived ticket (POST /stream/ticket)
 * Phase 2: Consume ticket to open SSE stream (GET /stream?ticket=xxx)
 *
 * Ticket: UUID, 1-use, 30s TTL, stored in Redis.
 */
@Service
class SseTicketService(
    private val redisTemplate: StringRedisTemplate
) {
    private val log = LoggerFactory.getLogger(SseTicketService::class.java)

    companion object {
        private const val TICKET_PREFIX = "sse:ticket:"
        private const val TICKET_TTL_SECONDS = 30L

        /** Lua script for atomic GET + DEL (1-use ticket consumption). */
        private val CONSUME_TICKET_SCRIPT = DefaultRedisScript<String>(
            """
            local val = redis.call('GET', KEYS[1])
            if val then
                redis.call('DEL', KEYS[1])
            end
            return val
            """.trimIndent(),
            String::class.java
        )
    }

    /**
     * Create a new SSE ticket for the given user.
     * Stores userId in Redis with TTL.
     */
    fun createTicket(userId: Long): SseTicketResponse {
        val ticket = UUID.randomUUID().toString()
        val key = "$TICKET_PREFIX$ticket"

        redisTemplate.opsForValue().set(key, userId.toString(), Duration.ofSeconds(TICKET_TTL_SECONDS))
        log.debug("SSE ticket created: ticket={}, userId={}, ttl={}s", ticket, userId, TICKET_TTL_SECONDS)

        return SseTicketResponse(
            ticket = ticket,
            expiresIn = TICKET_TTL_SECONDS.toInt()
        )
    }

    /**
     * Consume a ticket atomically (GET + DEL via Lua script).
     * Returns userId if ticket is valid, throws exception otherwise.
     */
    fun consumeTicket(ticket: String): Long {
        val key = "$TICKET_PREFIX$ticket"
        val userId = redisTemplate.execute(CONSUME_TICKET_SCRIPT, listOf(key))

        if (userId == null) {
            log.warn("SSE ticket invalid or expired: ticket={}", ticket)
            throw NotificationException(NotificationErrorCode.SSE_AUTH_FAILED)
        }

        log.debug("SSE ticket consumed: ticket={}, userId={}", ticket, userId)
        return userId.toLong()
    }
}
