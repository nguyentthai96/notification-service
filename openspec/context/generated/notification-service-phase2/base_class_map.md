# Base Class Map — notification-service

## Application Layer

| Class | Type | File | Notes |
|-------|------|------|-------|
| `NotificationSender` | Interface (Strategy) | `notification/application/port/out/NotificationSender.kt` | Extension point — new channels implement this |
| `NotificationDispatcher` | @Component (Router) | `notification/application/NotificationDispatcher.kt` | Auto-discovers `NotificationSender` beans via constructor injection |
| `NotificationJobScheduler` | @Component (Scheduler) | `notification/application/NotificationJobScheduler.kt` | Polls queue → dispatcher. `@Scheduled` + `@Transactional` |
| `NotificationEnqueueService` | @Service | `notification/application/NotificationEnqueueService.kt` | Validates + inserts into queue. Called by controller + SDK |
| `NotificationRevokeService` | @Service | `notification/application/NotificationRevokeService.kt` | Revoke + retry logic |
| `TemplateRenderService` | @Service | `notification/application/TemplateRenderService.kt` | Thymeleaf template rendering |
| `NotificationCleanupScheduler` | @Component | `notification/application/NotificationCleanupScheduler.kt` | Scheduled cleanup of old processed notifications |

## Adapter Layer

### Inbound (Web)
| Class | Type | File | Notes |
|-------|------|------|-------|
| `NotificationController` | @RestController | `notification/adapter/in/web/NotificationController.kt` | REST API: enqueue, status, retry, revoke |

### Outbound (Sender)
| Class | Type | File | Notes |
|-------|------|------|-------|
| `SmtpEmailSender` | @Component (NotificationSender) | `notification/adapter/out/sender/SmtpEmailSender.kt` | Phase 1 EMAIL sender. `@ConditionalOnProperty`, Resilience4j CircuitBreaker |

### Outbound (Persistence)
| Class | Type | File | Notes |
|-------|------|------|-------|
| `NotificationQueueEntity` | @Entity | `notification/adapter/out/persistence/entity/NotificationQueueEntity.kt` | JPA entity — extends `SnowflakePersistentAuditableEntity` (base-core) |
| `NotificationTemplateEntity` | @Entity | `notification/adapter/out/persistence/entity/NotificationTemplateEntity.kt` | Template storage |
| `NotificationQueueRepository` | Interface (JpaRepository) | `notification/adapter/out/persistence/repository/NotificationQueueRepository.kt` | Custom query: `findPendingForProcessing` (SELECT FOR UPDATE SKIP LOCKED) |
| `NotificationTemplateRepository` | Interface (JpaRepository) | `notification/adapter/out/persistence/repository/NotificationTemplateRepository.kt` | `findByCodeAndActiveTrue` |

## Shared Layer

| Class | Type | File | Notes |
|-------|------|------|-------|
| `GlobalExceptionHandler` | @ControllerAdvice | `shared/exception/GlobalExceptionHandler.kt` | Extends `BaseControllerAdvice` (base-core) → ProblemDetail RFC 7807 |
| `NotificationException` | Exception | `shared/exception/NotificationException.kt` | Extends `BusinessException` (base-core) |
| `NotificationErrorCode` | Enum | `shared/exception/NotificationErrorCode.kt` | 7 error codes with bridge to `ErrorCodeBase` |

## Domain Layer

| Class | Type | File | Notes |
|-------|------|------|-------|
| `NotificationChannel` | Enum | `notification/domain/model/NotificationChannel.kt` | EMAIL, SMS, PUSH, OTT |
| `NotificationStatus` | Enum | `notification/domain/model/NotificationStatus.kt` | 8 states with documented transitions |
| `NotificationPriority` | Enum | `notification/domain/model/NotificationPriority.kt` | LOW, NORMAL, HIGH, URGENT |

## Configuration

| Class | Type | File | Notes |
|-------|------|------|-------|
| `NotificationProperties` | @ConfigurationProperties | `notification/config/NotificationProperties.kt` | `app.notification.*` — queue, channels, mail |

## Extension Pattern for Phase 2

To add a new channel sender (e.g. SMS):
1. Create `TwilioSmsSender` implementing `NotificationSender`
2. Annotate with `@Component` + `@ConditionalOnProperty(prefix = "app.notification.channels.sms", name = ["enabled"], havingValue = "true")`
3. Return `NotificationChannel.SMS` from `channel()` method
4. `NotificationDispatcher` auto-discovers via constructor injection — **ZERO CHANGE** needed
