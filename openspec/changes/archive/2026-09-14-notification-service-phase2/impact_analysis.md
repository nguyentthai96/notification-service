---
type: impact_analysis
change: notification-service-phase2
status: draft
---

# Impact Analysis: Notification Service Phase 2

## 1. Core Files Affected

| File | Action | Risk | FRs |
|------|--------|------|-----|
| `NotificationQueueEntity.kt` | MODIFY (add 2 columns) | 🟢 Low | FR-018, FR-020 |
| `NotificationErrorCode.kt` | MODIFY (add 10 enums) | 🟢 Low | Multiple |
| `NotificationProperties.kt` | MODIFY (add nested configs) | 🟢 Low | FR-001/002/004/005/006 |
| `NotificationEnqueueService.kt` | MODIFY (add subChannel param + rate limit) | 🟡 Medium | FR-018, FR-022 |
| `NotificationJobScheduler.kt` | MODIFY (add DLQ + retry log writes) | 🟡 Medium | FR-013, FR-014 |
| `NotificationQueueRepository.kt` | MODIFY (add queries) | 🟢 Low | FR-010, FR-016 |
| `NotificationController.kt` | MODIFY (pass subChannel) | 🟢 Low | FR-018 |
| `NotificationDtos.kt` | MODIFY (add subChannel field) | 🟢 Low | FR-018 |
| `build.gradle.kts` | MODIFY (add dependencies) | 🟢 Low | Multiple |
| `application.yml` | MODIFY (add config sections) | 🟢 Low | Multiple |

## 2. Call Tree (Modified Files)

```
NotificationEnqueueService.enqueue()
  ├── Called by: NotificationController.enqueue()      ← MODIFY (pass subChannel)
  ├── Called by: NotificationGrpcService.enqueue()      ← NEW (delegates to same service)
  ├── Called by: KafkaNotificationConsumer.consume()     ← NEW (delegates to same service)
  ├── Calls: RateLimitService.checkRateLimit()           ← NEW (inject + call)
  └── Calls: NotificationQueueRepository.save()          ← NO CHANGE

NotificationJobScheduler.handleFailure()
  ├── Calls: NotificationQueueRepository.save()          ← NO CHANGE
  ├── Calls: NotificationDlqRepository.save()            ← NEW (inject + call)
  └── Calls: NotificationRetryLogRepository.save()       ← NEW (inject + call)
```

## 3. Blast Radius

| Depth | Symbol | Impact |
|-------|--------|--------|
| d=0 | NotificationEnqueueService | Target — add subChannel + rate limit |
| d=1 | NotificationController | 🟢 Low — pass-through change |
| d=1 | notification-client (JpaNotificationAdapter) | 🟡 Medium — may need subChannel param |
| d=0 | NotificationJobScheduler.handleFailure | Target — add DLQ + retry log |
| d=1 | (no external callers) | 🟢 Low — private method |

**Overall blast radius: 🟡 MEDIUM** — 2 modified services methods, both internal with limited callers.

## 4. Reuse Map

| Symbol | Match | Decision | Source |
|--------|-------|----------|--------|
| SmtpEmailSender pattern | 100% | REUSE pattern | adapter/out/sender/SmtpEmailSender.kt |
| CircuitBreaker pattern | 100% | REUSE pattern | SmtpEmailSender.circuitBreaker |
| @ConditionalOnProperty | 100% | REUSE pattern | SmtpEmailSender annotation |
| NotificationSender interface | 100% | REUSE interface | application/port/out/NotificationSender.kt |
| SnowflakePersistentAuditableEntity | 100% | REUSE base class | base-core library |
| NotificationException pattern | 100% | REUSE pattern | shared/exception/ |
| Dedup by correlationId | 100% | REUSE logic | NotificationEnqueueService.enqueue() |

**No EXTRACT needed** — all reuse is pattern-level (copy convention, not shared code).

## 5. Context Snapshot

### Existing Architecture (Phase 1)
- **Strategy Pattern:** `NotificationSender` → `NotificationDispatcher` auto-discovers via Spring DI
- **Outbox Pattern:** `notification_queue` table, `NotificationJobScheduler` with `SELECT FOR UPDATE SKIP LOCKED`
- **Resilience:** `Resilience4j CircuitBreaker` per sender
- **Conditional:** `@ConditionalOnProperty` per channel
- **Base entity:** `SnowflakePersistentAuditableEntity` (Snowflake ID + audit timestamps)

### Extension Points (Zero-Change Core)
- Add new `NotificationSender` bean → auto-registered in dispatcher
- Add new `@ConditionalOnProperty` → enable/disable per config
- Add new columns to entity → JPA auto-maps
- Add new inbound adapter → delegates to existing `EnqueueService`

### Risk Summary
- **No breaking changes** — all modifications are additive
- **Backward compatible** — Phase 1 functionality preserved
- **Optional transports** — Kafka + gRPC disabled by default
