ALTER TABLE notifications
    ADD COLUMN IF NOT EXISTS request_id VARCHAR(100);

CREATE INDEX IF NOT EXISTS idx_notification_created_by_created_at
    ON notifications (created_by, created_at);

CREATE INDEX IF NOT EXISTS idx_notification_created_by_request_id
    ON notifications (created_by, request_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_created_by_request_id
    ON notifications (created_by, request_id)
    WHERE request_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_notification_recipients_delivery_status_created_at
    ON notification_recipients (delivery_status, created_at);
