---
type: srs
change: notification-service-phase2
status: draft
---

# SRS: Notification Service Phase 2

## 1. Functional Requirements

### 1.1 Channel Senders

#### FR-001: SMS via Twilio
- **Input:** `channel = SMS`, `recipient` = E.164 phone number
- **Process:** TwilioSmsSender validates E.164 → calls `Message.creator()` via Twilio SDK → stores Twilio SID in `external_message_id`
- **Output:** SMS delivered, status → SENT
- **Error:** `NOTIF-006 SMS_DELIVERY_FAILED` (502), `NOTIF-009 INVALID_PHONE_NUMBER` (400)
- **Config:** `app.notification.sms.twilio.account-sid`, `auth-token`, `from-number`
- **Resilience:** CircuitBreaker("smsCircuitBreaker")

#### FR-002: Push via FCM
- **Input:** `channel = PUSH`, `recipient` = user ID
- **Process:** FcmPushSender queries DeviceTokenRepository → sends to all active tokens via `FirebaseMessaging.sendEachForMulticast()` → stores FCM message_id in `external_message_id`
- **Output:** Push delivered to devices, status → SENT
- **Error:** `NOTIF-007 PUSH_DELIVERY_FAILED` (502), `NOTIF-010 INVALID_DEVICE_TOKEN` (400)
- **Config:** `app.notification.push.fcm.credentials-file`

#### FR-004: Telegram OTT
- **Input:** `channel = OTT`, `sub_channel = TELEGRAM`, `recipient` = Telegram chat_id
- **Process:** OttDispatcher routes to TelegramOttSender → calls `sendMessage` via Bot API (WebClient) → stores Telegram message_id in `external_message_id`
- **Output:** Telegram message delivered, status → SENT
- **Error:** `NOTIF-008 OTT_DELIVERY_FAILED` (502)
- **Config:** `app.notification.ott.telegram.bot-token`

### 1.2 Transport Layer

#### FR-005: gRPC Server
- **Input:** `NotificationService.Enqueue(EnqueueRequest)` gRPC call
- **Process:** NotificationGrpcService validates → delegates to NotificationEnqueueService → returns EnqueueResponse
- **Output:** gRPC response with notification ID + status
- **Proto:** `notification_service.proto` (in notification-client)
- **Port:** 9090 (configurable via `grpc.server.port`)
- **Conditional:** `@ConditionalOnProperty("app.notification.grpc.enabled", havingValue = "true", matchIfMissing = false)`

#### FR-006: Kafka Consumer
- **Input:** Kafka message from topic `notification-inbound`
- **Process:** KafkaNotificationConsumer deserializes (JSON or Protobuf via SmartNotificationDeserializer) → validates → delegates to NotificationEnqueueService
- **Output:** Notification enqueued, Kafka offset committed
- **DLT:** Invalid messages → `notification-inbound-dlt`
- **Consumer Group:** `notification-service-group`
- **Conditional:** `@ConditionalOnProperty("app.notification.kafka.enabled", havingValue = "true", matchIfMissing = false)`

#### FR-007: Kafka DLT
- **Input:** Failed/invalid Kafka messages
- **Process:** Spring Kafka `DefaultErrorHandler` with `DeadLetterPublishingRecoverer` → publishes to `-dlt` topic
- **Output:** Message in DLT for manual review

### 1.3 Client SDK

#### FR-008: gRPC Client Adapter
- **Class:** `GrpcNotificationAdapter` implements `NotificationPort`
- **Location:** `notification-client` module
- **Dependency:** Generated gRPC stubs from `.proto`
- **Conditional:** Available when gRPC stubs on classpath

#### FR-009: Kafka Producer Adapter
- **Class:** `KafkaNotificationAdapter` implements `NotificationPort`
- **Location:** `notification-client` module
- **Dependency:** `spring-kafka` + `KafkaTemplate`
- **Conditional:** Available when Kafka on classpath

### 1.4 Delivery Tracking

#### FR-010: Twilio Webhook
- **Endpoint:** `POST /api/v1/webhooks/twilio`
- **Input:** Twilio StatusCallback payload (MessageSid, MessageStatus)
- **Process:** Validate `X-Twilio-Signature` (HMAC-SHA1) → find notification by `external_message_id` → update status (delivered/failed)
- **Security:** HMAC signature verification

#### FR-012: Delivery/Read Timestamps
- **Modify:** NotificationQueueEntity `delivered_at`, `read_at` already exist
- **Process:** Webhook handlers populate these fields on status transitions SENT→DELIVERED→READ

### 1.5 Retry Management

#### FR-013: DLQ Table
- **Table:** `notification_dlq`
- **Columns:** `id`, `notification_id` (FK), `channel`, `error_code`, `error_message`, `original_payload` (JSONB), `resolved` (boolean), `resolved_by`, `resolved_at`, `created_at`
- **Trigger:** NotificationJobScheduler.handleFailure() → when retryCount >= maxRetries → insert into DLQ

#### FR-014: Retry Log
- **Table:** `notification_retry_log`
- **Columns:** `id`, `notification_id` (FK), `attempt_number`, `error_code`, `error_message`, `attempted_at`
- **Trigger:** Every retry attempt in handleFailure() → insert log entry

#### FR-015: Manual Retry
- **Endpoints:**
  - `GET /api/v1/notifications/dlq` — list DLQ entries (paginated)
  - `POST /api/v1/notifications/dlq/{id}/retry` — re-enqueue for retry
  - `POST /api/v1/notifications/dlq/{id}/discard` — mark resolved without retry
- **Authorization:** Admin only

### 1.6 Reporting

#### FR-016: Statistics API
- **Endpoint:** `GET /api/v1/notifications/stats?from=&to=&channel=`
- **Response:** Total sent/delivered/failed/read counts per channel, delivery latency P50/P95
- **Implementation:** Native SQL aggregate queries for performance

#### FR-017: Retry Report
- **Endpoint:** `GET /api/v1/notifications/stats/retry?from=&to=`
- **Response:** Retry count per channel, top error codes, DLQ count

### 1.7 Data Model

#### FR-018: Sub-channel Column
- **Migration V4:** `ALTER TABLE notification_queue ADD COLUMN sub_channel VARCHAR(20);`
- **Entity:** Add `subChannel: String?` to NotificationQueueEntity
- **Usage:** OTT routing (TELEGRAM, ZALO future)

#### FR-019: Device Token Table
- **Migration V5:** `CREATE TABLE notification_device_token(...)`
- **Columns:** `id`, `user_id` (indexed), `token` (unique), `platform` (ANDROID/IOS/WEB), `active` (boolean), `created_at`, `updated_at`
- **Admin API:**
  - `POST /api/v1/admin/device-tokens` — register
  - `GET /api/v1/admin/device-tokens/{userId}` — list by user
  - `DELETE /api/v1/admin/device-tokens/{id}` — deactivate
  - `PUT /api/v1/admin/device-tokens/{id}` — update

#### FR-020: External Message ID
- **Migration V4:** `ALTER TABLE notification_queue ADD COLUMN external_message_id VARCHAR(255);`
- **Entity:** Add `externalMessageId: String?` to NotificationQueueEntity
- **Usage:** Twilio SID, FCM message_id, Telegram message_id

### 1.8 Domain Enhancement

#### FR-021: Idempotency (Kafka)
- **Check:** `source_id + source_service` combination must be unique
- **Implementation:** Query `findByCorrelationId()` (correlationId = "{source_service}:{source_id}")
- **Error:** `NOTIF-002 DUPLICATE` (409)

#### FR-022: Rate Limiting
- **Rule:** Max 1 SMS per user per template per hour
- **Implementation:** RateLimitService queries recent notifications
- **Error:** `NOTIF-012 RATE_LIMIT_EXCEEDED` (429)

#### FR-023: Circuit Breaker per Channel
- **Pattern:** Follow SmtpEmailSender — each sender gets named CircuitBreaker
- **Registry:** CircuitBreakerRegistry with per-channel config
- **Names:** `emailCircuitBreaker`, `smsCircuitBreaker`, `pushCircuitBreaker`, `ottCircuitBreaker`

## 2. Non-Functional Requirements

| NFR | Requirement |
|-----|------------|
| NFR-001 | SMS delivery latency < 5s |
| NFR-002 | Push delivery latency < 2s |
| NFR-003 | gRPC deadline default 5s |
| NFR-004 | Kafka consumer concurrency configurable (default 3) |
| NFR-005 | Prometheus metrics per channel |

## 3. Error Codes (New)

| Code | Enum | HTTP | Description |
|------|------|------|-------------|
| NOTIF-006 | SMS_DELIVERY_FAILED | 502 | Twilio SMS delivery failed |
| NOTIF-007 | PUSH_DELIVERY_FAILED | 502 | FCM push delivery failed |
| NOTIF-008 | OTT_DELIVERY_FAILED | 502 | OTT message delivery failed |
| NOTIF-009 | INVALID_PHONE_NUMBER | 400 | Not E.164 format |
| NOTIF-010 | INVALID_DEVICE_TOKEN | 400 | FCM token invalid/expired |
| NOTIF-011 | DLQ_ENTRY_NOT_FOUND | 404 | DLQ entry not found |
| NOTIF-012 | RATE_LIMIT_EXCEEDED | 429 | Rate limit exceeded |
| NOTIF-015 | KAFKA_DESERIALIZATION_ERROR | 400 | Invalid Kafka message |
| NOTIF-016 | GRPC_VALIDATION_ERROR | 400 | Invalid gRPC request |
| NOTIF-017 | PROVIDER_UNAVAILABLE | 503 | Provider circuit open |

## 4. API Endpoints (New)

| Method | Path | FR | Auth |
|--------|------|-----|------|
| POST | `/api/v1/webhooks/twilio` | FR-010 | Twilio HMAC |
| POST | `/api/v1/webhooks/telegram` | FR-004 | Secret token |
| GET | `/api/v1/notifications/stats` | FR-016 | Admin |
| GET | `/api/v1/notifications/stats/retry` | FR-017 | Admin |
| GET | `/api/v1/notifications/dlq` | FR-015 | Admin |
| POST | `/api/v1/notifications/dlq/{id}/retry` | FR-015 | Admin |
| POST | `/api/v1/notifications/dlq/{id}/discard` | FR-015 | Admin |
| POST | `/api/v1/admin/device-tokens` | FR-019 | Admin |
| GET | `/api/v1/admin/device-tokens/{userId}` | FR-019 | Admin |
| PUT | `/api/v1/admin/device-tokens/{id}` | FR-019 | Admin |
| DELETE | `/api/v1/admin/device-tokens/{id}` | FR-019 | Admin |

## 5. Database Migrations

| Version | Description | FRs |
|---------|-------------|-----|
| V4 | Add `sub_channel`, `external_message_id` to notification_queue | FR-018, FR-020 |
| V5 | Create `notification_device_token` table | FR-019 |
| V6 | Create `notification_dlq` table | FR-013 |
| V7 | Create `notification_retry_log` table | FR-014 |
