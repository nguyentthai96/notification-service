package com.ntt.notificationservice.notification.adapter.`in`.kafka

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ntt.notificationservice.notification.adapter.`in`.web.dto.EnqueueNotificationRequest
import org.slf4j.LoggerFactory

/**
 * Smart deserializer — detects format from content-type header.
 * Supports JSON (default) and Protobuf (future).
 */
class SmartNotificationDeserializer(
    private val objectMapper: ObjectMapper = ObjectMapper().apply {
        findAndRegisterModules()
    }
) {

    private val log = LoggerFactory.getLogger(SmartNotificationDeserializer::class.java)

    /**
     * Deserialize notification payload.
     * @param data raw bytes from Kafka
     * @param contentType content-type header value (null defaults to JSON)
     */
    fun deserialize(data: ByteArray, contentType: String?): EnqueueNotificationRequest {
        return when {
            contentType == null || contentType.contains("json", ignoreCase = true) -> {
                objectMapper.readValue<EnqueueNotificationRequest>(data)
            }
            contentType.contains("protobuf", ignoreCase = true) -> {
                // Protobuf deserialization — parse from proto bytes
                val protoRequest = com.ntt.notificationservice.grpc.v1.EnqueueRequest.parseFrom(data)
                EnqueueNotificationRequest(
                    recipient = protoRequest.recipient,
                    templateCode = protoRequest.templateCode,
                    templateData = if (protoRequest.templateDataJson.isNotBlank()) {
                        objectMapper.readValue<Map<String, Any>>(protoRequest.templateDataJson)
                    } else emptyMap(),
                    channel = protoRequest.channel,
                    priority = protoRequest.priority.ifBlank { "NORMAL" },
                    correlationId = protoRequest.correlationId.ifBlank { null },
                    sourceService = protoRequest.sourceService.ifBlank { null },
                    subChannel = protoRequest.subChannel.ifBlank { null }
                )
            }
            else -> {
                log.warn("Unknown content-type: {}, defaulting to JSON", contentType)
                objectMapper.readValue<EnqueueNotificationRequest>(data)
            }
        }
    }
}
