---
type: proposal
change: notification-service-phase2
status: draft
---

# Proposal: Notification Service Phase 2 — Multi-Channel + Multi-Transport

## 1. Overview

Mở rộng notification-service Phase 1 (EMAIL-only, REST + outbox polling) thành multi-channel, multi-transport notification platform.

**Scope:** 21 FRs active, 2 deferred (Zalo OTT).

## 2. Problem Statement

Phase 1 chỉ hỗ trợ gửi email qua SMTP với REST API input. Các hệ thống cần:
- Gửi SMS, push notification, Telegram messages
- Nhận notification request qua gRPC (low latency) và Kafka (event-driven)
- Theo dõi delivery status (sent, delivered, read)
- Quản lý retry failures (DLQ) và báo cáo thống kê

## 3. Proposed Solution

### Architecture Direction
**Incremental Extension** — giữ nguyên kiến trúc Phase 1 (Strategy Pattern + Outbox), thêm:
- Channel senders: TwilioSmsSender, FcmPushSender, TelegramOttSender
- Transport adapters: gRPC server, Kafka consumer
- Tracking: webhook receivers + status updates
- Management: DLQ, retry log, statistics API

### Key Design Decisions

| DD | Decision |
|----|----------|
| DD-001 | OTT sub-routing via OttDispatcher composite |
| DD-002 | Kafka dual JSON+Protobuf (content-type header) |
| DD-003 | Proto in notification-client → mavenLocal |
| DD-004 | Admin CRUD API for device tokens |
| DD-005 | Zalo DEFERRED |
| DD-006 | DLQ + retry_log two-table approach |
| DD-007 | Statistics via native SQL aggregates |
| DD-008 | Per-provider webhook security |
| DD-009 | 21/23 FRs in scope |
| DD-010 | Telegram webhook (prod) / long polling (dev) |
| DD-011 | gRPC reflection dev profile only |
| DD-012 | Kafka + gRPC optional plug-and-play (@ConditionalOnProperty) |

## 4. Scope

### In Scope (21 FRs)

| Category | FRs | Summary |
|----------|-----|---------|
| Channel Senders | FR-001, FR-002, FR-004 | SMS (Twilio), Push (FCM), Telegram OTT |
| Transport | FR-005, FR-006, FR-007 | gRPC server, Kafka consumer, Kafka DLT |
| Client SDK | FR-008, FR-009 | gRPC + Kafka adapters |
| Tracking | FR-010, FR-012 | Twilio webhook, delivery/read timestamps |
| Retry | FR-013, FR-014, FR-015 | DLQ table, retry log, manual retry API |
| Reporting | FR-016, FR-017 | Statistics API, retry report |
| Data Model | FR-018, FR-019, FR-020 | sub_channel, device tokens, external msg ID |
| Domain | FR-021, FR-022, FR-023 | Idempotency, rate limiting, circuit breaker |

### Out of Scope (Deferred)
- FR-003: Zalo OTT sender
- FR-011: Zalo status polling

## 5. Implementation Phases

| Phase | Sprint | Deliverables |
|-------|--------|-------------|
| 2a | 1-2 | SMS (Twilio) + gRPC + Kafka + Client SDK v2 + Rate limiting |
| 2b | 3 | Push (FCM) + Device tokens + Delivery tracking |
| 2c | 4 | OTT (Telegram) + Sub-routing + Telegram webhook |
| 2d | 5 | DLQ + Retry log + Statistics + Reporting |

## 6. Dependencies (New)

| Dependency | Purpose |
|-----------|---------|
| `com.twilio.sdk:twilio:10.6.3` | SMS |
| `com.google.firebase:firebase-admin:9.3.0` | Push |
| `net.devh:grpc-server-spring-boot-starter:3.1.0` | gRPC |
| `io.grpc:grpc-protobuf:1.68.0` | Protobuf |
| `org.springframework.kafka:spring-kafka` | Kafka |
| `spring-boot-starter-webflux` | WebClient for OTT |

## 7. Risks

| Risk | Impact | Mitigation |
|------|--------|-----------|
| Telegram no read receipts | Feature gap | Inline button workaround |
| Kafka infrastructure cost | Ops overhead | Optional — @ConditionalOnProperty |
| gRPC browser limitation | Frontend can't call | Keep REST API for external |

## 8. Success Criteria

- All 21 FRs implemented and tested
- Backward compatible — Phase 1 functionality unchanged
- Each channel independently enable/disable via config
- Kafka + gRPC optional — service runs without them
