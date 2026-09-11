---
type: brainstorm_notes
change: notification-service-phase2
date: 2026-09-11
selected_direction: "Incremental Extension — Strategy + Multi-Transport + Provider Abstraction"
pre_flow: "Command"
pre_feature_type: "EXTEND"
status: complete
---

# Brainstorm Notes: Notification Service Phase 2

## Date
2026-09-11

## Context
Mở rộng notification-service Phase 1 (EMAIL-only, REST + outbox polling) thành multi-channel, multi-transport platform. User yêu cầu: SMS, Push, OTT, gRPC, Kafka, retry management, reporting, read tracking.

## Questions Asked & Answers

- Q1: Zalo ZBS migration (01/2026) — API có thay đổi breaking changes không?
  → A: **DEFER** — chưa cần triển khai Zalo OTT trong Phase 2. Chỉ thiết kế interface cho tương lai.

- Q2: Kafka serialization — JSON ban đầu hay Protobuf từ đầu?
  → A: **Hỗ trợ cả 2** — dual serialization (JSON + Protobuf). Consumer phải auto-detect format.

- Q3: Device token management — Ai quản lý token registration?
  → A: **Admin quản lý** — Admin CRUD API cho device tokens, không phải client tự đăng ký.

## Approaches Considered

### Approach 1: Incremental Extension (SELECTED ✅)

Mở rộng trực tiếp trên kiến trúc Phase 1 — thêm sender beans + transport adapters.

```
                        ┌─────────────────────────────────────────────────────┐
                        │             NOTIFICATION SERVICE                    │
                        │                                                     │
  ┌──────────┐          │  ┌───────────┐   ┌──────────┐   ┌───────────────┐  │
  │ REST API │─────────▶│  │ Enqueue   │──▶│  Queue   │──▶│ JobScheduler  │  │
  └──────────┘          │  │ Service   │   │  (DB)    │   │ (poll+lock)   │  │
  ┌──────────┐          │  └───────────┘   └──────────┘   └──────┬────────┘  │
  │ gRPC     │─────────▶│        ▲                               │           │
  └──────────┘          │        │                        ┌──────▼────────┐  │
  ┌──────────┐          │  ┌─────┴─────┐                  │  Dispatcher   │  │
  │ Kafka    │─────────▶│  │ Kafka     │                  │  (Strategy)   │  │
  │ Consumer │          │  │ Consumer  │                  └──────┬────────┘  │
  └──────────┘          │  └───────────┘                         │           │
                        │                    ┌───────────┬───────┼─────┐     │
                        │                    ▼           ▼       ▼     ▼     │
                        │               ┌────────┐ ┌─────┐ ┌────┐ ┌─────┐  │
                        │               │ SMTP   │ │ SMS │ │FCM │ │ OTT │  │
                        │               │ Email  │ │Twil.│ │Push│ │Tele.│  │
                        │               └────────┘ └─────┘ └────┘ └─────┘  │
                        │                                                     │
                        │  ┌───────────────────────────────────────────────┐  │
                        │  │ Tracking: Webhooks → Status Update → Metrics  │  │
                        │  └───────────────────────────────────────────────┘  │
                        └─────────────────────────────────────────────────────┘
```

- **Pros:**
  - Zero change ở core dispatcher/scheduler — chỉ thêm beans
  - Từng channel có thể deploy/test độc lập (`@ConditionalOnProperty`)
  - Backward compatible hoàn toàn
  - Risk thấp — mỗi sprint thêm 1 channel

- **Cons:**
  - Dispatcher chỉ route theo `NotificationChannel` enum — OTT cần sub-routing
  - Cần giải quyết OTT sub-channel routing

### Approach 2: Event-Driven Full Rewrite

Chuyển toàn bộ sang Kafka-centric architecture — mọi notification đi qua Kafka topics.

```
  Producer → Kafka[notification-inbound] → Consumer → Process → Kafka[channel-email/sms/push] → Channel Worker
```

- **Pros:**
  - Scalable, resilient, built-in retry/DLQ
  - Uniform architecture

- **Cons:**
  - ❌ Breaking change — Phase 1 outbox pattern bị thay thế
  - ❌ Kafka dependency trở thành mandatory (hiện tại optional)
  - ❌ Overengineering cho current scale
  - ❌ Mất transactional guarantee của outbox pattern

### Approach 3: Hybrid với Spring Integration

Sử dụng Spring Integration channels/handlers thay cho custom dispatcher.

- **Pros:** Built-in channel routing, wire tap, error channel
- **Cons:**
  - ❌ Thêm abstraction layer không cần thiết
  - ❌ Phase 1 code cần refactor lớn
  - ❌ Spring Integration DSL learning curve

## Selected Direction

**Approach 1: Incremental Extension** — vì:
1. Phase 1 architecture đã thiết kế đúng cho extensibility
2. `NotificationDispatcher.senderMap` auto-discovers beans — ZERO change
3. `@ConditionalOnProperty` cho phép enable/disable channels runtime
4. Outbox pattern giữ transactional guarantee
5. Kafka/gRPC là transport mới, không thay thế outbox

## Key Design Decisions (DD)

### DD-001: OTT Sub-Channel Routing

**Problem:** `NotificationDispatcher` route theo `NotificationChannel` enum. OTT có nhiều sub-channels (Zalo, Telegram). 1 enum value `OTT` → cần route tới đúng sender.

**Decision:** Composite OTT Dispatcher — tạo `OttDispatcher` implement `NotificationSender(channel = OTT)`, bên trong route theo `sub_channel` field.

```
NotificationDispatcher
  └─▶ senderMap[OTT] = OttDispatcher
        └─▶ subSenderMap["ZALO"] = ZaloOttSender
        └─▶ subSenderMap["TELEGRAM"] = TelegramOttSender
```

**Why:** Giữ nguyên Dispatcher interface, không modify core code. OttDispatcher chỉ là 1 NotificationSender như mọi sender khác.

### DD-002: Kafka Dual Serialization (JSON + Protobuf)

**Problem:** User yêu cầu hỗ trợ cả JSON và Protobuf trên Kafka.

**Decision:** Content-type based deserialization. Kafka header `content-type` xác định format:

```
Header: content-type = "application/json"    → JsonDeserializer
Header: content-type = "application/protobuf" → ProtobufDeserializer
```

**Implementation:**
```kotlin
// Custom deserializer checks header, delegates to appropriate deserializer
class SmartNotificationDeserializer : Deserializer<NotificationEvent> {
    override fun deserialize(topic: String, headers: Headers, data: ByteArray): NotificationEvent {
        val contentType = headers.lastHeader("content-type")?.value()?.let { String(it) }
        return when (contentType) {
            "application/protobuf" -> protobufDeserializer.deserialize(topic, data)
            else -> jsonDeserializer.deserialize(topic, data)  // default: JSON
        }
    }
}
```

**Why:** Backward compatible — existing JSON producers continue working. New services can adopt Protobuf. Default fallback = JSON.

### DD-003: gRPC Proto + Client SDK Distribution

**Problem:** gRPC cần `.proto` file shared giữa server và clients. Cách distribute?

**Decision:** Proto file trong `notification-client` module, publish lên mavenLocal (user preference từ Phase 1).

```
notification-client/
├── src/main/proto/
│   └── ntt/notification/v1/notification_service.proto
├── src/main/kotlin/
│   ├── NotificationPort.kt          # (existing)
│   ├── JpaNotificationAdapter.kt    # (existing)
│   ├── GrpcNotificationAdapter.kt   # (NEW)
│   └── KafkaNotificationAdapter.kt  # (NEW)
```

**Why:** Single source of truth cho contract. Consumer services depend on `notification-client` → auto-get proto stubs.

### DD-004: Device Token Admin Management

**Problem:** Admin quản lý device tokens (user decision). Flow?

**Decision:**
```
Admin API:
  POST   /api/v1/admin/device-tokens       → register token
  GET    /api/v1/admin/device-tokens/{userId} → list tokens by user
  DELETE /api/v1/admin/device-tokens/{id}   → deactivate token
  PUT    /api/v1/admin/device-tokens/{id}   → update (platform, active status)
```

FCM sender queries `DeviceTokenRepository.findByUserIdAndActiveTrue(userId)` để lấy all active tokens, gửi multi-device.

**Why:** Centralized management, admin kiểm soát token lifecycle.

### DD-005: Zalo Deferred — Interface Design Only

**Problem:** User defer Zalo OTT implementation.

**Decision:** 
- `ZaloOttSender` class → **NOT created** trong Phase 2
- `OttDispatcher` sub-sender map → chỉ register `TelegramOttSender`
- Config `app.notification.channels.ott.enabled: false` → OTT channel disabled
- Sub-channel routing logic ready cho khi add Zalo sau

**Impact on FRs:**
- FR-003 (Zalo OTT) → **DEFERRED** (removed from Phase 2 scope)
- FR-011 (Zalo Status Polling) → **DEFERRED**
- FR-003/011 vẫn giữ trong pre_openspec.md nhưng tagged `[DEFERRED]`

### DD-006: DLQ + Retry Log Architecture

**Problem:** Cần visibility vào failed notifications + retry history.

**Decision:** Two-table approach:

```
notification_queue (existing)
  │ max retries exceeded
  ▼
notification_dlq (NEW)
  ├── notification_id (FK → queue)
  ├── error_code, error_message
  ├── resolved (boolean) → admin marks resolved
  └── resolved_by, resolved_at

notification_retry_log (NEW)
  ├── notification_id (FK → queue)
  ├── attempt_number
  ├── error_code, error_message
  └── attempted_at
```

**Integration with JobScheduler:**
```kotlin
// In handleFailure():
retryLogRepository.save(RetryLogEntity(notificationId, retryCount, errorCode, errorMsg))

if (retryCount >= maxRetries) {
    dlqRepository.save(DlqEntity(notificationId, channel, errorCode, errorMsg))
    notification.status = FAILED
}
```

**Why:** 
- `notification_dlq` = actionable queue for admin (retry/discard)
- `notification_retry_log` = audit trail for every attempt
- Both queryable via SQL/API for reporting

### DD-007: Statistics API Design

**Problem:** Reporting endpoints cho delivery statistics.

**Decision:** Native SQL aggregate queries (performant, no ORM overhead):

```sql
-- Per-channel breakdown
SELECT channel, status, COUNT(*) as count
FROM notification_queue
WHERE created_at BETWEEN :from AND :to
GROUP BY channel, status;

-- Delivery latency (P50/P95/P99)
SELECT channel,
  PERCENTILE_CONT(0.50) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (sent_at - created_at))) as p50,
  PERCENTILE_CONT(0.95) WITHIN GROUP (ORDER BY EXTRACT(EPOCH FROM (sent_at - created_at))) as p95
FROM notification_queue
WHERE sent_at IS NOT NULL AND created_at BETWEEN :from AND :to
GROUP BY channel;
```

**Endpoints:**
```
GET /api/v1/notifications/stats?from=&to=&channel=    → aggregate stats
GET /api/v1/notifications/stats/channels              → per-channel breakdown
GET /api/v1/notifications/stats/retry                 → retry failure report
GET /api/v1/notifications/dlq                         → DLQ entries (paginated)
POST /api/v1/notifications/dlq/{id}/retry             → manual retry
POST /api/v1/notifications/dlq/{id}/discard           → discard entry
```

### DD-008: Webhook Security

**Problem:** Webhook endpoints nhận callbacks từ Twilio/Telegram. Cần verify authenticity.

**Decision:** Per-provider verification:
- **Twilio:** Validate `X-Twilio-Signature` header (HMAC-SHA1 of URL + params with Auth Token)
- **Telegram:** Verify secret_token header (set via `setWebhook(secret_token=...)`)
- **Common:** Rate limiting trên webhook endpoints

### DD-009: Scope Adjustment (User-Driven)

**Updated FR scope for Phase 2:**

| FR | Status | Notes |
|----|--------|-------|
| FR-001 SMS (Twilio) | ✅ IN SCOPE | Phase 2a |
| FR-002 Push (FCM) | ✅ IN SCOPE | Phase 2b |
| FR-003 Zalo OTT | ❌ DEFERRED | User decision: chưa cần |
| FR-004 Telegram OTT | ✅ IN SCOPE | Phase 2c (as only OTT initially) |
| FR-005 gRPC Server | ✅ IN SCOPE | Phase 2a |
| FR-006 Kafka Consumer | ✅ IN SCOPE | Phase 2a |
| FR-007 Kafka DLT | ✅ IN SCOPE | Phase 2a |
| FR-008 gRPC Client | ✅ IN SCOPE | Phase 2a |
| FR-009 Kafka Producer | ✅ IN SCOPE | Phase 2a |
| FR-010 Twilio Webhook | ✅ IN SCOPE | Phase 2b |
| FR-011 Zalo Polling | ❌ DEFERRED | Follows FR-003 |
| FR-012 Delivery Tracking | ✅ IN SCOPE | Phase 2b |
| FR-013 DLQ | ✅ IN SCOPE | Phase 2d |
| FR-014 Retry Log | ✅ IN SCOPE | Phase 2d |
| FR-015 Manual Retry | ✅ IN SCOPE | Phase 2d |
| FR-016 Statistics | ✅ IN SCOPE | Phase 2d |
| FR-017 Retry Report | ✅ IN SCOPE | Phase 2d |
| FR-018 Sub-channel | ✅ IN SCOPE | Phase 2c |
| FR-019 Device Tokens | ✅ IN SCOPE | Phase 2b (admin API) |
| FR-020 External Msg ID | ✅ IN SCOPE | Phase 2b |
| FR-021 Idempotency | ✅ IN SCOPE | Phase 2a |
| FR-022 Rate Limiting | ✅ IN SCOPE | Phase 2a |
| FR-023 Circuit Breaker | ✅ IN SCOPE | Phase 2a-c |

**Summary:** 21/23 FRs IN SCOPE, 2 DEFERRED (Zalo-related)

## Revised Implementation Phases

```
Phase 2a (Sprint 1-2): Foundation + SMS + Transports
  ├── TwilioSmsSender + TwilioSmsProperties
  ├── KafkaNotificationConsumer (dual JSON+Protobuf)
  ├── NotificationGrpcService + .proto
  ├── notification-client v2 (gRPC + Kafka adapters)
  ├── Rate limiting service
  ├── Kafka idempotency check (source_id + source_service)
  └── DB migration: V4 (sub_channel, external_message_id columns)

Phase 2b (Sprint 3): Push + Tracking
  ├── FcmPushSender + FirebaseConfig
  ├── DeviceTokenEntity + admin CRUD API
  ├── TwilioWebhookController (delivery callback)
  ├── Delivery tracking status updates
  └── DB migration: V5 (device_token table)

Phase 2c (Sprint 4): OTT (Telegram only) + Sub-routing
  ├── OttDispatcher (composite sender)
  ├── TelegramOttSender + TelegramProperties
  ├── TelegramWebhookController
  ├── Sub-channel routing logic
  └── DB migration: V6 (sub_channel index)

Phase 2d (Sprint 5): Reporting + DLQ
  ├── NotificationDlqEntity + repository
  ├── NotificationRetryLogEntity + repository
  ├── DlqController (admin: list, retry, discard)
  ├── StatisticsController + StatisticsService
  ├── Prometheus custom metrics enhancement
  └── DB migration: V7 (dlq, retry_log tables)
```

## Open Questions for Design Phase

- [RESOLVED] OQ-001: Zalo → DEFERRED
- [RESOLVED] OQ-002: Kafka serialization → dual JSON + Protobuf
- [RESOLVED] OQ-003: Device tokens → admin quản lý
- [RESOLVED] DD-010: Telegram bot → **Webhook cho production, long polling cho dev** (profile-based config)
- [RESOLVED] DD-011: gRPC reflection → **Enable cho dev profile only**, disable cho production
- [RESOLVED] DD-012: Kafka → **3 partitions, configurable + Kafka OPTIONAL (plug-and-play)** — hệ thống không dựng Kafka vẫn chạy được
## DD-012 Detail: Kafka Optional (Plug-and-Play)

> **Quan trọng:** Một số hệ thống sẽ không dựng Kafka. Tất cả Kafka beans phải optional.

### Implementation Strategy

```
@ConditionalOnProperty("app.notification.kafka.enabled", havingValue = "true", matchIfMissing = false)
```

Áp dụng cho:
- `KafkaNotificationConsumer` — @ConditionalOnProperty
- `KafkaNotificationAdapter` (client SDK) — @ConditionalOnProperty
- `SmartNotificationDeserializer` — chỉ load khi Kafka enabled
- Kafka auto-configuration — exclude khi disabled

### Config Pattern

```yaml
# Hệ thống CÓ Kafka:
app:
  notification:
    kafka:
      enabled: true
      bootstrap-servers: kafka:9092
      consumer:
        group-id: notification-service-group
        concurrency: 3

# Hệ thống KHÔNG CÓ Kafka (default):
app:
  notification:
    kafka:
      enabled: false   # hoặc không khai báo → matchIfMissing = false
```

### Spring Boot Auto-Config Exclusion

```kotlin
// NotificationServiceApplication.kt — khi kafka.enabled = false
@SpringBootApplication(
    exclude = [KafkaAutoConfiguration::class]  // exclude khi không cần
)
// HOẶC dùng @ConditionalOnProperty trên @Configuration class
```

### Gradle Dependency

```kotlin
// Kafka là optional dependency
implementation("org.springframework.kafka:spring-kafka") {
    // Có sẵn trong classpath nhưng auto-config bị exclude khi disabled
}
```

### Same Pattern cho gRPC

```
@ConditionalOnProperty("app.notification.grpc.enabled", havingValue = "true", matchIfMissing = false)
```

→ gRPC server cũng optional, chỉ start khi enabled.
