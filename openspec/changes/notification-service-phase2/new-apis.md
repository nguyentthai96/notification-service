# New APIs — notification-service-phase2

## REST Endpoints

| Method | Path | Purpose | FR |
|--------|------|---------|-----|
| POST | `/api/v1/webhooks/twilio` | Twilio SMS delivery callback | FR-010 |
| POST | `/api/v1/webhooks/telegram` | Telegram bot webhook | FR-004 |
| GET | `/api/v1/notifications/stats` | Delivery statistics | FR-016 |
| GET | `/api/v1/notifications/stats/retry` | Retry failure report | FR-017 |
| GET | `/api/v1/notifications/dlq` | List DLQ entries (paginated) | FR-015 |
| POST | `/api/v1/notifications/dlq/{id}/retry` | Re-enqueue DLQ entry | FR-015 |
| POST | `/api/v1/notifications/dlq/{id}/discard` | Discard DLQ entry | FR-015 |
| POST | `/api/v1/admin/device-tokens` | Register device token | FR-019 |
| GET | `/api/v1/admin/device-tokens/{userId}` | List tokens by user | FR-019 |
| PUT | `/api/v1/admin/device-tokens/{id}` | Update device token | FR-019 |
| DELETE | `/api/v1/admin/device-tokens/{id}` | Deactivate token | FR-019 |

## gRPC Endpoints (Optional)

| Service | Method | Purpose | FR |
|---------|--------|---------|-----|
| NotificationService | Enqueue | Enqueue notification via gRPC | FR-005 |

## Kafka Topics (Optional)

| Topic | Direction | Purpose | FR |
|-------|-----------|---------|-----|
| notification-inbound | Consumer | Receive notification events | FR-006 |
| notification-inbound-dlt | Consumer (DLT) | Dead letter topic | FR-007 |
