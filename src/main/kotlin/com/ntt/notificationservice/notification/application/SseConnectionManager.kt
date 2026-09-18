package com.ntt.notificationservice.notification.application

import com.fasterxml.jackson.databind.ObjectMapper
import com.ntt.notificationservice.notification.application.port.out.InboxNotificationEvent
import com.ntt.notificationservice.notification.application.port.out.RealtimeNotificationPort
import com.ntt.notificationservice.notification.config.NotificationProperties
import io.micrometer.core.instrument.MeterRegistry
import jakarta.annotation.PreDestroy
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.codec.ServerSentEvent
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Sinks
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicLong

/**
 * SSE Connection Manager — implements RealtimeNotificationPort.
 * Manages per-user SSE Reactor Sinks, heartbeat, timeout, and Redis Pub/Sub broadcast.
 *
 * Multi-instance: publishes events to Redis channel, listens on RedisInboxConfig.
 * Multi-tab: supports multiple sinks per userId via CopyOnWriteArrayList.
 */
@Component
@ConditionalOnProperty("app.notification.inApp.enabled", havingValue = "true", matchIfMissing = true)
class SseConnectionManager(
    private val redisTemplate: StringRedisTemplate,
    private val objectMapper: ObjectMapper,
    private val properties: NotificationProperties,
    meterRegistry: MeterRegistry
) : RealtimeNotificationPort {

    private val log = LoggerFactory.getLogger(SseConnectionManager::class.java)

    /** userId → list of SSE sinks (supports multi-tab). */
    private val connections = ConcurrentHashMap<Long, CopyOnWriteArrayList<Sinks.Many<ServerSentEvent<String>>>>()
    private val activeConnectionsCount = AtomicLong(0)

    companion object {
        private const val CHANNEL_PREFIX = "notification:inbox:"
    }

    init {
        meterRegistry.gauge("sse.connections.active", activeConnectionsCount) { it.toDouble() }
    }

    override fun subscribe(userId: Long): Flux<ServerSentEvent<String>> {
        val sink = Sinks.many().multicast().onBackpressureBuffer<ServerSentEvent<String>>()

        connections.computeIfAbsent(userId) { CopyOnWriteArrayList() }.add(sink)
        activeConnectionsCount.incrementAndGet()
        log.info("SSE connected: userId={}, totalConnections={}", userId, activeConnectionsCount.get())

        // Heartbeat flux — :ping comment every N seconds
        val heartbeatInterval = Duration.ofSeconds(properties.inApp.sse.heartbeatIntervalSeconds)
        val heartbeat = Flux.interval(heartbeatInterval)
            .map { ServerSentEvent.builder<String>().comment("ping").build() }

        // Merge data events + heartbeat, with timeout
        val timeout = Duration.ofMinutes(properties.inApp.sse.timeoutMinutes)
        return Flux.merge(sink.asFlux(), heartbeat)
            .timeout(timeout)
            .doOnCancel { removeSink(userId, sink) }
            .doOnTerminate { removeSink(userId, sink) }
            .doOnError { e -> log.debug("SSE stream error: userId={}, error={}", userId, e.message) }
    }

    override fun pushToUser(userId: Long, event: InboxNotificationEvent) {
        val json = objectMapper.writeValueAsString(event)
        val channel = "$CHANNEL_PREFIX$userId"
        redisTemplate.convertAndSend(channel, json)
        log.debug("Published SSE event to Redis: channel={}, eventType={}", channel, event.type)
    }

    override fun disconnect(userId: Long) {
        val sinks = connections.remove(userId)
        sinks?.forEach { sink ->
            sink.tryEmitComplete()
            activeConnectionsCount.decrementAndGet()
        }
        log.info("SSE disconnected: userId={}, closedSinks={}", userId, sinks?.size ?: 0)
    }

    override fun getActiveConnections(): Long = activeConnectionsCount.get()

    /**
     * Called by RedisInboxConfig MessageListener when a message arrives on the Pub/Sub channel.
     * Routes the event to local SSE sinks for the target user.
     */
    fun onRedisMessage(userId: Long, eventJson: String) {
        val sinks = connections[userId] ?: return

        try {
            val event = objectMapper.readValue(eventJson, InboxNotificationEvent::class.java)
            val sse = ServerSentEvent.builder(eventJson)
                .event(event.type.name.lowercase())
                .id(event.eventId)
                .build()

            sinks.forEach { sink ->
                val result = sink.tryEmitNext(sse)
                if (result.isFailure) {
                    log.warn("Failed to push SSE event: userId={}, result={}", userId, result)
                }
            }
        } catch (e: Exception) {
            log.error("Failed to process Redis SSE message: userId={}", userId, e)
        }
    }

    private fun removeSink(userId: Long, sink: Sinks.Many<ServerSentEvent<String>>) {
        val sinks = connections[userId] ?: return
        if (sinks.remove(sink)) {
            activeConnectionsCount.decrementAndGet()
            if (sinks.isEmpty()) {
                connections.remove(userId)
            }
            log.debug("SSE sink removed: userId={}, remaining={}", userId, sinks.size)
        }
    }

    @PreDestroy
    fun shutdown() {
        log.info("Shutting down SSE connections: total={}", activeConnectionsCount.get())
        connections.forEach { (userId, sinks) ->
            sinks.forEach { it.tryEmitComplete() }
        }
        connections.clear()
        activeConnectionsCount.set(0)
    }
}
