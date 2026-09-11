# Integration Map — notification-service

## Existing Integrations (Phase 1)

### SMTP (Email)
| Property | Value |
|----------|-------|
| **Client** | `org.springframework.mail.javamail.JavaMailSender` |
| **Class** | `SmtpEmailSender` (`notification/adapter/out/sender/SmtpEmailSender.kt`) |
| **Protocol** | SMTP/SMTPS |
| **Config** | `spring.mail.*` in application.yml |
| **Resilience** | `CircuitBreaker("emailCircuitBreaker")` via Resilience4j |
| **Direction** | Outbound (send email) |

### base-core Library
| Property | Value |
|----------|-------|
| **Artifact** | `com.ntt:base-core` |
| **Classes Used** | `SnowflakePersistentAuditableEntity`, `BaseControllerAdvice`, `BusinessException`, `ErrorCodeBase` |
| **Direction** | Library dependency (inherited) |

## Phase 2 — New Integrations

### Twilio (SMS)
| Property | Value |
|----------|-------|
| **SDK** | `com.twilio.sdk:twilio:10.6.3` |
| **Class** | `TwilioSmsSender` [NEW] |
| **Protocol** | HTTPS (REST API) |
| **Auth** | Account SID + Auth Token |
| **Config** | `app.notification.sms.twilio.*` |
| **Callback** | `POST /api/v1/webhooks/twilio` (StatusCallback URL) |
| **Resilience** | CircuitBreaker (follow SmtpEmailSender pattern) |
| **Direction** | Outbound (send) + Inbound (webhook) |

### Firebase Cloud Messaging (Push)
| Property | Value |
|----------|-------|
| **SDK** | `com.google.firebase:firebase-admin:9.3.0` |
| **Class** | `FcmPushSender` [NEW] |
| **Protocol** | HTTP/2 (FCM v1 API via Admin SDK) |
| **Auth** | Service Account JSON (OAuth2 auto-handled by SDK) |
| **Config** | `app.notification.push.fcm.credentials-file` |
| **Resilience** | CircuitBreaker |
| **Direction** | Outbound (send push) |

### Zalo Official Account (OTT)
| Property | Value |
|----------|-------|
| **SDK** | REST API (WebClient) |
| **Class** | `ZaloOttSender` [NEW] |
| **Base URL** | `https://business.openapi.zalo.me` |
| **Auth** | OAuth2 (App ID + Secret Key → access_token) |
| **Config** | `app.notification.ott.zalo.*` |
| **Callback** | `POST /api/v1/webhooks/zalo` (event webhook) |
| **Status API** | `GET /message/status` (polling) |
| **Direction** | Outbound (send + poll) + Inbound (webhook) |

### Telegram Bot API (OTT)
| Property | Value |
|----------|-------|
| **SDK** | REST API (WebClient) |
| **Class** | `TelegramOttSender` [NEW] |
| **Base URL** | `https://api.telegram.org/bot{token}` |
| **Auth** | Bot Token (from BotFather) |
| **Config** | `app.notification.ott.telegram.bot-token` |
| **Webhook** | `setWebhook` → `POST /api/v1/webhooks/telegram` |
| **Direction** | Outbound (send) + Inbound (webhook) |

### Apache Kafka (Transport)
| Property | Value |
|----------|-------|
| **Library** | `org.springframework.kafka:spring-kafka` |
| **Class** | `KafkaNotificationConsumer` [NEW] |
| **Topic** | `notification-inbound` |
| **Consumer Group** | `notification-service-group` |
| **Serialization** | JSON (initially), Protobuf (future) |
| **DLT** | `notification-inbound-dlt` (Dead Letter Topic) |
| **Config** | `spring.kafka.*` |
| **Direction** | Inbound (consume events) |

### gRPC (Transport)
| Property | Value |
|----------|-------|
| **Library** | `net.devh:grpc-server-spring-boot-starter:3.1.0` |
| **Class** | `NotificationGrpcService` [NEW] |
| **Proto** | `notification_service.proto` |
| **Port** | 9090 |
| **Config** | `grpc.server.port` |
| **Direction** | Inbound (receive enqueue requests) |
