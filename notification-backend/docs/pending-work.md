# Pending Work

## Submission

- Add a root project `README.md` with architecture and setup instructions.
- Add an explicit AI usage declaration for submission.
- Add Kubernetes manifests for backend, frontend, Redis, and PostgreSQL.

## Auth

- Replace development OTP exposure with real mail-only delivery.
- Decide whether signup should require OTP verification before issuing auth cookies.
- Add automated tests for password login, OTP login, refresh, logout, and reset-password flows.

## Notification

- Add automated tests for notification creation, pagination, unread count, SSE delivery, and retry behavior.
- Consider moving fully to an event-first delivery flow where API success is returned immediately after persistence and delivery is always handled asynchronously after commit.
- Add optional metrics for queue lag, delivery success rate, retry count, and connected SSE clients.

## Realtime and Scaling

- Add stronger duplicate-delivery protection for reconnect and retry edge cases.
- Add operational limits for very high fanout notifications.
- Add observability for multi-instance SSE delivery behavior.

## Infrastructure

- Add production-ready profiles and deployment guidance.
- Add health/readiness probes for Kubernetes.
- Add central logging and metrics wiring.
