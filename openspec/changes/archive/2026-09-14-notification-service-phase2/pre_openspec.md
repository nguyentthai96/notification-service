# Pre-OpenSpec: Notification Service Phase 2

> **Type**: EXTEND
> **Source**: URD (research/notification-service-phase2/business_analysis.md)
> **Feature**: notification-service-phase2
> **Flow**: Command
> **Mode**: FULL
> **Archive**: N/A (fresh)
> **Classification Evidence**: `NotificationSender` interface exists (`NotificationSender.kt`), `NotificationChannel` enum đã có SMS/PUSH/OTT values → extending existing service

---

## 1. Tổng quan

Mở rộng notification-service từ hệ thống EMAIL-only thành multi-channel (SMS, Push, OTT), multi-transport (REST, gRPC, Kafka) notification platform. Bao gồm delivery tracking, retry management nâng cao, và reporting API.

## 2. Actors

| Actor | Mô tả |
|-------|-------|
| System (Producer Services) | Các microservice gửi notification request (auth-service, order-service...) |
| NotificationJobScheduler | Job nội bộ poll và dispatch notifications |
| External API (Twilio, FCM, Zalo, Telegram) | Third-party delivery services |
| Webhook Callback | Third-party gọi lại báo delivery status |
| Admin | Quản lý DLQ, retry, statistics |

## 3. Functional Requirements

### Channel Senders

- **FR-001** [URD] — Gửi SMS qua Twilio: Hệ thống phải gửi SMS qua Twilio API khi channel = SMS và recipient là số điện thoại E.164.
- **FR-002** [URD] — Gửi Push qua FCM: Hệ thống phải gửi push notification qua Firebase Cloud Messaging khi channel = PUSH.
- **FR-003** [DEFERRED] — ~~Gửi Zalo OTT~~ → Deferred: chưa cần triển khai.
- **FR-004** [URD] — Gửi Telegram OTT: Hệ thống phải gửi message qua Telegram Bot API khi channel = OTT và sub_channel = TELEGRAM.

### Transport Layer

- **FR-005** [URD] — gRPC Server: Hệ thống phải expose gRPC API cho phép inter-service enqueue notifications.
- **FR-006** [URD] — Kafka Consumer: Hệ thống phải consume notification events từ Kafka topic `notification-inbound`.
- **FR-007** [URD] — Kafka DLT: Hệ thống phải route invalid/failed Kafka messages tới Dead Letter Topic.

### Client SDK Enhancement

- **FR-008** [URD] — gRPC Client Adapter: notification-client SDK phải cung cấp gRPC adapter cho producer services.
- **FR-009** [URD] — Kafka Producer Adapter: notification-client SDK phải cung cấp KafkaTemplate adapter.

### Delivery Tracking

- **FR-010** [URD] — Twilio Webhook: Hệ thống phải nhận SMS delivery callbacks từ Twilio.
- **FR-011** [DEFERRED] — ~~Zalo Status Polling~~ → Deferred: follows FR-003.
- **FR-012** [URD] — Delivered/Read Timestamp: Hệ thống phải ghi delivered_at và read_at cho mỗi notification.

### Retry Management

- **FR-013** [URD] — DLQ Table: Hệ thống phải lưu notifications failed sau max retries vào `notification_dlq`.
- **FR-014** [URD] — Retry Log: Hệ thống phải log mỗi retry attempt vào `notification_retry_log`.
- **FR-015** [URD] — Manual Retry: Admin phải có thể retry hoặc discard DLQ entries qua API.

### Reporting

- **FR-016** [URD] — Statistics API: Hệ thống phải cung cấp API thống kê delivery per channel, per time range.
- **FR-017** [URD] — Retry Report: Hệ thống phải cung cấp API report retry failures.

### Data Model

- **FR-018** [URD] — Sub-channel Column: Thêm sub_channel vào notification_queue cho OTT routing.
- **FR-019** [URD] — Device Token Table: Tạo notification_device_token để quản lý FCM device tokens.
- **FR-020** [URD] — External Message ID: Lưu external message ID từ third-party (Twilio SID, FCM message_id).

### Domain Enhancement

- **FR-021** [ENRICHED] — Idempotency Check: Kafka consumer phải dedup bằng source_id + source_service.
- **FR-022** [ENRICHED] — Rate Limiting: Max 1 SMS/user/template/giờ để tránh spam.
- **FR-023** [ENRICHED] — Circuit Breaker per Channel: Mỗi channel sender phải có Resilience4j CircuitBreaker riêng.

## 4. Open Questions

- ✅ **OQ-001 [RESOLVED]**: Zalo ZBS migration → **DEFERRED** — chưa cần triển khai trong Phase 2.
- ✅ **OQ-002 [RESOLVED]**: Kafka serialization → **Hỗ trợ cả JSON + Protobuf** (dual serialization).
- ✅ **OQ-003 [RESOLVED]**: Device token management → **Admin quản lý** (Admin CRUD API).

## 5. Issues & Risks

- 🟡 **ISS-001**: Telegram không hỗ trợ read receipt native (FR-012) → workaround inline buttons
- 🟡 **ISS-002**: Email read tracking bị block bởi nhiều email clients → pixel tracking optional
- 🟢 **ISS-003**: Kafka infrastructure cần setup riêng → có thể defer nếu chưa cần

## 6. Quality Score

| Tiêu chí | Điểm | Ghi chú |
|----------|------|---------|
| Rõ ràng (Clarity) | 23/25 | FR-003/004 cần rõ hơn sub_channel routing |
| Đầy đủ (Completeness) | 22/25 | Thiếu detailed Zalo OAuth2 token refresh flow |
| Nhất quán (Consistency) | 25/25 | Consistent với Phase 1 architecture |
| Kiểm thử được (Testability) | 23/25 | FR-011 Zalo polling khó test tự động |
| **Tổng** | **93/100** | |

## 7. Integrations

| System | Protocol | Direction |
|--------|----------|-----------|
| Twilio | REST (HTTPS) | Outbound (send SMS) + Inbound (webhook) |
| Firebase FCM | gRPC/REST (Admin SDK) | Outbound (send push) |
| Zalo OA | REST (HTTPS) + OAuth2 | Outbound (send + poll status) |
| Telegram | REST (HTTPS) | Outbound (send) + Inbound (webhook) |
| Kafka | Binary (TCP) | Inbound (consume) |
| Producer Services | gRPC / REST / JPA | Inbound (enqueue) |

## 8. Non-Functional Requirements

- NFR-001: SMS delivery latency < 5s (Twilio SLA)
- NFR-002: Push delivery latency < 2s (FCM SLA)
- NFR-003: gRPC deadline default 5s
- NFR-004: Kafka consumer group concurrency configurable
- NFR-005: Prometheus metrics per channel (success/failure/retry)

## 9. Dependencies (New)

| Dependency | Version | Purpose |
|-----------|---------|---------|
| `com.twilio.sdk:twilio` | 10.6.3 | SMS channel sender |
| `com.google.firebase:firebase-admin` | 9.3.0 | Push channel sender |
| `net.devh:grpc-server-spring-boot-starter` | 3.1.0 | gRPC server |
| `io.grpc:grpc-protobuf` | 1.68.0 | Protobuf serialization |
| `org.springframework.kafka:spring-kafka` | (managed) | Kafka consumer/producer |
| `spring-boot-starter-webflux` | (managed) | WebClient for OTT APIs |

## 10. DETECTED SCOPE

| Service | Path | Detection |
|---------|------|-----------|
| notification-service | `services/notification-service/` | PRIMARY — all changes here |

**Candidate Keyword Matches:**
- `NotificationSender` → Strategy interface (extension point)
- `NotificationChannel` → SMS, PUSH, OTT values (already in enum)
- `NotificationDispatcher` → Auto-discovers senders (zero-change on extension)
- `NotificationProperties` → Channel config (sms/push/ott: enabled: false)

## 11. Transaction Flow Detail

**Type: Command** — Single-step enqueue operations, no OTP/confirmation required.

| Flow | Steps |
|------|-------|
| REST Enqueue | POST request → validate → INSERT → return 201 |
| gRPC Enqueue | gRPC call → validate → INSERT → return response |
| Kafka Consume | consume event → validate → INSERT → commit offset |
| Webhook Receive | POST callback → find notification → UPDATE status |

## 12. Traceability Matrix

| FR | Source | Affected Classes |
|----|--------|-----------------|
| FR-001 | UC-01 (business_analysis) | [ADD] TwilioSmsSender, TwilioSmsProperties |
| FR-002 | UC-02 | [ADD] FcmPushSender, FirebaseConfig |
| FR-003 | UC-03 | [ADD] ZaloOttSender, ZaloProperties |
| FR-004 | UC-03 | [ADD] TelegramOttSender, TelegramProperties |
| FR-005 | UC-05 | [ADD] NotificationGrpcService, .proto file |
| FR-006 | UC-04 | [ADD] KafkaNotificationConsumer, KafkaConfig |
| FR-007 | UC-04 | [ADD] DLT handling in consumer |
| FR-008 | UC-05 | [ADD] GrpcNotificationAdapter (client SDK) |
| FR-009 | UC-04 | [ADD] KafkaNotificationAdapter (client SDK) |
| FR-010 | UC-06 | [ADD] TwilioWebhookController |
| FR-011 | UC-06 | [ADD] ZaloStatusPoller |
| FR-012 | UC-06 | [MODIFY] NotificationQueueEntity (add timestamps) |
| FR-013 | UC-08 | [ADD] NotificationDlqEntity, DlqRepository |
| FR-014 | UC-08 | [ADD] NotificationRetryLogEntity, RetryLogRepository |
| FR-015 | UC-08 | [ADD] DlqController |
| FR-016 | UC-07 | [ADD] StatisticsController, StatisticsService |
| FR-017 | UC-07 | [ADD] RetryReportService |
| FR-018 | UC-03 | [MODIFY] NotificationQueueEntity (add sub_channel) |
| FR-019 | UC-02 | [ADD] DeviceTokenEntity, DeviceTokenRepository |
| FR-020 | UC-06 | [MODIFY] NotificationQueueEntity (add external_message_id) |
| FR-021 | Enriched | [MODIFY] KafkaNotificationConsumer (dedup check) |
| FR-022 | Enriched | [ADD] RateLimitService |
| FR-023 | Enriched | [REUSE] Resilience4j (existing pattern in SmtpEmailSender) |

## 13. Agent Notes

### Observations
- Phase 1 architecture đã thiết kế đúng cho extensibility — Strategy Pattern + enum values cho Phase 2
- `NotificationDispatcher.senderMap` auto-discovers beans → thêm sender = thêm @Component
- `NotificationQueueEntity` đã có `delivered_at`, `read_at` columns → Phase 2 chỉ cần populate them
- `NotificationProperties.ChannelConfig` đã có `sms`/`push`/`ott` (enabled: false) → chỉ cần set true

### Suggested Implementation Order
1. **Sprint 1**: TwilioSmsSender + KafkaNotificationConsumer (cả 2 độc lập, song song được)
2. **Sprint 2**: gRPC server + FcmPushSender
3. **Sprint 3**: OTT senders (Zalo + Telegram) + webhook receivers
4. **Sprint 4**: Statistics API + DLQ management + retry dashboard

### Integration Notes
- Twilio webhook URL cần public endpoint — sử dụng ngrok cho dev
- Firebase service-account.json KHÔNG commit vào repo — env variable hoặc secrets manager
- Zalo OA cần business verification — setup trước khi develop
- Kafka topic `notification-inbound` cần create trước khi consumer start
