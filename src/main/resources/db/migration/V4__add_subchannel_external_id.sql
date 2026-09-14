-- V4: Add sub_channel for OTT routing and external_message_id for delivery tracking
ALTER TABLE notification_queue ADD COLUMN sub_channel VARCHAR(20);
ALTER TABLE notification_queue ADD COLUMN external_message_id VARCHAR(255);

CREATE INDEX idx_notif_queue_ext_msg_id ON notification_queue(external_message_id);
CREATE INDEX idx_notif_queue_sub_channel ON notification_queue(sub_channel);
