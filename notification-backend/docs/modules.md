# Implemented Modules

## Common Module

Package:

```text
com.renuka.notification_backend.common
```

Provides:

- Common success/error response wrappers.
- Global exception handling.
- Redis helpers for rate limiting and distributed locking.

## User Module

Package:

```text
com.renuka.notification_backend.user
```

Provides:

- User, role, and user-role entities.
- Current-user profile API.
- Admin user listing API with search and pagination.

## Auth Module

Package:

```text
com.renuka.notification_backend.auth
```

Provides:

- Signup.
- Password login.
- OTP login.
- Refresh token flow.
- Logout.
- Forgot-password OTP generation.
- Password reset.

Notes:

- JWT access and refresh tokens are stored in HttpOnly cookies.
- OTP values are stored hashed in the database.
- OTP APIs do not return raw OTP values.
- OTP delivery requires mail configuration and fails explicitly when email delivery is not configured.

## Security Module

Package:

```text
com.renuka.notification_backend.security
```

Provides:

- JWT parsing and validation.
- Cookie-based authentication filter.
- Method-level security for role checks.
- CORS configuration with credentials support.

## Notification Module

Package:

```text
com.renuka.notification_backend.notification
```

Provides:

- Admin send-to-all API.
- Admin send-to-selected-users API.
- User notification list API with database pagination and search.
- Mark viewed API.
- Mark read API.
- Admin overview API.
- Notification and recipient persistence.
- Idempotent request handling through `requestId`.

## Realtime Module

Package:

```text
com.renuka.notification_backend.notification.realtime
```

Provides:

- SSE subscription per authenticated user and client id.
- Multiple simultaneous device connections per user.
- Local in-memory emitter management per backend instance.
- Redis pub/sub fanout support for horizontal scaling.

Current delivery shape:

1. Persist notification.
2. Persist recipient rows.
3. Return success from the API.
4. Publish delivery after commit.
5. Push live updates to connected SSE clients.

## Redis Integration

Provides:

- Unread count caching.
- Notification admin rate limiting.
- Pub/sub event fanout.
- Retry-worker lock coordination.

Redis failure behavior:

- Unread count falls back to database reads.
- Pub/sub falls back to local in-process publish.
- Application should continue serving core APIs even if Redis is unavailable.

## Reliability Module

Provides:

- Delivery attempt tracking.
- `PENDING` and `FAILED` status handling.
- Scheduled retry worker for failed or missed deliveries.

## Config Module

Provides:

- Manual Flyway migration execution before JPA startup.
- Redis serialization and subscriber wiring.
- Seed data for roles and super admin.
- Test profile configuration for isolated backend boot checks.
