# API Reference

Base backend URL in local Docker:

```text
http://localhost:8080
```

All responses use the shared `ApiResponse` wrapper.

Success shape:

```json
{
  "status": true,
  "status_code": 200,
  "data": {},
  "message": "success message"
}
```

Error shape:

```json
{
  "status": false,
  "status_code": 400,
  "error_message": "error message"
}
```

## Auth APIs

Available auth endpoints:

- `POST /api/auth/signup`
- `POST /api/auth/login`
- `POST /api/auth/login/otp`
- `POST /api/auth/refresh`
- `POST /api/auth/logout`
- `POST /api/auth/forgot-password/otp`
- `POST /api/auth/reset-password`

Important current behavior:

- JWT auth uses HttpOnly cookies.
- OTP values are stored hashed in the database.
- OTP endpoints do not return raw OTP values.
- OTP login and password-reset OTP require working mail configuration.
- Signup currently issues auth cookies immediately after user creation.

## User APIs

### Get Current User

```http
GET /api/auth/me
```

Returns the authenticated user's profile.

### Admin User List

```http
GET /api/users/admin/all?page=0&size=20&search=value
```

Returns a paginated user list for admins.

Notes:

- Search runs in the database.
- Results are sorted by newest user first.

## Notification APIs

### Send Notification To All Active Users

```http
POST /api/notifications/send-all
```

Admin only.

Behavior:

1. Validate request and admin access.
2. Persist notification data.
3. Create recipient rows for active users.
4. Return success after persistence succeeds.
5. Publish delivery after commit.

Required request fields:

- `title`
- `message`
- `type`
- `priority`
- optional `requestId` for idempotency

### Send Notification To Selected Users

```http
POST /api/notifications/send-selected
```

Admin only.

Behavior is similar to send-all, but recipients are validated from the provided user id list.

Required request fields:

- `title`
- `message`
- `type`
- `priority`
- `recipientUserIds`
- optional `requestId` for idempotency

### Get My Notifications

```http
GET /api/notifications/me?page=0&size=20&search=value
```

Returns paginated notifications for the authenticated user.

Notes:

- Pagination is handled in the database.
- Search supports title, message, type, and priority.
- Search is case-insensitive.

### Mark Notification Viewed

```http
PATCH /api/notifications/{recipientId}/viewed
```

### Mark Notification Read

```http
PATCH /api/notifications/{recipientId}/read
```

### Get Unread Count

```http
GET /api/notifications/me/unread-count
```

### Get Admin Overview

```http
GET /api/notifications/admin/overview
```

Admin only.

## Realtime API

### SSE Stream

```http
GET /api/notifications/stream?clientId=<client-id>
```

Returns a Server-Sent Events stream for the authenticated user.

Notes:

- `clientId` identifies a specific browser tab or device session.
- Multiple simultaneous connections are supported for the same user.
- SSE uses the same cookie-based authentication as the REST APIs.

## Notification Delivery Model

Current model:

1. Frontend sends create-notification request.
2. Backend persists notification and recipients.
3. Backend responds with success.
4. After commit, backend publishes the notification through Redis pub/sub when enabled.
5. Connected instances push live updates through SSE.

Recommended production interpretation:

- Treat Redis as the async delivery bus.
- Treat PostgreSQL as the source of truth.
- Keep the API response dependent on persistence success, not on live delivery success.
