# Database Schema

The database is PostgreSQL.

JPA currently uses:

```yaml
spring.jpa.hibernate.ddl-auto: update
```

This is convenient during development. For production, migrations should be handled through a migration tool such as Flyway or Liquibase.

## users

Source:

```text
user/entity/User.java
```

Columns:

- `id UUID PRIMARY KEY`
- `email VARCHAR(255) UNIQUE NOT NULL`
- `password_hash VARCHAR(255) NOT NULL`
- `full_name VARCHAR(150)`
- `is_email_verified BOOLEAN NOT NULL`
- `is_active BOOLEAN NOT NULL`
- `created_at TIMESTAMP NOT NULL`
- `updated_at TIMESTAMP NOT NULL`

Notes:

- There is no phone number field.
- Passwords are stored as hashes only.

## roles

Source:

```text
user/entity/Role.java
```

Columns:

- `id UUID PRIMARY KEY`
- `name VARCHAR(50) UNIQUE NOT NULL`
- `description VARCHAR(255)`
- `created_at TIMESTAMP NOT NULL`

Current role values:

- `USER`
- `ADMIN`

## user_roles

Source:

```text
user/entity/UserRole.java
user/entity/UserRoleId.java
```

Columns:

- `user_id UUID NOT NULL`
- `role_id UUID NOT NULL`
- `created_at TIMESTAMP NOT NULL`

Primary key:

```text
(user_id, role_id)
```

Notes:

- Prevents duplicate role assignments.
- Admin is represented as a role mapping.

## otp_verifications

Source:

```text
auth/otp/OtpVerification.java
```

Columns:

- `id UUID PRIMARY KEY`
- `user_id UUID NULL`
- `destination VARCHAR(255) NOT NULL`
- `channel VARCHAR(20) NOT NULL`
- `purpose VARCHAR(30) NOT NULL`
- `otp_hash VARCHAR(255) NOT NULL`
- `expires_at TIMESTAMP NOT NULL`
- `verified_at TIMESTAMP NULL`
- `consumed_at TIMESTAMP NULL`
- `attempt_count INTEGER NOT NULL`
- `max_attempts INTEGER NOT NULL`
- `created_at TIMESTAMP NOT NULL`

Indexes:

- `idx_otp_destination_purpose`
- `idx_otp_expires_at`

Notes:

- OTP value is hashed.
- `destination` is currently email.
- OTP is consumed after successful verification.

## notifications

Source:

```text
notification/entity/Notification.java
```

Columns:

- `id UUID PRIMARY KEY`
- `title VARCHAR(150) NOT NULL`
- `message TEXT NOT NULL`
- `type VARCHAR(50) NOT NULL`
- `priority VARCHAR(20) NOT NULL`
- `created_by UUID NULL`
- `created_at TIMESTAMP NOT NULL`

Current type values:

- `SYSTEM`
- `ACCOUNT`
- `SECURITY`
- `PROMOTION`

Current priority values:

- `LOW`
- `NORMAL`
- `HIGH`
- `URGENT`

## notification_recipients

Source:

```text
notification/entity/NotificationRecipient.java
```

Columns:

- `id UUID PRIMARY KEY`
- `notification_id UUID NOT NULL`
- `user_id UUID NOT NULL`
- `delivered_at TIMESTAMP NULL`
- `viewed_at TIMESTAMP NULL`
- `read_at TIMESTAMP NULL`
- `delivery_status VARCHAR(30) NOT NULL`
- `created_at TIMESTAMP NOT NULL`

Constraints:

- Unique `notification_id, user_id`

Indexes:

- `idx_notification_recipient_user_created`
- `idx_notification_recipient_user_read`
- `idx_notification_recipient_user_viewed`

Notes:

- `viewed_at` tracks whether the user has seen the notification.
- `read_at` tracks whether the user has read/acted on it.
- Unique recipient constraint prevents duplicate notification rows for the same user.

## notification_delivery_attempts

Source:

```text
notification/entity/NotificationDeliveryAttempt.java
```

Columns:

- `id UUID PRIMARY KEY`
- `notification_recipient_id UUID NOT NULL`
- `channel VARCHAR(30) NOT NULL`
- `status VARCHAR(30) NOT NULL`
- `attempt_number INTEGER NOT NULL`
- `error_message TEXT NULL`
- `attempted_at TIMESTAMP NOT NULL`

Current channel values:

- `IN_APP`
- `EMAIL`
- `SMS`

Current attempt status values:

- `SUCCESS`
- `FAILED`

Notes:

- This table is for retry tracking and delivery audit history.
- Current implementation writes an attempt row for every in-app SSE delivery result.
- Failed rows can be used later by a retry worker.
