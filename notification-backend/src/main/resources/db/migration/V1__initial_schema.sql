CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(150),
    is_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS roles (
    id UUID PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id),
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles (id)
);

CREATE TABLE IF NOT EXISTS otp_verifications (
    id UUID PRIMARY KEY,
    user_id UUID,
    destination VARCHAR(255) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    otp_hash VARCHAR(255) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP,
    consumed_at TIMESTAMP,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_otp_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_otp_destination_purpose ON otp_verifications (destination, purpose);
CREATE INDEX IF NOT EXISTS idx_otp_expires_at ON otp_verifications (expires_at);

CREATE TABLE IF NOT EXISTS notifications (
    id UUID PRIMARY KEY,
    title VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    created_by UUID,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_notifications_created_by FOREIGN KEY (created_by) REFERENCES users (id)
);

CREATE TABLE IF NOT EXISTS notification_recipients (
    id UUID PRIMARY KEY,
    notification_id UUID NOT NULL,
    user_id UUID NOT NULL,
    delivered_at TIMESTAMP,
    viewed_at TIMESTAMP,
    read_at TIMESTAMP,
    delivery_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT uk_notification_recipient_user UNIQUE (notification_id, user_id),
    CONSTRAINT fk_notification_recipients_notification FOREIGN KEY (notification_id) REFERENCES notifications (id),
    CONSTRAINT fk_notification_recipients_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient_user_created ON notification_recipients (user_id, created_at);
CREATE INDEX IF NOT EXISTS idx_notification_recipient_user_read ON notification_recipients (user_id, read_at);
CREATE INDEX IF NOT EXISTS idx_notification_recipient_user_viewed ON notification_recipients (user_id, viewed_at);

CREATE TABLE IF NOT EXISTS notification_delivery_attempts (
    id UUID PRIMARY KEY,
    notification_recipient_id UUID NOT NULL,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    attempt_number INTEGER NOT NULL,
    error_message TEXT,
    attempted_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_delivery_attempt_recipient FOREIGN KEY (notification_recipient_id) REFERENCES notification_recipients (id)
);

CREATE INDEX IF NOT EXISTS idx_delivery_attempt_recipient ON notification_delivery_attempts (notification_recipient_id);
CREATE INDEX IF NOT EXISTS idx_delivery_attempt_status ON notification_delivery_attempts (status);
