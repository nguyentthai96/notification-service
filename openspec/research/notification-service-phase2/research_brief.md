# Research Brief — Notification Service Phase 2

## 1. Feature Overview

**Feature name:** Notification Service Phase 2 — Multi-Channel + Multi-Transport Expansion
**Input mode:** Name + Description (from user request)
**Phase 1 status:** ✅ Completed (EMAIL channel, REST API, outbox polling, notification-client SDK)

### Scope Phase 2

| Category | Features |
|----------|----------|
| **New Channels** | SMS (Twilio/Vonage), Firebase Cloud Messaging (Push), OTT (Zalo/Telegram) |
| **New Transports** | gRPC server, Apache Kafka consumer/producer pub-sub |
| **Enhanced Retry** | Dead Letter Queue, configurable retry policies per channel, retry dashboard |
| **Reporting** | Message delivery statistics, success/failure rates per channel, dashboard API |
| **Read Tracking** | User view status (READ receipts), webhook callbacks, delivery confirmation |
| **Revoke Enhancement** | Unsend/revoke across all channels, batch revoke |
| **Client SDK Enhancement** | Support multi-channel enqueue, gRPC client adapter, Kafka producer adapter |

## 2. Keywords & Search Queries

### Primary Keywords
- multi-channel notification service
- SMS notification Spring Boot Twilio
- Firebase Cloud Messaging FCM v1 Spring Boot
- notification delivery tracking read receipt
- gRPC notification service
- Kafka notification pub/sub
- OTT messaging API Zalo Telegram
- notification retry dead letter queue
- notification reporting dashboard metrics

### Search Queries (Executed)
1. "Spring Boot notification service SMS integration Twilio Vonage multi-channel architecture 2024 2025"
2. "Firebase Cloud Messaging Spring Boot push notification FCM v1 HTTP API Java Kotlin integration"
3. "Spring Boot gRPC notification service Kafka consumer producer event-driven notification system"
4. "open source notification service Novu NotifMe Apprise multi-channel comparison 2024 2025"
5. "notification delivery tracking read receipt webhook callback OTT Zalo Telegram API"
6. "notification service retry strategy dead letter queue exponential backoff reporting dashboard metrics"

## 3. Research Objectives

1. Xác định integration pattern chuẩn cho từng channel (SMS, Push, OTT)
2. Đánh giá gRPC vs REST vs Kafka cho notification ingestion
3. So sánh open-source notification platforms (Novu, Apprise, NotifMe)
4. Thiết kế delivery tracking & read receipt system
5. Thiết kế retry management với DLQ + reporting dashboard
6. Xác định notification-client SDK extension cho multi-transport

## 4. Current System Analysis

### 4.1 Phase 1 Architecture (Existing)

```
Producer Service          notification-client SDK          notification-service
(auth-service)            (JPA adapter)                    (Spring Boot)
     │                         │                                │
     ├─ NotificationPort ──────┤                                │
     │   .enqueue(request)     │── Native SQL INSERT ──────────►│ notification_queue table
     │                         │   (same DB transaction)        │
     │                         │                                │
     │                         │                    ┌───────────┤
     │                         │                    │ NotificationJobScheduler
     │                         │                    │ (SELECT FOR UPDATE SKIP LOCKED)
     │                         │                    │           │
     │                         │                    │   NotificationDispatcher
     │                         │                    │   (Strategy Pattern)
     │                         │                    │           │
     │                         │                    │   SmtpEmailSender
     │                         │                    │   (Resilience4j CircuitBreaker)
     │                         │                    └───────────┘
```

### 4.2 Existing Patterns
| Pattern | Implementation |
|---------|---------------|
| Strategy Pattern | `NotificationSender` interface → `NotificationDispatcher` auto-discovers beans |
| Transactional Outbox | `SELECT FOR UPDATE SKIP LOCKED` polling |
| Exponential Backoff | `baseRetryDelaySeconds × 2^retryCount` |
| Circuit Breaker | Resilience4j on `SmtpEmailSender` |
| Client SDK | `notification-client` with `NotificationPort` + `JpaNotificationAdapter` |
| Status Lifecycle | PENDING → PROCESSING → SENT → DELIVERED → READ / FAILED / REVOKED / CANCELLED |

### 4.3 Tech Stack
| Component | Technology |
|-----------|-----------|
| Language | Kotlin |
| Framework | Spring Boot 3.x |
| Database | PostgreSQL |
| ORM | Spring Data JPA |
| Migration | Flyway |
| Resilience | Resilience4j |
| Build | Gradle (Kotlin DSL) |
| Tracing | Micrometer + Brave |
| Metrics | Prometheus |

### 4.4 Extension Points (Ready for Phase 2)
- `NotificationChannel` enum already has `SMS`, `PUSH`, `OTT` values
- `ChannelProperties` already has `sms`, `push`, `ott` config (enabled: false)
- `NotificationSender` strategy pattern allows adding new senders as Spring beans
- `NotificationStatus` enum already has `DELIVERED`, `READ`, `REVOKED` states
