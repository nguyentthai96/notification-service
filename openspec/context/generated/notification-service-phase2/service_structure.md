# Service Structure — notification-service

## Package Structure

```
src/main/kotlin/com/ntt/notificationservice/
├── NotificationServiceApplication.kt          # @SpringBootApplication
│
├── notification/                               # Core module (Clean Architecture)
│   ├── adapter/
│   │   ├── in/
│   │   │   └── web/                           # Inbound adapters (REST controllers)
│   │   │       ├── NotificationController.kt
│   │   │       └── dto/
│   │   │           └── NotificationDtos.kt
│   │   └── out/
│   │       ├── persistence/                   # Outbound adapters (JPA)
│   │       │   ├── entity/
│   │       │   │   ├── NotificationQueueEntity.kt
│   │       │   │   └── NotificationTemplateEntity.kt
│   │       │   └── repository/
│   │       │       ├── NotificationQueueRepository.kt
│   │       │       └── NotificationTemplateRepository.kt
│   │       └── sender/                        # Channel senders (Strategy implementations)
│   │           └── SmtpEmailSender.kt
│   │
│   ├── application/                           # Use cases (services)
│   │   ├── NotificationDispatcher.kt
│   │   ├── NotificationEnqueueService.kt
│   │   ├── NotificationJobScheduler.kt
│   │   ├── NotificationRevokeService.kt
│   │   ├── NotificationCleanupScheduler.kt
│   │   ├── TemplateRenderService.kt
│   │   └── port/
│   │       └── out/
│   │           └── NotificationSender.kt      # Strategy interface
│   │
│   ├── config/                                # Configuration
│   │   └── NotificationProperties.kt
│   │
│   └── domain/                                # Domain models (pure)
│       └── model/
│           ├── NotificationChannel.kt
│           ├── NotificationPriority.kt
│           └── NotificationStatus.kt
│
└── shared/                                    # Cross-cutting concerns
    └── exception/
        ├── GlobalExceptionHandler.kt
        ├── NotificationErrorCode.kt
        └── NotificationException.kt
```

## Naming Conventions

| Pattern | Convention | Example |
|---------|-----------|---------|
| Entity | `*Entity` | `NotificationQueueEntity` |
| Repository | `*Repository` | `NotificationQueueRepository` |
| Controller | `*Controller` | `NotificationController` |
| Service | `*Service` | `NotificationEnqueueService` |
| DTO Request | `*Request` | `EnqueueNotificationRequest` |
| DTO Response | `*Response` | `EnqueueNotificationResponse` |
| Sender (Strategy) | `*Sender` (channel prefix) | `SmtpEmailSender` |
| Scheduler | `*Scheduler` | `NotificationJobScheduler` |
| Properties | `*Properties` | `NotificationProperties` |
| Error Code | `*ErrorCode` | `NotificationErrorCode` |
| Exception | `*Exception` | `NotificationException` |
| Handler | `*Handler` | `GlobalExceptionHandler` |

## Architecture Notes

- **Clean Architecture**: adapter → application → domain (dependency inversion)
- **Entity base class**: `SnowflakePersistentAuditableEntity` (base-core) → Snowflake ID + createdAt/updatedAt
- **Config pattern**: `@ConfigurationProperties` with nested data classes
- **Conditional beans**: `@ConditionalOnProperty` per channel
- **Package-per-feature**: `notification/` as single bounded context
- **Shared module**: `shared/exception/` for cross-cutting concerns

## Phase 2 Structure Extension

```
notification/
├── adapter/
│   ├── in/
│   │   ├── web/                         # [EXISTS]
│   │   │   ├── webhook/                 # [NEW] Webhook controllers
│   │   │   │   ├── TwilioWebhookController.kt
│   │   │   │   └── ZaloWebhookController.kt
│   │   │   ├── admin/                   # [NEW] Admin/DLQ/Stats controllers
│   │   │   │   ├── DlqController.kt
│   │   │   │   └── StatisticsController.kt
│   │   │   └── dto/                     # [EXTEND]
│   │   ├── grpc/                        # [NEW] gRPC inbound adapter
│   │   │   └── NotificationGrpcService.kt
│   │   └── kafka/                       # [NEW] Kafka inbound adapter
│   │       └── KafkaNotificationConsumer.kt
│   └── out/
│       ├── sender/                      # [EXTEND] New channel senders
│       │   ├── SmtpEmailSender.kt       # [EXISTS]
│       │   ├── TwilioSmsSender.kt       # [NEW]
│       │   ├── FcmPushSender.kt         # [NEW]
│       │   ├── ZaloOttSender.kt         # [NEW]
│       │   └── TelegramOttSender.kt     # [NEW]
│       └── persistence/                 # [EXTEND]
│           ├── entity/
│           │   ├── NotificationDlqEntity.kt        # [NEW]
│           │   ├── NotificationRetryLogEntity.kt   # [NEW]
│           │   └── DeviceTokenEntity.kt            # [NEW]
│           └── repository/
│               ├── NotificationDlqRepository.kt    # [NEW]
│               ├── NotificationRetryLogRepository.kt  # [NEW]
│               └── DeviceTokenRepository.kt        # [NEW]
```
