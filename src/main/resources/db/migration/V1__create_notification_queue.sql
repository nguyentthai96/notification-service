-- Notification queue table — transactional outbox for async notification delivery
-- Uses SnowflakePersistentAuditableEntity base columns (id, created_at, updated_at)
CREATE TABLE notification_queue (
    id                  BIGINT PRIMARY KEY,
    correlation_id      VARCHAR(100) UNIQUE,
    channel             VARCHAR(20) NOT NULL,
    priority            VARCHAR(10) NOT NULL DEFAULT 'NORMAL',
    recipient           VARCHAR(255) NOT NULL,
    subject             VARCHAR(500),
    template_code       VARCHAR(100) NOT NULL,
    template_data       JSONB NOT NULL DEFAULT '{}',
    body_rendered       TEXT,
    status              VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count         INT NOT NULL DEFAULT 0,
    max_retries         INT NOT NULL DEFAULT 3,
    error_message       TEXT,
    sent_at             TIMESTAMP,
    delivered_at        TIMESTAMP,
    read_at             TIMESTAMP,
    revoked_at          TIMESTAMP,
    revoke_reason       VARCHAR(500),
    next_retry_at       TIMESTAMP,
    source_service      VARCHAR(100),
    created_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Performance indexes for scheduled polling
CREATE INDEX idx_notif_queue_status_retry ON notification_queue(status, next_retry_at);
CREATE INDEX idx_notif_queue_channel_status ON notification_queue(channel, status);
CREATE INDEX idx_notif_queue_created ON notification_queue(created_at);
CREATE INDEX idx_notif_queue_correlation ON notification_queue(correlation_id);
CREATE INDEX idx_notif_queue_source ON notification_queue(source_service);
