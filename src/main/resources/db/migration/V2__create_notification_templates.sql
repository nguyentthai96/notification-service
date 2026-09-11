-- Notification templates table — stores templates with placeholder support
CREATE TABLE notification_template (
    id                  BIGINT PRIMARY KEY,
    code                VARCHAR(100) NOT NULL UNIQUE,
    name                VARCHAR(255) NOT NULL,
    channel             VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    subject_template    VARCHAR(500) NOT NULL,
    body_template       TEXT NOT NULL,
    tracking_mode       VARCHAR(10) NOT NULL DEFAULT 'NONE',
    language            VARCHAR(10) NOT NULL DEFAULT 'vi',
    active              BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notif_template_code_active ON notification_template(code, active);
