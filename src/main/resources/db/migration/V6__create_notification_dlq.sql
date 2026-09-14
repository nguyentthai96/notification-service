-- V6: Create DLQ table for failed notifications
CREATE TABLE notification_dlq (
    id                  BIGINT PRIMARY KEY,
    notification_id     BIGINT NOT NULL REFERENCES notification_queue(id),
    channel             VARCHAR(20) NOT NULL,
    error_code          VARCHAR(50),
    error_message       TEXT,
    original_payload    JSONB,
    resolved            BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_by         BIGINT,
    resolved_at         TIMESTAMP,
    created_by          BIGINT,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_dlq_resolved ON notification_dlq(resolved);
CREATE INDEX idx_dlq_notification ON notification_dlq(notification_id);
CREATE INDEX idx_dlq_channel ON notification_dlq(channel, resolved);
