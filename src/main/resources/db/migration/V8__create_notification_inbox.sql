-- V8: Create notification inbox for in-app real-time notifications
-- Schema separation: inbox schema for future DB split

CREATE SCHEMA IF NOT EXISTS inbox;

CREATE TABLE inbox.notification_inbox (
    id                      BIGINT PRIMARY KEY,
    user_id                 BIGINT NOT NULL,
    category                VARCHAR(30) NOT NULL,
    title                   VARCHAR(255) NOT NULL,
    body                    TEXT NOT NULL,
    icon_url                VARCHAR(500),
    action_url              VARCHAR(500),
    priority                VARCHAR(10) DEFAULT 'NORMAL',
    metadata                JSONB DEFAULT '{}',
    read                    BOOLEAN DEFAULT FALSE,
    read_at                 TIMESTAMPTZ,
    archived                BOOLEAN DEFAULT FALSE,
    archived_at             TIMESTAMPTZ,
    source_service          VARCHAR(100),
    correlation_id          VARCHAR(100),
    notification_queue_id   BIGINT,
    created_at              TIMESTAMPTZ DEFAULT NOW(),
    updated_at              TIMESTAMPTZ DEFAULT NOW(),
    created_by              VARCHAR(255),
    updated_by              VARCHAR(255),
    active                  BOOLEAN DEFAULT TRUE NOT NULL,
    CONSTRAINT uq_inbox_correlation UNIQUE (correlation_id)
);

-- Partial indexes for performance (only query non-archived records)
CREATE INDEX idx_inbox_user_unread
    ON inbox.notification_inbox (user_id, created_at DESC)
    WHERE read = FALSE AND archived = FALSE;

CREATE INDEX idx_inbox_user_all
    ON inbox.notification_inbox (user_id, created_at DESC)
    WHERE archived = FALSE;

CREATE INDEX idx_inbox_cleanup
    ON inbox.notification_inbox (created_at)
    WHERE archived = FALSE;

CREATE INDEX idx_inbox_queue_id
    ON inbox.notification_inbox (notification_queue_id);
