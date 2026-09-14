package com.ntt.notificationservice.notification.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Configuration

/**
 * gRPC server configuration — only active when grpc.enabled = true.
 * gRPC reflection is enabled via application-dev.yml (dev profile only).
 */
@Configuration
@ConditionalOnProperty(
    prefix = "app.notification.grpc",
    name = ["enabled"],
    havingValue = "true",
    matchIfMissing = false
)
class GrpcConfig
