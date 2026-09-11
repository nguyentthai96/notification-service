# Web Research — Notification Service Phase 2

## 1. Search Iterations

### Iteration 1: Multi-channel SMS integration
- **Query:** "Spring Boot notification service SMS integration Twilio Vonage multi-channel architecture 2024 2025"
- **Key findings:**
  - Strategy Pattern + Message Queue (Kafka/RabbitMQ) là standard architecture
  - Twilio Java SDK official (`com.twilio.sdk:twilio`) cho SMS
  - Provider abstraction quan trọng — tránh vendor lock-in
  - Virtual Threads (Java 21+) cải thiện I/O-heavy notification workers
  - Rate limiting per provider là must-have
- **Sources:** Medium, Dev.to, Substack, StackAdemic

### Iteration 2: Firebase Cloud Messaging integration
- **Query:** "Firebase Cloud Messaging Spring Boot push notification FCM v1 HTTP API Java Kotlin integration"
- **Key findings:**
  - Firebase Admin SDK (`com.google.firebase:firebase-admin:9.x`) là recommended approach
  - FCM v1 API tự động handle OAuth2 access tokens
  - Service Account JSON file cho authentication
  - `FirebaseMessaging.getInstance().send(message)` hoặc `sendAsync` cho async
  - Notification vs Data messages — data messages cho custom background processing
- **Sources:** Viblo, Medium, Firebase docs, StackAdemic

### Iteration 3: gRPC + Kafka architecture
- **Query:** "Spring Boot gRPC notification service Kafka consumer producer event-driven notification"
- **Key findings:**
  - Hybrid model: gRPC cho synchronous + Kafka cho async event propagation
  - `@KafkaListener` consume events → enqueue vào notification_queue
  - Protobuf serialization cho cả gRPC và Kafka messages
  - DLQ (Dead Letter Queue) cho failed message handling
  - Idempotency key là critical — track processed eventIds trong Redis
- **Sources:** Spring.io, Dev.to, DZone

### Iteration 4: Open source platforms
- **Query:** "open source notification service Novu NotifMe Apprise multi-channel comparison 2024 2025"
- **Key findings:**
  - Novu: Full infrastructure (35k+ stars) nhưng TypeScript ecosystem
  - Apprise: Lightweight, 120+ integrations nhưng cho system alerts/home automation
  - NotifMe: Legacy, Node.js, thiếu features hiện đại
  - **Conclusion:** Self-build extend current architecture, không adopt external platform
- **Sources:** Reddit, GitHub, APISCout, Medium

### Iteration 5: Delivery tracking & OTT
- **Query:** "notification delivery tracking read receipt webhook callback OTT Zalo Telegram API"
- **Key findings:**
  - **Zalo OA/ZNS:** Có delivery + read status via `message/status` API. Từ 01/2026 chuyển sang ZBS Template Messages
  - **Telegram Bot:** Không có native read receipts (privacy-focused). Workaround: inline buttons/callback queries
  - **Email:** Pixel tracking (1x1 image) hoặc link click tracking — cả hai đều optional
  - Webhook mechanism phổ biến cho real-time status updates
- **Sources:** Zalo Developer, Telegram docs, Stack Overflow

### Iteration 6: Retry & reporting
- **Query:** "notification service retry strategy dead letter queue exponential backoff reporting dashboard metrics"
- **Key findings:**
  - Exponential backoff + Jitter là standard retry strategy
  - Kafka `@RetryableTopic` cho non-blocking retries
  - DLQ pattern: sau max retries → route to dedicated DLQ topic/table
  - Phân biệt transient vs permanent errors — chỉ retry transient
  - Monitoring: Micrometer + Prometheus + Grafana
  - RED metrics: Rate, Errors, Duration
  - OpenTelemetry cho distributed tracing
- **Sources:** Spring.io, Dev.to, DZone, Medium

## 2. Products/Tools Evaluated

| Product | Type | Pros | Cons |
|---------|------|------|------|
| **Twilio** | SMS API | Reliable, global coverage, official Java SDK | Pricing per SMS |
| **Firebase FCM** | Push API | Free, official Admin SDK, cross-platform | Google ecosystem dependency |
| **Zalo OA** | OTT API | Vietnam market leader, delivery tracking | Regional (Vietnam only), ZBS migration |
| **Telegram Bot** | OTT API | Global, free, simple API | No read receipts, bot limitations |
| **gRPC** | Transport | Low latency, type-safe Protobuf contracts | Setup complexity, browser limitations |
| **Apache Kafka** | Transport | Scalable, durable, built-in retries | Infrastructure overhead, operational cost |
| **Prometheus+Grafana** | Monitoring | Industry standard, free | Needs hosting/infra |

## 3. Unique Sources: 18 sources across 6 iterations
