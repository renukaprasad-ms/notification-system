# Backend Documentation

This folder documents the current backend implementation for the realtime notification system.

## Documents

- [Project Context](./context.md)
- [Implemented Modules](./modules.md)
- [API Reference](./api.md)
- [Database Schema](./database-schema.md)
- [Infrastructure](./infrastructure.md)

## Current Backend Status

The backend currently includes:

- JWT cookie-based authentication.
- OTP-based login and password reset flows.
- Role-based authorization with admin-only notification endpoints.
- Notification persistence in PostgreSQL.
- User notification list API with database pagination.
- Mark viewed and mark read APIs.
- SSE-based live notification streaming.
- Redis-based unread-count caching.
- Redis-based rate limiting.
- Redis pub/sub support for cross-instance notification fanout.
- Retry worker for failed notification delivery attempts.
- Idempotency support for admin notification creation.
- A dedicated `test` profile so `./mvnw test` can boot without production environment variables.
- Integration tests for auth cookies, login validation, notification inbox flow, unread count, and mark-read behavior.

## Recommended Reading Order

1. [API Reference](./api.md)
2. [Implemented Modules](./modules.md)
3. [Database Schema](./database-schema.md)
4. [Infrastructure](./infrastructure.md)

## Notification Flow

Current implementation:

1. Admin creates a notification through the backend API.
2. Backend writes the notification and recipient rows to PostgreSQL.
3. Backend returns success after persistence succeeds.
4. After transaction commit, notification delivery is attempted through Redis pub/sub when enabled.
5. If Redis pub/sub is unavailable, the backend falls back to local SSE publishing.
6. Delivery results are recorded for retry and audit history.

Recommended production direction:

1. Persist notification and recipient rows first.
2. Return API success immediately after the database transaction commits.
3. Publish a lightweight event to Redis after commit.
4. Let connected backend instances consume that event and deliver through SSE.
5. Keep retry logic focused on recipients still marked `PENDING` or `FAILED`.

That design keeps the write path fast for the frontend while delivery stays asynchronous.
