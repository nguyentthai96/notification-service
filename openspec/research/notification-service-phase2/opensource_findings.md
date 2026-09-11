# Open Source Findings — Notification Service Phase 2

## 1. Evaluated Projects

### 1.1 Novu (https://github.com/novuhq/novu)
⭐ **35k+ stars** | TypeScript/Node.js | Self-hosted + Cloud

| Criteria | Score (1-5) | Notes |
|----------|------------|-------|
| Multi-channel support | 5 | Email, SMS, Push, In-App, Chat |
| Workflow engine | 5 | Visual workflow builder, digest engine |
| Delivery tracking | 4 | Delivery logs, status tracking |
| Read receipt | 4 | In-app notification feed with read status |
| SDK/API quality | 4 | REST API, multiple SDKs (JS, Python, Java) |
| Self-hosted ease | 3 | Docker Compose, cần MongoDB + Redis + worker |
| Spring Boot compatibility | 2 | TypeScript ecosystem, REST API integration only |
| **Total** | **27/35** | |

**Gap Analysis:**
- ✅ Production-proven multi-channel routing
- ✅ Visual workflow builder
- ✅ In-app notification inbox component
- ❌ TypeScript — không native Spring Boot
- ❌ Cần infrastructure riêng (MongoDB, Redis, Workers)
- ❌ Overhead lớn nếu chỉ cần SMS/Push addition

### 1.2 Apache Camel (https://camel.apache.org/)
⭐ **5k+ stars** | Java | Integration Framework

| Criteria | Score (1-5) | Notes |
|----------|------------|-------|
| Multi-channel support | 5 | 400+ components (SMTP, Twilio, FCM, Telegram) |
| Workflow engine | 4 | EIP routing, content-based routing |
| Delivery tracking | 3 | Tracer, Wiretap pattern |
| Spring Boot compatibility | 5 | First-class Spring Boot starter |
| Learning curve | 2 | DSL phức tạp, heavy abstraction |
| Lightweight | 2 | Heavyweight framework |
| **Total** | **21/30** | |

**Gap Analysis:**
- ✅ Native Java, Spring Boot starter
- ✅ Built-in components cho hầu hết channels
- ✅ Enterprise Integration Patterns (EIP)
- ❌ Quá heavyweight cho use case notification
- ❌ Learning curve cao, DSL phức tạp
- ❌ Overkill nếu chỉ cần thêm SMS sender

### 1.3 Spring Integration (https://spring.io/projects/spring-integration)
⭐ Spring ecosystem | Java | Messaging Framework

| Criteria | Score (1-5) | Notes |
|----------|------------|-------|
| Multi-channel support | 4 | Mail, AMQP, Kafka, TCP/UDP, WebSocket |
| Spring Boot compatibility | 5 | First-class Spring Boot project |
| Lightweight | 4 | Modular, chỉ import cần dùng |
| Delivery tracking | 3 | Message history, Wire Tap |
| Community | 4 | Spring ecosystem, enterprise adoption |
| SMS/Push/OTT | 2 | Không có built-in SMS/FCM/OTT adapters |
| **Total** | **22/30** | |

**Gap Analysis:**
- ✅ Native Spring Boot
- ✅ Channel/MessageHandler pattern quen thuộc
- ✅ Kafka, AMQP integration built-in
- ❌ Vẫn cần custom SMS/FCM/OTT senders
- ❌ Abstraction layer thêm complexity

## 2. SDK/Libraries cho Channels cụ thể

### SMS Providers
| Provider | Library | Gradle | Pricing |
|----------|---------|--------|---------|
| **Twilio** | `com.twilio.sdk:twilio:10.x` | Official Java SDK | ~$0.0075/SMS |
| **Vonage** | `com.vonage:server-sdk:8.x` | Official Java SDK | ~$0.0068/SMS |
| **AWS SNS** | `software.amazon.awssdk:sns` | AWS SDK v2 | ~$0.00645/SMS |

### Push Notification
| Provider | Library | Gradle | Notes |
|----------|---------|--------|-------|
| **Firebase (FCM)** | `com.google.firebase:firebase-admin:9.x` | Official Admin SDK | FCM v1 HTTP API, OAuth2 |
| **OneSignal** | REST API only | HTTP client | Higher abstraction |

### OTT Messaging
| Provider | Library | Integration | Read Receipt |
|----------|---------|-------------|-------------|
| **Zalo OA/ZNS** | REST API | OAuth2, ZBS Template Messages | ✅ Supported (`message/status` API) |
| **Telegram Bot** | REST API (`api.telegram.org`) | BotFather token | ❌ No native read receipts |
| **Viber** | REST API | Official bot API | ✅ Delivery + read status |

### Transport Layer
| Transport | Library | Spring Boot Starter |
|-----------|---------|-------------------|
| **gRPC** | `io.grpc:grpc-spring-boot-starter` | `net.devh:grpc-server-spring-boot-starter:3.x` |
| **Kafka** | `spring-kafka` | `spring-boot-starter` (built-in) |
| **RabbitMQ** | `spring-amqp` | `spring-boot-starter-amqp` |

## 3. Recommendation

### Build (Extend current architecture) — **RECOMMENDED**

**Rationale:**
1. Current Strategy Pattern (`NotificationSender`) đã sẵn sàng cho extension
2. Enum `NotificationChannel` đã có SMS, PUSH, OTT values
3. Thêm sender implementation = thêm Spring Bean, zero code change ở dispatcher
4. Novu quá heavyweight + TypeScript ecosystem mismatch
5. Apache Camel overkill cho use case này
6. Direct SDK integration (Twilio, FCM Admin, Zalo REST) đơn giản và kiểm soát được

**Implementation Strategy:**
```
Phase 2a: SMS Channel (Twilio SDK) + gRPC transport + Kafka consumer
Phase 2b: Push Channel (FCM Admin SDK) + delivery tracking webhook
Phase 2c: OTT Channel (Zalo OA API + Telegram Bot API)
Phase 2d: Reporting dashboard API + retry management enhancement
```
