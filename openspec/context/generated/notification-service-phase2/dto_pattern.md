# DTO Pattern — notification-service

## Existing DTOs

### Request DTOs

| DTO | File | Annotations | Fields |
|-----|------|-------------|--------|
| `EnqueueNotificationRequest` | `adapter/in/web/dto/NotificationDtos.kt` | `@NotBlank`, `@Size` | recipient, templateCode, templateData, channel, priority, correlationId, sourceService, createdBy |
| `RevokeRequest` | `adapter/in/web/dto/NotificationDtos.kt` | `@Size(max=500)` | reason |

### Response DTOs

| DTO | File | Fields |
|-----|------|--------|
| `EnqueueNotificationResponse` | `adapter/in/web/dto/NotificationDtos.kt` | id, status="PENDING" |
| `NotificationStatusResponse` | `adapter/in/web/dto/NotificationDtos.kt` | id, correlationId, channel, priority, recipient, templateCode, status, retryCount, maxRetries, errorMessage, sentAt, deliveredAt, readAt, revokedAt, revokeReason, sourceService, createdAt |

## DTO Conventions

| Convention | Pattern | Example |
|-----------|---------|---------|
| Request naming | `<Action><Entity>Request` | `EnqueueNotificationRequest` |
| Response naming | `<Entity><Action>Response` or `<Action><Entity>Response` | `EnqueueNotificationResponse` |
| File location | `adapter/in/web/dto/` | All DTOs in single file (`NotificationDtos.kt`) |
| Validation | Jakarta Bean Validation annotations | `@NotBlank`, `@Size` |
| Format | Kotlin `data class` | Immutable by default |
| Default values | Provided where sensible | `channel = "EMAIL"`, `priority = "NORMAL"` |
| Enum handling | String in DTO → enum conversion in controller | `channel: String` → `NotificationChannel.valueOf()` |

## Phase 2 — New DTOs Required

### New Request DTOs
| DTO | Purpose |
|-----|---------|
| `KafkaNotificationEvent` | Kafka inbound message (deserialized from JSON/Protobuf) |
| `TwilioWebhookPayload` | Twilio SMS delivery callback |
| `ZaloWebhookEvent` | Zalo OA event callback |
| `TelegramUpdate` | Telegram Bot update webhook |
| `RegisterDeviceTokenRequest` | Register FCM device token |
| `DlqRetryRequest` | Manual retry DLQ entry |

### New Response DTOs
| DTO | Purpose |
|-----|---------|
| `StatisticsResponse` | Delivery statistics per channel |
| `ChannelBreakdownResponse` | Per-channel breakdown |
| `DlqEntryResponse` | Dead letter queue entry detail |
| `RetryReportResponse` | Retry failures report |
| `DeviceTokenResponse` | Registered device token |
