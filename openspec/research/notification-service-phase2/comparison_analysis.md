# Comparison Analysis — Notification Service Phase 2

## 1. Channel Provider Comparison Matrix

### SMS Providers

| Feature | Twilio | Vonage | AWS SNS |
|---------|--------|--------|---------|
| Java/Kotlin SDK | ✅ Official | ✅ Official | ✅ AWS SDK v2 |
| Spring Boot Integration | ⭐⭐⭐⭐ | ⭐⭐⭐ | ⭐⭐⭐⭐ |
| Delivery Reports | ✅ Webhook | ✅ Webhook | ✅ CloudWatch |
| Vietnam Coverage | ✅ | ✅ | ✅ |
| Pricing (per SMS) | ~$0.0075 | ~$0.0068 | ~$0.00645 |
| Two-way SMS | ✅ | ✅ | ⚠️ Limited |
| **Recommendation** | ✅ Primary | Backup | Cloud-native option |

### Push Notification Providers

| Feature | Firebase FCM | OneSignal | AWS SNS Push |
|---------|-------------|-----------|-------------|
| Admin SDK | ✅ Official Java | REST only | ✅ AWS SDK |
| Free Tier | ✅ Unlimited | ✅ Limited | ⚠️ Pay per request |
| Delivery Tracking | ✅ Firebase Analytics | ✅ Dashboard | ✅ CloudWatch |
| Topic Messaging | ✅ | ✅ | ✅ |
| Cross-platform | ✅ iOS/Android/Web | ✅ | ✅ |
| **Recommendation** | ✅ Primary | Alternative | Cloud-native |

### OTT Messaging

| Feature | Zalo OA/ZBS | Telegram Bot | Viber Bot |
|---------|------------|-------------|-----------|
| Vietnam Market | ✅ #1 | ⚠️ Growing | ⚠️ Niche |
| Delivery Status | ✅ API | ❌ | ✅ API |
| Read Receipt | ✅ | ❌ (privacy) | ✅ |
| Template Messages | ✅ ZBS | ✅ (HTML/Markdown) | ✅ |
| Two-way Chat | ✅ | ✅ | ✅ |
| SDK | REST API | REST API | REST API |
| **Recommendation** | ✅ Primary (VN) | ✅ Secondary | Deferred |

## 2. Transport Layer Comparison

| Feature | REST API (current) | gRPC | Kafka |
|---------|-------------------|------|-------|
| Latency | Medium | Low (~10x faster) | Async (eventual) |
| Type Safety | OpenAPI/Swagger | ✅ Protobuf (strict) | ✅ Protobuf/Avro |
| Streaming | ❌ | ✅ Bidirectional | ✅ Pub/Sub |
| Durability | ❌ (request lost if fail) | ❌ (request lost if fail) | ✅ Persistent log |
| Scalability | ⭐⭐⭐ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐⭐ |
| Browser Support | ✅ Full | ⚠️ gRPC-Web | ❌ |
| Retry Built-in | ❌ (app-level) | ❌ (app-level) | ✅ (@RetryableTopic, DLQ) |
| Use Case | External API, client SDK | Inter-service sync | Event-driven, high-throughput |

### Recommendation: Multi-Transport Strategy
```
REST API  → External clients, notification-client SDK, admin UI
gRPC      → Inter-service communication (low latency, type-safe)
Kafka     → Event-driven ingestion, high-throughput, durability
```

## 3. Retry Strategy Comparison

| Strategy | DB Polling (current) | Kafka @RetryableTopic | Spring Retry |
|----------|---------------------|----------------------|-------------|
| Durability | ✅ DB-backed | ✅ Kafka-backed | ❌ In-memory |
| Non-blocking | ✅ SKIP LOCKED | ✅ Separate topics | ❌ Blocks thread |
| Backoff | ✅ Exponential | ✅ Configurable | ✅ @Backoff |
| DLQ | ⚠️ Manual (FAILED status) | ✅ Built-in -dlt topic | ⚠️ @Recover |
| Visibility | ⚠️ SQL queries | ⚠️ Kafka tooling | ❌ Logs only |
| **Verdict** | Keep for outbox | Add for Kafka transport | Add for external API calls |

### Recommendation: Hybrid retry
- **DB polling** (existing): Keep for outbox pattern — guaranteed delivery
- **Kafka DLQ** (new): Add for Kafka ingestion path
- **Resilience4j** (existing): Keep for external API circuit breaking
- **Dedicated retry table**: New `notification_retry_log` for tracking all retry attempts

## 4. Gap Analysis Summary

| Gap | Current (Phase 1) | Required (Phase 2) | Effort |
|-----|-------------------|-------------------|--------|
| SMS Channel | ❌ | ✅ Twilio SDK | Medium |
| Push Channel | ❌ | ✅ FCM Admin SDK | Medium |
| OTT Channel | ❌ | ✅ Zalo + Telegram REST | High |
| gRPC Transport | ❌ | ✅ grpc-spring-boot-starter | Medium |
| Kafka Transport | ❌ | ✅ spring-kafka | Medium |
| Delivery Tracking | ⚠️ Status enum only | ✅ Webhook receivers | Medium |
| Read Receipt | ⚠️ Status enum only | ✅ Per-channel tracking | High |
| Retry Dashboard | ❌ | ✅ REST API + metrics | Low |
| Reporting API | ❌ | ✅ Statistics endpoints | Low |
| Client SDK (gRPC) | ❌ | ✅ gRPC client stub | Medium |
| Client SDK (Kafka) | ❌ | ✅ KafkaTemplate adapter | Low |
