---
type: design
change: notification-service-phase2
status: draft
---

# Design: Notification Service Phase 2

## 1. Architecture

```
                           INBOUND ADAPTERS
                ┌─────────────────────────────────────────┐
                │                                         │
  ┌──────────┐  │  ┌──────────────┐  NotificationEnqueue  │
  │ REST API │──┼─▶│ Controller   │──────Service──────────┼──┐
  └──────────┘  │  └──────────────┘                       │  │
  ┌──────────┐  │  ┌──────────────┐                       │  │
  │ gRPC     │──┼─▶│ GrpcService  │──────────────────────┼──┤
  │ (opt)    │  │  └──────────────┘                       │  │
  └──────────┘  │  ┌──────────────┐                       │  │
  ┌──────────┐  │  │ KafkaConsumer│                       │  │
  │ Kafka    │──┼─▶│ (opt)        │──────────────────────┼──┤
  │ (opt)    │  │  └──────────────┘                       │  │  ┌──────────┐
  └──────────┘  │                                         │  ├─▶│  Queue   │
  ┌──────────┐  │  ┌──────────────┐                       │  │  │  (DB)    │
  │ Webhooks │──┼─▶│ Webhook      │──DeliveryTracking────┼──┤  └────┬─────┘
  └──────────┘  │  │ Controllers  │   Service             │  │       │
                │  └──────────────┘                       │  │  ┌────▼──────┐
                └─────────────────────────────────────────┘  │  │ JobSched. │
                                                             │  │ (poll)    │
                           OUTBOUND ADAPTERS                 │  └────┬──────┘
                ┌─────────────────────────────────────────┐  │       │
                │  ┌──────────────┐                       │  │  ┌────▼──────┐
                │  │ Dispatcher   │◀──────────────────────┼──┘  │ Template  │
                │  │ (Strategy)   │                       │     │ Render    │
                │  └──────┬───────┘                       │     └───────────┘
                │         │                               │
                │    ┌────┼────┬────────┬────────┐        │
                │    ▼    ▼    ▼        ▼        ▼        │
                │  ┌────┐┌────┐┌─────┐┌────────┐┌─────┐  │
                │  │SMTP││SMS ││ FCM ││OttDisp.││ ... │  │
                │  │Mail││Twil││Push ││  ├Tele. ││     │  │
                │  └────┘└────┘└─────┘│  └Zalo* ││     │  │
                │                     └────────┘└─────┘  │
                └─────────────────────────────────────────┘
```

## 2. Component Design

### 2.1 Channel Senders (adapter/out/sender/)

#### TwilioSmsSender
- **File:** `adapter/out/sender/TwilioSmsSender.kt`
- **Implements:** `NotificationSender`
- **Channel:** `NotificationChannel.SMS`
- **Dependencies:** `com.twilio.rest.api.v2010.account.Message`, `CircuitBreakerRegistry`
- **Config class:** `TwilioSmsProperties` (nested in `NotificationProperties`)
- **Conditional:** `@ConditionalOnProperty("app.notification.channels.sms.enabled", havingValue = "true")`
- **Pattern:** Follow SmtpEmailSender (CircuitBreaker + @ConditionalOnProperty)

#### FcmPushSender
- **File:** `adapter/out/sender/FcmPushSender.kt`
- **Implements:** `NotificationSender`
- **Channel:** `NotificationChannel.PUSH`
- **Dependencies:** `FirebaseMessaging`, `DeviceTokenRepository`, `CircuitBreakerRegistry`
- **Config:** `FirebaseConfig` (@Configuration, initializes FirebaseApp from credentials-file)
- **Conditional:** `@ConditionalOnProperty("app.notification.channels.push.enabled", havingValue = "true")`

#### OttDispatcher (Composite Sender)
- **File:** `adapter/out/sender/OttDispatcher.kt`
- **Implements:** `NotificationSender`
- **Channel:** `NotificationChannel.OTT`
- **Sub-routing:** `subSenderMap: Map<String, OttChannelSender>` keyed by sub_channel
- **Conditional:** `@ConditionalOnProperty("app.notification.channels.ott.enabled", havingValue = "true")`

#### OttChannelSender (Interface)
- **File:** `application/port/out/OttChannelSender.kt`
- **Methods:** `subChannel(): String`, `send(notification)`
- **Purpose:** Sub-channel strategy for OTT routing

#### TelegramOttSender
- **File:** `adapter/out/sender/TelegramOttSender.kt`
- **Implements:** `OttChannelSender`
- **Sub-channel:** `"TELEGRAM"`
- **Dependencies:** `WebClient`, `CircuitBreakerRegistry`
- **Config:** `TelegramProperties` (bot-token)
- **API:** `POST https://api.telegram.org/bot{token}/sendMessage`

### 2.2 Transport Adapters (adapter/in/)

#### NotificationGrpcService
- **File:** `adapter/in/grpc/NotificationGrpcService.kt`
- **Extends:** `NotificationServiceGrpc.NotificationServiceImplBase`
- **Methods:** `enqueue(EnqueueRequest): EnqueueResponse`
- **Delegates to:** `NotificationEnqueueService.enqueue()`
- **Conditional:** `@ConditionalOnProperty("app.notification.grpc.enabled", havingValue = "true", matchIfMissing = false)`

#### KafkaNotificationConsumer
- **File:** `adapter/in/kafka/KafkaNotificationConsumer.kt`
- **Annotation:** `@KafkaListener(topics = "notification-inbound")`
- **Deserializer:** `SmartNotificationDeserializer` (content-type header → JSON/Protobuf)
- **Delegates to:** `NotificationEnqueueService.enqueue()`
- **DLT:** `DefaultErrorHandler` + `DeadLetterPublishingRecoverer`
- **Conditional:** `@ConditionalOnProperty("app.notification.kafka.enabled", havingValue = "true", matchIfMissing = false)`

#### KafkaConfig
- **File:** `notification/config/KafkaConfig.kt`
- **Purpose:** Configure `ConsumerFactory`, `ConcurrentKafkaListenerContainerFactory`, `SmartNotificationDeserializer`
- **Conditional:** Same as consumer

### 2.3 Webhook Controllers (adapter/in/web/webhook/)

#### TwilioWebhookController
- **File:** `adapter/in/web/webhook/TwilioWebhookController.kt`
- **Endpoint:** `POST /api/v1/webhooks/twilio`
- **Security:** Validate X-Twilio-Signature via `RequestValidator`
- **Process:** Find by external_message_id → update status (delivered/undelivered/failed)

#### TelegramWebhookController
- **File:** `adapter/in/web/webhook/TelegramWebhookController.kt`
- **Endpoint:** `POST /api/v1/webhooks/telegram`
- **Security:** Validate `X-Telegram-Bot-Api-Secret-Token` header
- **Process:** Parse Update JSON → handle delivery confirmations

### 2.4 Admin Controllers (adapter/in/web/admin/)

#### DlqController
- **File:** `adapter/in/web/admin/DlqController.kt`
- **Endpoints:** GET list, POST retry, POST discard
- **Dependencies:** `NotificationDlqRepository`, `NotificationEnqueueService`

#### StatisticsController
- **File:** `adapter/in/web/admin/StatisticsController.kt`
- **Endpoints:** GET stats, GET stats/retry
- **Dependencies:** `StatisticsService`

#### DeviceTokenController
- **File:** `adapter/in/web/admin/DeviceTokenController.kt`
- **Endpoints:** CRUD for device tokens
- **Dependencies:** `DeviceTokenRepository`

### 2.5 Application Services

#### DeliveryTrackingService
- **File:** `application/DeliveryTrackingService.kt`
- **Purpose:** Update notification status based on webhook callbacks
- **Methods:** `updateDeliveryStatus(externalMessageId, newStatus, timestamp)`

#### StatisticsService
- **File:** `application/StatisticsService.kt`
- **Purpose:** Aggregate delivery statistics
- **Methods:** `getStats(from, to, channel)`, `getRetryReport(from, to)`
- **Implementation:** Native SQL queries via NotificationQueueRepository

#### RateLimitService
- **File:** `application/RateLimitService.kt`
- **Purpose:** Check rate limits before enqueue
- **Methods:** `checkRateLimit(recipient, channel, templateCode): Boolean`
- **Rule:** Max 1 SMS per user per template per hour
- **Error:** `NOTIF-012 RATE_LIMIT_EXCEEDED`

### 2.6 Persistence (adapter/out/persistence/)

#### NotificationQueueEntity (MODIFY)
- **Add columns:** `sub_channel: String?`, `external_message_id: String?`

#### NotificationDlqEntity (NEW)
- **Table:** `notification_dlq`
- **Base:** `SnowflakePersistentAuditableEntity`
- **Fields:** notificationId, channel, errorCode, errorMessage, originalPayload (JSONB), resolved, resolvedBy, resolvedAt

#### NotificationRetryLogEntity (NEW)
- **Table:** `notification_retry_log`
- **Base:** `SnowflakePersistentAuditableEntity`
- **Fields:** notificationId, attemptNumber, errorCode, errorMessage, attemptedAt

#### DeviceTokenEntity (NEW)
- **Table:** `notification_device_token`
- **Base:** `SnowflakePersistentAuditableEntity`
- **Fields:** userId, token (unique), platform (ANDROID/IOS/WEB), active

### 2.7 Configuration Extension

```yaml
app:
  notification:
    # Existing
    queue: { batch-size: 10, poll-interval-ms: 5000, max-retries: 3 }
    channels:
      email: { enabled: true }
      sms: { enabled: false }
      push: { enabled: false }
      ott: { enabled: false }
    mail: { from: "noreply@example.com" }
    
    # NEW — Phase 2
    sms:
      twilio:
        account-sid: ${TWILIO_ACCOUNT_SID}
        auth-token: ${TWILIO_AUTH_TOKEN}
        from-number: ${TWILIO_FROM_NUMBER}
    push:
      fcm:
        credentials-file: ${FIREBASE_CREDENTIALS_FILE}
    ott:
      telegram:
        bot-token: ${TELEGRAM_BOT_TOKEN}
        webhook-secret: ${TELEGRAM_WEBHOOK_SECRET}
    kafka:
      enabled: false   # plug-and-play
      bootstrap-servers: localhost:9092
      consumer:
        group-id: notification-service-group
        concurrency: 3
    grpc:
      enabled: false   # plug-and-play
```

### 2.8 Proto Definition

```protobuf
syntax = "proto3";
package ntt.notification.v1;

service NotificationService {
  rpc Enqueue(EnqueueRequest) returns (EnqueueResponse);
}

message EnqueueRequest {
  string recipient = 1;
  string template_code = 2;
  string template_data_json = 3;
  string channel = 4;
  string priority = 5;
  string correlation_id = 6;
  string source_service = 7;
  string sub_channel = 8;
}

message EnqueueResponse {
  int64 id = 1;
  string status = 2;
}
```

## 3. Sequence Diagrams

### 3.1 Kafka Enqueue Flow

```
Producer          Kafka            Consumer          EnqueueService       DB
  │                 │                 │                    │               │
  │  publish msg    │                 │                    │               │
  │────────────────▶│                 │                    │               │
  │                 │  poll/consume   │                    │               │
  │                 │────────────────▶│                    │               │
  │                 │                 │  detect format     │               │
  │                 │                 │  (JSON/Protobuf)   │               │
  │                 │                 │  dedup check       │               │
  │                 │                 │───────────────────▶│               │
  │                 │                 │                    │  INSERT       │
  │                 │                 │                    │──────────────▶│
  │                 │                 │                    │  201 Created  │
  │                 │                 │                    │◀──────────────│
  │                 │  commit offset  │                    │               │
  │                 │◀────────────────│                    │               │
```

### 3.2 Webhook Delivery Tracking

```
Twilio         WebhookCtrl     DeliveryTracking    QueueRepo        DB
  │                │                  │                │              │
  │  POST callback │                  │                │              │
  │───────────────▶│                  │                │              │
  │                │ verify HMAC      │                │              │
  │                │ extract SID      │                │              │
  │                │─────────────────▶│                │              │
  │                │                  │ findByExtId    │              │
  │                │                  │───────────────▶│              │
  │                │                  │                │─────────────▶│
  │                │                  │                │◀─────────────│
  │                │                  │ update status  │              │
  │                │                  │ set delivered_at│             │
  │                │                  │───────────────▶│              │
  │                │                  │                │─────────────▶│
  │  200 OK        │                  │                │              │
  │◀───────────────│                  │                │              │
```

## 4. Database Migrations

### V4 — Add columns to notification_queue
```sql
ALTER TABLE notification_queue ADD COLUMN sub_channel VARCHAR(20);
ALTER TABLE notification_queue ADD COLUMN external_message_id VARCHAR(255);
CREATE INDEX idx_notif_queue_ext_msg_id ON notification_queue(external_message_id);
CREATE INDEX idx_notif_queue_sub_channel ON notification_queue(sub_channel);
```

### V5 — Device token table
```sql
CREATE TABLE notification_device_token (
    id          BIGINT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(500) NOT NULL UNIQUE,
    platform    VARCHAR(10) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_device_token_user ON notification_device_token(user_id, active);
```

### V6 — DLQ table
```sql
CREATE TABLE notification_dlq (
    id                  BIGINT PRIMARY KEY,
    notification_id     BIGINT NOT NULL REFERENCES notification_queue(id),
    channel             VARCHAR(20) NOT NULL,
    error_code          VARCHAR(50),
    error_message       TEXT,
    original_payload    JSONB,
    resolved            BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by         BIGINT,
    resolved_at         TIMESTAMP,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_dlq_resolved ON notification_dlq(resolved);
CREATE INDEX idx_dlq_notification ON notification_dlq(notification_id);
```

### V7 — Retry log table
```sql
CREATE TABLE notification_retry_log (
    id                  BIGINT PRIMARY KEY,
    notification_id     BIGINT NOT NULL REFERENCES notification_queue(id),
    attempt_number      INT NOT NULL,
    error_code          VARCHAR(50),
    error_message       TEXT,
    attempted_at        TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_retry_log_notification ON notification_retry_log(notification_id);
```
