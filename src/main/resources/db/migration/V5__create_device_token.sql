-- V5: Create notification_device_token table for FCM push notifications
CREATE TABLE notification_device_token (
    id          BIGINT PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    token       VARCHAR(500) NOT NULL UNIQUE,
    platform    VARCHAR(10) NOT NULL,
    active      BOOLEAN NOT NULL DEFAULT TRUE,
    created_by  BIGINT,
    created_at  TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_device_token_user ON notification_device_token(user_id, active);
CREATE INDEX idx_device_token_platform ON notification_device_token(platform, active);
