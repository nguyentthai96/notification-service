package com.ntt.notificationservice.notification.adapter.`in`.web

import com.ntt.notificationservice.notification.adapter.`in`.web.dto.SseTicketResponse
import com.ntt.notificationservice.notification.application.SseConnectionManager
import com.ntt.notificationservice.notification.application.SseTicketService
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Flux

/**
 * SSE Notification controller — ticket generation + SSE stream.
 * Two-Phase Auth: POST /stream/ticket (JWT) → GET /stream?ticket=xxx (no JWT).
 */
@RestController
@RequestMapping("/api/v1/inbox")
class SseNotificationController(
    private val sseTicketService: SseTicketService,
    private val sseConnectionManager: SseConnectionManager
) {

    /**
     * Phase 1: Generate SSE ticket (requires JWT auth via gateway).
     * POST /api/v1/inbox/stream/ticket
     */
    @PostMapping("/stream/ticket")
    fun createTicket(
        @RequestHeader("X-User-Id") userId: Long
    ): ResponseEntity<SseTicketResponse> {
        val ticket = sseTicketService.createTicket(userId)
        return ResponseEntity.ok(ticket)
    }

    /**
     * Phase 2: Open SSE stream using ticket (no JWT required).
     * GET /api/v1/inbox/stream?ticket=xxx&lastEventId=xxx
     *
     * Gateway must whitelist this endpoint (no JWT header check).
     */
    @GetMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun stream(
        @RequestParam ticket: String,
        @RequestParam(required = false) lastEventId: String?
    ): Flux<ServerSentEvent<String>> {
        // Consume ticket atomically (1-use) → get userId
        val userId = sseTicketService.consumeTicket(ticket)

        // Send initial connected event
        val connectedEvent = ServerSentEvent.builder<String>()
            .event("connected")
            .data("""{"type":"CONNECTED","userId":$userId}""")
            .build()

        // Subscribe to real-time events
        val eventStream = sseConnectionManager.subscribe(userId)

        return Flux.concat(Flux.just(connectedEvent), eventStream)
    }
}
