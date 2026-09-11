# Business Analysis — Notification Service Phase 2

## 1. Use Cases

### UC-01: Gửi SMS Notification
**Mô tả:** Hệ thống gửi SMS qua Twilio khi channel = SMS
**Tại sao cần:** Không phải user nào cũng dùng email, SMS reach rate > 95%

**Basic Flow:**
1. Producer enqueue notification với channel=SMS, recipient=phone_number
2. JobScheduler pick up notification
3. Dispatcher route tới `TwilioSmsSender`
4. TwilioSmsSender gọi Twilio API
5. Nhận delivery callback → update status DELIVERED

**Exception Flows:**
- E1: Twilio API rate limit → exponential backoff retry
- E2: Invalid phone number → mark FAILED, log error code
- E3: Twilio outage → CircuitBreaker OPEN, fallback to queue

**Business Rules:**
- BR-01: Phone number phải đúng format E.164 (+84xxx)
- BR-02: Max 1 SMS/user/template/giờ (rate limiting)
- BR-03: SMS content max 160 chars (hoặc split multi-part)

---

### UC-02: Gửi Push Notification (FCM)
**Mô tả:** Gửi push notification qua Firebase Cloud Messaging
**Tại sao cần:** Push notification cho mobile app, real-time engagement

**Basic Flow:**
1. Producer enqueue với channel=PUSH, recipient=device_token
2. Dispatcher route tới `FcmPushSender`
3. FcmPushSender gọi `FirebaseMessaging.send(message)`
4. FCM return message_id → mark SENT
5. Firebase Analytics → delivery tracking

**Exception Flows:**
- E1: Invalid device token (UNREGISTERED) → remove token, mark FAILED
- E2: FCM quota exceeded → backoff retry
- E3: Token expired → request new token via app refresh

**Business Rules:**
- BR-04: Mỗi user có thể có nhiều device tokens (multi-device)
- BR-05: Support notification + data message types
- BR-06: Topic messaging cho broadcast

---

### UC-03: Gửi OTT Message (Zalo/Telegram)
**Mô tả:** Gửi message qua Zalo OA hoặc Telegram Bot
**Tại sao cần:** Vietnam market — Zalo là platform #1, Telegram growing

**Basic Flow:**
1. Producer enqueue với channel=OTT, sub_channel=ZALO/TELEGRAM
2. Dispatcher route tới `ZaloOttSender` hoặc `TelegramOttSender`
3. Sender gọi OTT API với template message
4. Nhận response → update status
5. (Zalo) Poll `message/status` API → update DELIVERED/READ

**Exception Flows:**
- E1: Zalo access token expired → refresh via OAuth2 flow
- E2: User chưa follow OA → mark FAILED with reason
- E3: Telegram bot blocked → mark FAILED

**Business Rules:**
- BR-07: Zalo cần user đã follow Official Account
- BR-08: Zalo ZBS template messages cần pre-approved templates
- BR-09: Telegram không có read receipt — track qua inline button clicks

---

### UC-04: Nhận Notification qua Kafka Consumer
**Mô tả:** Notification service consume events từ Kafka topic
**Tại sao cần:** Event-driven architecture, decouple producers, high throughput

**Basic Flow:**
1. Producer publish event tới `notification-inbound` topic
2. KafkaNotificationConsumer listen topic
3. Deserialize Protobuf/JSON message
4. Validate + insert vào notification_queue
5. JobScheduler pick up và process

**Exception Flows:**
- E1: Deserialization error → send to DLT (Dead Letter Topic)
- E2: Validation error → send to DLT with error reason
- E3: DB insert failure → retry consumer, max 3 attempts

**Business Rules:**
- BR-10: Kafka consumer group = `notification-service-group`
- BR-11: Idempotency check: dedup bằng `source_id + source_service`
- BR-12: Support batch consume cho throughput

---

### UC-05: Expose gRPC API
**Mô tả:** Notification service expose gRPC endpoints cho inter-service communication
**Tại sao cần:** Low latency, type-safe contracts, efficient binary protocol

**Basic Flow:**
1. Client service gọi `NotificationGrpcService.enqueue(request)`
2. Server validate Protobuf request
3. Insert vào notification_queue
4. Return response với notification_id
5. Client có thể stream status updates

**Exception Flows:**
- E1: Invalid request → return gRPC Status.INVALID_ARGUMENT
- E2: Server overload → return gRPC Status.RESOURCE_EXHAUSTED

**Business Rules:**
- BR-13: gRPC deadline default 5 seconds
- BR-14: Proto file share qua Maven artifact

---

### UC-06: Delivery Tracking & Read Receipts
**Mô tả:** Track delivery status và read receipts cho mỗi notification
**Tại sao cần:** Business cần biết message đã đến tay user chưa

**Basic Flow:**
1. Notification sent → status = SENT
2. Channel webhook callback → status = DELIVERED
3. User view/read → status = READ
4. System record timestamp cho mỗi transition

**Tracking per Channel:**
| Channel | Delivery Tracking | Read Receipt |
|---------|------------------|-------------|
| EMAIL | Pixel tracking (optional) | Link click tracking (optional) |
| SMS | Twilio webhook callback | ❌ (SMS spec limitation) |
| PUSH | FCM delivery receipt | ❌ (app-level tracking) |
| ZALO | `message/status` API polling | ✅ Native support |
| TELEGRAM | ❌ (privacy) | ❌ (privacy) |

---

### UC-07: Reporting & Statistics API
**Mô tả:** API cung cấp statistics về notification delivery
**Tại sao cần:** Business monitoring, SLA tracking, cost optimization

**Endpoints:**
- `GET /api/v1/notifications/stats?from=&to=&channel=` — tổng hợp statistics
- `GET /api/v1/notifications/stats/channels` — breakdown per channel
- `GET /api/v1/notifications/retry-report` — retry failures report

**Metrics:**
- Total sent/delivered/failed/read per channel per time range
- Average delivery latency per channel
- Retry success rate
- Failed reasons distribution

---

### UC-08: Enhanced Retry Management
**Mô tả:** Quản lý retry với DLQ, configurable policies, dashboard
**Tại sao cần:** Production reliability, visibility into failures

**Basic Flow:**
1. Notification fails after max retries → insert vào `notification_dlq` table
2. Admin review DLQ entries via API
3. Admin can manually retry or discard entries
4. System records retry history for each notification

---

## 2. Traceability Matrix

| Use Case | Channels | Transports | Status Tracking |
|----------|----------|-----------|----------------|
| UC-01 SMS | SMS | REST/gRPC/Kafka | SENT→DELIVERED |
| UC-02 Push | PUSH | REST/gRPC/Kafka | SENT→(app tracking) |
| UC-03 OTT | OTT | REST/gRPC/Kafka | SENT→DELIVERED→READ |
| UC-04 Kafka | All | Kafka inbound | N/A (ingestion) |
| UC-05 gRPC | All | gRPC inbound | N/A (ingestion) |
| UC-06 Tracking | All | Webhook inbound | All states |
| UC-07 Reporting | All | REST outbound | N/A (query) |
| UC-08 Retry | All | REST/internal | FAILED→PENDING |
