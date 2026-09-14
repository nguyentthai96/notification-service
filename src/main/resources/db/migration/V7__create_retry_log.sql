-- V7: Create retry log table for tracking retry attempts
CREATE TABLE notification_retry_log (
    id                  BIGINT PRIMARY KEY,
    notification_id     BIGINT NOT NULL REFERENCES notification_queue(id),
    attempt_number      INT NOT NULL,
    error_code          VARCHAR(50),
    error_message       TEXT,
    attempted_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_retry_log_notification ON notification_retry_log(notification_id);
CREATE INDEX idx_retry_log_attempted ON notification_retry_log(attempted_at);
