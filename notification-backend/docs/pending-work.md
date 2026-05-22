# Pending Work

## Auth

- Add mail sender provider for OTP delivery.
- Stop returning raw OTP from login OTP API after mail delivery is added.
- Stop returning raw password-reset OTP after mail delivery is added.
- Add CORS config for React frontend with credentials.
- Decide whether signup should require OTP before setting cookies.

## User

- Add user service if user profile operations are needed.
- Add admin-only user management APIs only if required.

## Notification

- Add optional unviewed-count API if frontend needs it.

## Realtime

- Support same user connected from multiple devices.
- Make delivery work across horizontally scaled backend instances.
- Prevent duplicate deliveries.

## Redis

- Add Redis container.
- Add unread-count cache.
- Add rate limiting.
- Add graceful behavior when Redis is unavailable.

## Reliability

- Add retry worker for failed notification delivery.
- Add idempotency strategy for notification creation.
- Add structured logging.
- Add tests for auth, OTP, and notification flows.

## Infrastructure

- Add Kubernetes manifests.
- Add production profile config.
- Replace `ddl-auto=update` with migrations before production.
