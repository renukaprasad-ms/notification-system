# Database Schema

The primary database is PostgreSQL.

JPA validation is configured with:

```yaml
spring.jpa.hibernate.ddl-auto: validate
```

Schema creation and updates are handled through migration files under:

```text
src/main/resources/db/migration
```

## Core Tables

### users

- Stores user identity and account state.
- Includes `is_email_verified` and `is_active`.

### roles

- Stores role definitions.
- Current values include `USER` and `ADMIN`.

### user_roles

- Maps users to roles.
- Composite primary key prevents duplicate assignments.

### otp_verifications

- Stores hashed OTP values.
- Tracks expiry, attempts, verification, and consumption.

## Notification Tables

### notifications

Stores the notification payload itself:

- `title`
- `message`
- `type`
- `priority`
- `request_id`
- `created_by`
- `created_at`

`request_id` supports idempotent admin notification creation.

### notification_recipients

Stores one delivery row per target user:

- `notification_id`
- `user_id`
- `delivery_status`
- `delivered_at`
- `viewed_at`
- `read_at`
- `created_at`

Important constraint:

- Unique key on `notification_id, user_id`

That unique key helps prevent duplicate recipient rows for the same notification and user.

### notification_delivery_attempts

Tracks delivery audit and retry history:

- `notification_recipient_id`
- `channel`
- `status`
- `attempt_number`
- `error_message`
- `attempted_at`

## Query and Scaling Notes

- User notification listing is paginated in the database, not in Java memory.
- Recipient tables are indexed for user timeline queries and unread checks.
- Send-to-all uses a database-side insert-select for active users to avoid loading every user row into application memory.
- Redis is used for fanout and caching, but PostgreSQL remains the source of truth.
