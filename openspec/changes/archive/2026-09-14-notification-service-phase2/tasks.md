---
type: tasks
change: notification-service-phase2
status: draft
<!-- self-contained: true -->
<!-- pipeline: wf_openspec_apply -->
<!-- locked_profile: { flow: "Command", factory: "N/A", feature_type: "EXTEND", transaction_flow: "single-step" } -->
<!-- context_loaded: true -->
<!-- reuse_rules_loaded: true -->
---

# Tasks: Notification Service Phase 2

## Phase 2a — Sprint 1-2: SMS + Transports + Client SDK

### Data Model Foundation

- [x] **Task 1: DB Migration V4 — Add columns**
  - File: `src/main/resources/db/migration/V4__add_subchannel_external_id.sql` | Action: [NEW]
  - FR: FR-018 — Add sub_channel column, FR-020 — Add external_message_id column
  - SQL: `ALTER TABLE notification_queue ADD COLUMN sub_channel VARCHAR(20); ALTER TABLE notification_queue ADD COLUMN external_message_id VARCHAR(255);`
  - Indexes: `idx_notif_queue_ext_msg_id`, `idx_notif_queue_sub_channel`

- [x] **Task 2: Update NotificationQueueEntity**
  - File: `notification/adapter/out/persistence/entity/NotificationQueueEntity.kt` | Action: [MODIFY]
  - FR: FR-018, FR-020
  - Add: `subChannel: String?` (line ~28, after channel), `externalMessageId: String?` (line ~81, after sourceService)
  - Base: `SnowflakePersistentAuditableEntity` from `com.ntt.basecore.model.id`

### Error Codes

- [x] **Task 3: Add new error codes**
  - File: `shared/exception/NotificationErrorCode.kt` | Action: [MODIFY]
  - FR: FR-001/002/004/022
  - Add enums: `SMS_DELIVERY_FAILED(NOTIF-006, 502)`, `PUSH_DELIVERY_FAILED(NOTIF-007, 502)`, `OTT_DELIVERY_FAILED(NOTIF-008, 502)`, `INVALID_PHONE_NUMBER(NOTIF-009, 400)`, `INVALID_DEVICE_TOKEN(NOTIF-010, 400)`, `DLQ_ENTRY_NOT_FOUND(NOTIF-011, 404)`, `RATE_LIMIT_EXCEEDED(NOTIF-012, 429)`, `KAFKA_DESERIALIZATION_ERROR(NOTIF-015, 400)`, `GRPC_VALIDATION_ERROR(NOTIF-016, 400)`, `PROVIDER_UNAVAILABLE(NOTIF-017, 503)`
  - Pattern: Follow existing enum pattern with `toErrorCodeBase()` bridge

### SMS Channel

- [x] **Task 4: Twilio properties in NotificationProperties**
  - File: `notification/config/NotificationProperties.kt` | Action: [MODIFY]
  - FR: FR-001
  - Add: `val sms: SmsProperties = SmsProperties()` with nested `TwilioProperties(accountSid, authToken, fromNumber)`
  - Pattern: Follow `MailSenderProperties` nesting

- [x] **Task 5: TwilioSmsSender**
  - File: `notification/adapter/out/sender/TwilioSmsSender.kt` | Action: [NEW]
  - FR: FR-001 — Send SMS via Twilio API
  - Base: implements `NotificationSender`
  - Dependencies: `com.twilio.rest.api.v2010.account.Message`, `CircuitBreakerRegistry`
  - Pattern: Follow `SmtpEmailSender` — `@Component`, `@ConditionalOnProperty("app.notification.channels.sms.enabled")`, `circuitBreaker.executeRunnable{}`
  - Error: `NOTIF-006 SMS_DELIVERY_FAILED`, `NOTIF-009 INVALID_PHONE_NUMBER`
  - Store Twilio SID → `notification.externalMessageId`

- [x] **Task 6: Add Twilio dependency**
  - File: `build.gradle.kts` | Action: [MODIFY]
  - Add: `implementation("com.twilio.sdk:twilio:10.6.3")`

### Rate Limiting

- [x] **Task 7: RateLimitService**
  - File: `notification/application/RateLimitService.kt` | Action: [NEW]
  - FR: FR-022 — Max 1 SMS/user/template/hour
  - Dependencies: `NotificationQueueRepository`
  - Add repository method: `countByRecipientAndTemplateCodeAndChannelAndCreatedAtAfter()`
  - Error: `NOTIF-012 RATE_LIMIT_EXCEEDED`

- [x] **Task 8: Integrate rate limiting into EnqueueService**
  - File: `notification/application/NotificationEnqueueService.kt` | Action: [MODIFY]
  - FR: FR-022
  - Add: `rateLimitService.checkRateLimit()` call before save, inject `RateLimitService`

### gRPC Transport (Optional)

- [x] **Task 9: Proto definition**
  - File: `src/main/proto/ntt/notification/v1/notification_service.proto` | Action: [NEW]
  - FR: FR-005
  - Define: `NotificationService.Enqueue(EnqueueRequest) returns (EnqueueResponse)`
  - Fields: recipient, template_code, template_data_json, channel, priority, correlation_id, source_service, sub_channel

- [x] **Task 10: gRPC Spring Boot config**
  - File: `notification/config/GrpcConfig.kt` | Action: [NEW]
  - FR: FR-005
  - Conditional: `@ConditionalOnProperty("app.notification.grpc.enabled", havingValue = "true", matchIfMissing = false)`
  - gRPC reflection: `@ConditionalOnProfile("dev")`

- [x] **Task 11: NotificationGrpcService**
  - File: `notification/adapter/in/grpc/NotificationGrpcService.kt` | Action: [NEW]
  - FR: FR-005
  - Extends: `NotificationServiceGrpc.NotificationServiceImplBase` (generated from proto)
  - Delegates to: `NotificationEnqueueService.enqueue()`
  - Conditional: `@ConditionalOnProperty("app.notification.grpc.enabled")`
  - Error: `NOTIF-016 GRPC_VALIDATION_ERROR`

- [x] **Task 12: Add gRPC dependencies**
  - File: `build.gradle.kts` | Action: [MODIFY]
  - Add: `implementation("net.devh:grpc-server-spring-boot-starter:3.1.0")`, `implementation("io.grpc:grpc-protobuf:1.68.0")`, protobuf plugin

### Kafka Transport (Optional)

- [x] **Task 13: Kafka config**
  - File: `notification/config/KafkaConfig.kt` | Action: [NEW]
  - FR: FR-006
  - Conditional: `@ConditionalOnProperty("app.notification.kafka.enabled", havingValue = "true", matchIfMissing = false)`
  - Configure: `ConsumerFactory`, `ConcurrentKafkaListenerContainerFactory`, `DeadLetterPublishingRecoverer`
  - Concurrency: configurable (default 3)

- [x] **Task 14: SmartNotificationDeserializer**
  - File: `notification/adapter/in/kafka/SmartNotificationDeserializer.kt` | Action: [NEW]
  - FR: FR-006 (DD-002 dual serialization)
  - Logic: Check `content-type` header → delegate to JsonDeserializer or ProtobufDeserializer
  - Default: JSON (when no header)

- [x] **Task 15: KafkaNotificationConsumer**
  - File: `notification/adapter/in/kafka/KafkaNotificationConsumer.kt` | Action: [NEW]
  - FR: FR-006, FR-007, FR-021
  - `@KafkaListener(topics = "notification-inbound", containerFactory = "kafkaListenerContainerFactory")`
  - Idempotency: build correlationId from `source_service:source_id` → check `findByCorrelationId()`
  - DLT: Invalid messages → `notification-inbound-dlt`
  - Conditional: `@ConditionalOnProperty("app.notification.kafka.enabled")`

- [x] **Task 16: Add Kafka dependencies**
  - File: `build.gradle.kts` | Action: [MODIFY]
  - Add: `implementation("org.springframework.kafka:spring-kafka")`
  - Auto-config exclude when disabled: handle via `@ConditionalOnProperty`

- [x] **Task 17: Kafka properties in NotificationProperties**
  - File: `notification/config/NotificationProperties.kt` | Action: [MODIFY]
  - Add: `val kafka: KafkaProperties = KafkaProperties()` with `enabled`, `bootstrapServers`, `consumer(groupId, concurrency)`

### Client SDK v2

- [x] **Task 18: GrpcNotificationAdapter**
  - File: `notification-client/.../GrpcNotificationAdapter.kt` | Action: [NEW]
  - FR: FR-008
  - Implements: `NotificationPort`
  - Dependencies: Generated gRPC stubs
  - Auto-config: `@ConditionalOnClass(NotificationServiceGrpc::class)`

- [x] **Task 19: KafkaNotificationAdapter**
  - File: `notification-client/.../KafkaNotificationAdapter.kt` | Action: [NEW]
  - FR: FR-009
  - Implements: `NotificationPort`
  - Dependencies: `KafkaTemplate`
  - Auto-config: `@ConditionalOnClass(KafkaTemplate::class)`

### Application Config

- [x] **Task 20: Update application.yml**
  - File: `src/main/resources/application.yml` | Action: [MODIFY]
  - Add: sms.twilio, push.fcm, ott.telegram, kafka, grpc sections (all disabled by default)

---

## Phase 2b — Sprint 3: Push + Tracking

### Push Channel

- [x] **Task 21: DB Migration V5 — Device token table**
  - File: `src/main/resources/db/migration/V5__create_device_token.sql` | Action: [NEW]
  - FR: FR-019
  - SQL: `CREATE TABLE notification_device_token(id, user_id, token, platform, active, created_at, updated_at)`

- [x] **Task 22: DeviceTokenEntity**
  - File: `notification/adapter/out/persistence/entity/DeviceTokenEntity.kt` | Action: [NEW]
  - FR: FR-019
  - Base: `SnowflakePersistentAuditableEntity`
  - Fields: userId, token (unique), platform (ANDROID/IOS/WEB), active

- [x] **Task 23: DeviceTokenRepository**
  - File: `notification/adapter/out/persistence/repository/DeviceTokenRepository.kt` | Action: [NEW]
  - FR: FR-019
  - Methods: `findByUserIdAndActiveTrue(userId)`, `findByToken(token)`

- [x] **Task 24: DeviceTokenController (Admin)**
  - File: `notification/adapter/in/web/admin/DeviceTokenController.kt` | Action: [NEW]
  - FR: FR-019
  - Endpoints: POST register, GET by userId, PUT update, DELETE deactivate

- [x] **Task 25: Firebase config**
  - File: `notification/config/FirebaseConfig.kt` | Action: [NEW]
  - FR: FR-002
  - Initialize `FirebaseApp` from credentials-file path
  - Conditional: `@ConditionalOnProperty("app.notification.channels.push.enabled")`

- [x] **Task 26: FcmPushSender**
  - File: `notification/adapter/out/sender/FcmPushSender.kt` | Action: [NEW]
  - FR: FR-002
  - Base: implements `NotificationSender`
  - Query `DeviceTokenRepository.findByUserIdAndActiveTrue()` → `FirebaseMessaging.sendEachForMulticast()`
  - Store FCM message_id → `externalMessageId`
  - Pattern: Follow SmtpEmailSender (CircuitBreaker)
  - Error: `NOTIF-007`, `NOTIF-010`

- [x] **Task 27: Add Firebase dependency**
  - File: `build.gradle.kts` | Action: [MODIFY]
  - Add: `implementation("com.google.firebase:firebase-admin:9.3.0")`

- [x] **Task 28: Push properties**
  - File: `notification/config/NotificationProperties.kt` | Action: [MODIFY]
  - Add: `val push: PushProperties = PushProperties()` with `FcmProperties(credentialsFile)`

### Delivery Tracking

- [x] **Task 29: DeliveryTrackingService**
  - File: `notification/application/DeliveryTrackingService.kt` | Action: [NEW]
  - FR: FR-012
  - Methods: `updateDeliveryStatus(externalMessageId, newStatus, timestamp)`
  - Find by `externalMessageId` → update status + `deliveredAt`/`readAt`

- [x] **Task 30: NotificationQueueRepository — findByExternalMessageId**
  - File: `notification/adapter/out/persistence/repository/NotificationQueueRepository.kt` | Action: [MODIFY]
  - FR: FR-010, FR-012
  - Add: `findByExternalMessageId(externalMessageId: String): NotificationQueueEntity?`

- [x] **Task 31: TwilioWebhookController**
  - File: `notification/adapter/in/web/webhook/TwilioWebhookController.kt` | Action: [NEW]
  - FR: FR-010
  - Endpoint: `POST /api/v1/webhooks/twilio`
  - Security: Validate `X-Twilio-Signature` (HMAC-SHA1)
  - Delegates to: `DeliveryTrackingService.updateDeliveryStatus()`

- [x] **Task 32: Add WebFlux dependency**
  - File: `build.gradle.kts` | Action: [MODIFY]
  - Add: `implementation("org.springframework.boot:spring-boot-starter-webflux")` (WebClient for OTT)

---

## Phase 2c — Sprint 4: OTT (Telegram) + Sub-routing

- [x] **Task 33: OttChannelSender interface**
  - File: `notification/application/port/out/OttChannelSender.kt` | Action: [NEW]
  - FR: FR-004 (DD-001)
  - Methods: `subChannel(): String`, `send(notification)`

- [x] **Task 34: OttDispatcher (Composite Sender)**
  - File: `notification/adapter/out/sender/OttDispatcher.kt` | Action: [NEW]
  - FR: FR-004 (DD-001)
  - Implements: `NotificationSender` (channel = OTT)
  - Sub-routing: `subSenderMap: Map<String, OttChannelSender>` keyed by `notification.subChannel`
  - Conditional: `@ConditionalOnProperty("app.notification.channels.ott.enabled")`

- [x] **Task 35: TelegramOttSender**
  - File: `notification/adapter/out/sender/TelegramOttSender.kt` | Action: [NEW]
  - FR: FR-004
  - Implements: `OttChannelSender`
  - WebClient → `POST https://api.telegram.org/bot{token}/sendMessage`
  - Store Telegram message_id → `externalMessageId`
  - CircuitBreaker("ottCircuitBreaker")
  - Error: `NOTIF-008 OTT_DELIVERY_FAILED`

- [x] **Task 36: TelegramWebhookController**
  - File: `notification/adapter/in/web/webhook/TelegramWebhookController.kt` | Action: [NEW]
  - FR: FR-004 (DD-010 webhook mode)
  - Endpoint: `POST /api/v1/webhooks/telegram`
  - Security: Validate `X-Telegram-Bot-Api-Secret-Token`
  - Delegates to: `DeliveryTrackingService`

- [x] **Task 37: OTT properties**
  - File: `notification/config/NotificationProperties.kt` | Action: [MODIFY]
  - Add: `val ott: OttProperties = OttProperties()` with `TelegramProperties(botToken, webhookSecret)`

---

## Phase 2d — Sprint 5: DLQ + Retry + Statistics

### DLQ Management

- [x] **Task 38: DB Migration V6 — DLQ table**
  - File: `src/main/resources/db/migration/V6__create_notification_dlq.sql` | Action: [NEW]
  - FR: FR-013

- [x] **Task 39: DB Migration V7 — Retry log table**
  - File: `src/main/resources/db/migration/V7__create_retry_log.sql` | Action: [NEW]
  - FR: FR-014

- [x] **Task 40: NotificationDlqEntity**
  - File: `notification/adapter/out/persistence/entity/NotificationDlqEntity.kt` | Action: [NEW]
  - FR: FR-013
  - Base: `SnowflakePersistentAuditableEntity`
  - Fields: notificationId, channel, errorCode, errorMessage, originalPayload (JSONB), resolved, resolvedBy, resolvedAt

- [x] **Task 41: NotificationRetryLogEntity**
  - File: `notification/adapter/out/persistence/entity/NotificationRetryLogEntity.kt` | Action: [NEW]
  - FR: FR-014
  - Base: `SnowflakePersistentAuditableEntity`
  - Fields: notificationId, attemptNumber, errorCode, errorMessage, attemptedAt

- [x] **Task 42: Repositories for DLQ + RetryLog**
  - File: `notification/adapter/out/persistence/repository/NotificationDlqRepository.kt` | Action: [NEW]
  - File: `notification/adapter/out/persistence/repository/NotificationRetryLogRepository.kt` | Action: [NEW]
  - FR: FR-013, FR-014

- [x] **Task 43: Integrate DLQ + RetryLog into JobScheduler**
  - File: `notification/application/NotificationJobScheduler.kt` | Action: [MODIFY]
  - FR: FR-013, FR-014
  - In `handleFailure()`: insert RetryLogEntity per attempt, insert DlqEntity when retryCount >= maxRetries
  - Dependencies: inject `NotificationDlqRepository`, `NotificationRetryLogRepository`

- [x] **Task 44: DlqController**
  - File: `notification/adapter/in/web/admin/DlqController.kt` | Action: [NEW]
  - FR: FR-015
  - Endpoints: GET `/api/v1/notifications/dlq` (paginated), POST `/{id}/retry`, POST `/{id}/discard`
  - Error: `NOTIF-011 DLQ_ENTRY_NOT_FOUND`

### Statistics & Reporting

- [x] **Task 45: StatisticsService**
  - File: `notification/application/StatisticsService.kt` | Action: [NEW]
  - FR: FR-016, FR-017
  - Native SQL: channel × status count, delivery latency P50/P95
  - Methods: `getStats(from, to, channel)`, `getRetryReport(from, to)`

- [x] **Task 46: Statistics repository queries**
  - File: `notification/adapter/out/persistence/repository/NotificationQueueRepository.kt` | Action: [MODIFY]
  - FR: FR-016, FR-017
  - Add native queries: `findStatsByChannelAndDateRange()`, `findRetryStatsByDateRange()`

- [x] **Task 47: StatisticsController**
  - File: `notification/adapter/in/web/admin/StatisticsController.kt` | Action: [NEW]
  - FR: FR-016, FR-017
  - Endpoints: GET `/api/v1/notifications/stats`, GET `/api/v1/notifications/stats/retry`

- [x] **Task 48: Statistics DTOs**
  - File: `notification/adapter/in/web/dto/StatisticsDtos.kt` | Action: [NEW]
  - FR: FR-016, FR-017
  - Classes: `StatisticsResponse`, `ChannelBreakdownResponse`, `RetryReportResponse`

- [x] **Task 49: DLQ + DeviceToken DTOs**
  - File: `notification/adapter/in/web/dto/AdminDtos.kt` | Action: [NEW]
  - FR: FR-015, FR-019
  - Classes: `DlqEntryResponse`, `DlqRetryRequest`, `RegisterDeviceTokenRequest`, `DeviceTokenResponse`

### Integration

- [x] **Task 50: Update EnqueueService DTOs — add sub_channel**
  - File: `notification/adapter/in/web/dto/NotificationDtos.kt` | Action: [MODIFY]
  - FR: FR-018
  - Add: `subChannel: String?` to `EnqueueNotificationRequest` and `NotificationStatusResponse`

- [x] **Task 51: Update EnqueueService — pass sub_channel**
  - File: `notification/application/NotificationEnqueueService.kt` | Action: [MODIFY]
  - FR: FR-018
  - Add: `subChannel` parameter to `enqueue()` method, set on entity

- [x] **Task 52: Update Controller — pass sub_channel**
  - File: `notification/adapter/in/web/NotificationController.kt` | Action: [MODIFY]
  - FR: FR-018
  - Pass `subChannel` from request DTO to enqueue service

### Cross-Cutting

- [x] **Task 53: Circuit Breaker per Channel**
  - File: `src/main/resources/application.yml` | Action: [MODIFY]
  - FR: FR-023
  - Add resilience4j circuit breaker config per channel: `smsCircuitBreaker`, `pushCircuitBreaker`, `ottCircuitBreaker`
  - Pattern: Follow existing `emailCircuitBreaker` config
  - Note: Individual sender implementations (Task 5, 26, 35) already inject `CircuitBreakerRegistry` and create named breakers
