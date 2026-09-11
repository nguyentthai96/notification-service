# Error Pattern — notification-service

## Exception Hierarchy

```
java.lang.RuntimeException
└── com.ntt.basecore.exception.BusinessException (base-core)
    └── com.ntt.notificationservice.shared.exception.NotificationException
        ├── errorCode: NotificationErrorCode
        ├── message: String
        └── httpStatus: HttpStatus
```

**File**: `shared/exception/NotificationException.kt`

## Error Codes

| Code | Enum | HTTP Status | Description |
|------|------|-------------|-------------|
| `NOTIF-001` | `ENQUEUE_FAILED` | 500 | Failed to enqueue notification |
| `NOTIF-002` | `DUPLICATE` | 409 | Duplicate correlation_id |
| `NOTIF-004` | `TEMPLATE_NOT_FOUND` | 404 | Template code not found or inactive |
| `NOTIF-005` | `CHANNEL_NOT_SUPPORTED` | 400 | Channel type not supported |
| `NOTIF-013` | `INVALID_STATUS_TRANSITION` | 409 | Invalid status transition |
| `NOTIF-014` | `NOT_FOUND` | 404 | Notification not found |
| `NOTIF-022` | `INVALID_REQUEST` | 400 | Invalid notification request |

**File**: `shared/exception/NotificationErrorCode.kt`

## Error Response Format

RFC 7807 ProblemDetail (via `BaseControllerAdvice` + `GlobalExceptionHandler`):

```json
{
  "type": "about:blank",
  "title": "CHANNEL_NOT_SUPPORTED",
  "status": 400,
  "detail": "Channel not supported: WHATSAPP",
  "instance": "/api/v1/notifications",
  "errorCode": "NOTIF-005"
}
```

**File**: `shared/exception/GlobalExceptionHandler.kt`

## Error Handling Patterns

| Pattern | Implementation | File |
|---------|---------------|------|
| Global exception handler | `@ControllerAdvice` extending `BaseControllerAdvice` | `GlobalExceptionHandler.kt` |
| Domain exception | `NotificationException` extending `BusinessException` | `NotificationException.kt` |
| Error code bridge | `toErrorCodeBase()` → base-core `ErrorCodeBase` | `NotificationErrorCode.kt` |
| Controller validation | `@Valid` + Bean Validation → auto 400 response | `NotificationController.kt` |
| Circuit breaker | Resilience4j `circuitBreaker.executeRunnable{}` | `SmtpEmailSender.kt` |
| Retry failure | Exponential backoff → FAILED after max retries | `NotificationJobScheduler.kt` |

## Phase 2 — New Error Codes Required

| Code | Enum | HTTP Status | Description |
|------|------|-------------|-------------|
| `NOTIF-006` | `SMS_DELIVERY_FAILED` | 502 | Twilio SMS delivery failed |
| `NOTIF-007` | `PUSH_DELIVERY_FAILED` | 502 | FCM push delivery failed |
| `NOTIF-008` | `OTT_DELIVERY_FAILED` | 502 | OTT message delivery failed |
| `NOTIF-009` | `INVALID_PHONE_NUMBER` | 400 | Phone number not in E.164 format |
| `NOTIF-010` | `INVALID_DEVICE_TOKEN` | 400 | FCM device token invalid/expired |
| `NOTIF-011` | `DLQ_ENTRY_NOT_FOUND` | 404 | DLQ entry not found |
| `NOTIF-012` | `RATE_LIMIT_EXCEEDED` | 429 | Rate limit exceeded for channel |
| `NOTIF-015` | `KAFKA_DESERIALIZATION_ERROR` | 400 | Invalid Kafka message format |
| `NOTIF-016` | `GRPC_VALIDATION_ERROR` | 400 | Invalid gRPC request |
| `NOTIF-017` | `PROVIDER_UNAVAILABLE` | 503 | External provider unavailable |
