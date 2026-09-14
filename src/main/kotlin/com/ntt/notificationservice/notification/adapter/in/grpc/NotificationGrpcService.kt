package com.ntt.notificationservice.notification.adapter.`in`.grpc

import com.ntt.notificationservice.grpc.v1.EnqueueRequest
import com.ntt.notificationservice.grpc.v1.EnqueueResponse
import com.ntt.notificationservice.grpc.v1.NotificationServiceGrpc
import com.ntt.notificationservice.notification.application.NotificationEnqueueService
import com.ntt.notificationservice.notification.domain.model.NotificationChannel
import com.ntt.notificationservice.notification.domain.model.NotificationPriority
import com.ntt.notificationservice.shared.exception.NotificationErrorCode
import com.ntt.notificationservice.shared.exception.NotificationException
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

/**
 * gRPC server for notification enqueue — optional transport.
 * Only active when app.notification.grpc.enabled = true.
 */
@GrpcService
@ConditionalOnProperty(
    prefix = "app.notification.grpc",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class NotificationGrpcService(
    private val enqueueService: NotificationEnqueueService
) : NotificationServiceGrpc.NotificationServiceImplBase() {

    private val log = LoggerFactory.getLogger(NotificationGrpcService::class.java)

    override fun enqueue(request: EnqueueRequest, responseObserver: StreamObserver<EnqueueResponse>) {
        try {
            validateRequest(request)

            val channel = NotificationChannel.valueOf(request.channel.uppercase())
            val priority = if (request.priority.isNotBlank())
                NotificationPriority.valueOf(request.priority.uppercase())
            else NotificationPriority.NORMAL

            val templateData: Map<String, Any> = if (request.templateDataJson.isNotBlank()) {
                val mapper = com.fasterxml.jackson.databind.ObjectMapper()
                mapper.readValue(request.templateDataJson, Map::class.java) as Map<String, Any>
            } else emptyMap()

            val id = enqueueService.enqueue(
                recipient = request.recipient,
                templateCode = request.templateCode,
                templateData = templateData,
                channel = channel,
                priority = priority,
                correlationId = request.correlationId.ifBlank { null },
                sourceService = request.sourceService.ifBlank { null },
                subChannel = request.subChannel.ifBlank { null }
            )

            val response = EnqueueResponse.newBuilder()
                .setId(id)
                .setStatus("PENDING")
                .build()

            responseObserver.onNext(response)
            responseObserver.onCompleted()

            log.info("gRPC enqueue: id={}, channel={}, recipient={}", id, channel, request.recipient)

        } catch (e: NotificationException) {
            responseObserver.onError(
                io.grpc.Status.INVALID_ARGUMENT
                    .withDescription(e.message)
                    .asRuntimeException()
            )
        } catch (e: Exception) {
            log.error("gRPC enqueue failed", e)
            responseObserver.onError(
                io.grpc.Status.INTERNAL
                    .withDescription("Internal error: ${e.message}")
                    .asRuntimeException()
            )
        }
    }

    private fun validateRequest(request: EnqueueRequest) {
        if (request.recipient.isBlank()) {
            throw NotificationException(NotificationErrorCode.GRPC_VALIDATION_ERROR, "recipient is required")
        }
        if (request.templateCode.isBlank()) {
            throw NotificationException(NotificationErrorCode.GRPC_VALIDATION_ERROR, "template_code is required")
        }
        if (request.channel.isBlank()) {
            throw NotificationException(NotificationErrorCode.GRPC_VALIDATION_ERROR, "channel is required")
        }
    }
}
