# Realtime Notification System

A scalable realtime notification system built with Spring Boot, PostgreSQL, Redis, React, Docker Compose, and Kubernetes.

## Objective

Build a production-oriented notification system that supports:

- notification creation
- realtime delivery
- unread count tracking
- retry for failed delivery
- horizontal scaling considerations

## Tech Stack

Backend:

- Spring Boot
- Spring Security
- Spring Data JPA
- PostgreSQL
- Redis
- Flyway-style migration flow

Frontend:

- React
- Vite
- Axios
- Tailwind CSS

Infra:

- Docker Compose
- Kubernetes manifests

## Core Features

- JWT cookie-based authentication
- login with password or OTP
- forgot-password OTP flow
- admin-only notification send APIs
- user notification inbox with pagination and search
- mark viewed and mark read APIs
- SSE-based realtime notification stream
- unread count caching in Redis
- Redis-based rate limiting
- retry worker for failed notification delivery
- idempotent notification creation with `requestId`

## Submission Status

This repository is organized as a submission-ready full-stack project:

- backend, frontend, and infrastructure live in one repository
- architecture, API, schema, and infrastructure docs are included
- AI usage is declared in [AI_USAGE.md](./AI_USAGE.md)
- frontend production checks pass
- backend tests boot with a dedicated `test` profile

## Architecture Summary

The system uses PostgreSQL as the source of truth and Redis as the realtime coordination layer.

High-level flow:

1. Admin creates a notification.
2. Backend persists the notification and recipient rows in PostgreSQL.
3. Backend returns success after persistence succeeds.
4. Notification delivery is dispatched asynchronously after commit.
5. Redis pub/sub is used for cross-instance fanout when multiple backend pods are running.
6. Connected clients receive events through SSE.
7. Failed or missed deliveries stay trackable through recipient status and retry records.

This keeps the API path fast while still supporting live delivery and retry behavior.

## Request Lifecycle

1. Admin sends a notification request.
2. Backend validates admin access and request payload.
3. Notification and recipient rows are persisted in PostgreSQL.
4. API success is returned after persistence succeeds.
5. Delivery dispatch happens after transaction commit.
6. Redis pub/sub fans out events across backend instances when enabled.
7. SSE connections push updates to connected browser tabs or devices.
8. Redis caches unread counts, with database fallback when Redis is unavailable.

## Why SSE

SSE was chosen because the main requirement is server-to-client push, not full duplex messaging.

Benefits here:

- simpler than WebSocket for one-way notifications
- browser-native client support
- good fit for live inbox updates
- easier operational model for this use case

## Project Structure

```text
notification-backend/   Spring Boot backend
notification-frontend/  React frontend
infra/                  Dockerfiles and Docker Compose files
k8s/                    Kubernetes manifests
scripts/                Helper scripts
```

## Run Locally With Docker Compose

1. Make sure Docker is running.
2. Create `infra/.env` from `infra/.env.example`.
3. Update the values if needed.
4. Start the stack:

```bash
cp infra/.env.example infra/.env
./scripts/provision.sh up
```

Useful commands:

```bash
./scripts/provision.sh status
./scripts/provision.sh logs backend
./scripts/provision.sh restart backend
./scripts/provision.sh down
```

Default local URLs:

- frontend: `http://localhost:5173`
- backend: `http://localhost:8080`

Default seeded admin credentials:

- email: `admin@example.com`
- password: `Admin@12345`

If you changed `SUPER_ADMIN_EMAIL` or `SUPER_ADMIN_PASSWORD` in `infra/.env` or Kubernetes secrets/config, use those values instead.

## Test And Build Checks

Backend:

```bash
cd notification-backend
./mvnw test
```

Frontend:

```bash
cd notification-frontend
npm run lint
npm run build
```

## Demo Checklist

1. Login as the seeded admin account.
2. Open the notifications page in one or more browser tabs.
3. Send a notification from the admin page.
4. Confirm the notification appears live through SSE.
5. Confirm unread count updates immediately.
6. Search notifications by title, message, type, or priority.
7. Mark notifications viewed and read.
8. Refresh and confirm the persisted state remains correct.

## Local Environment Notes

Important variables:

- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `JWT_SECRET`
- `REDIS_ENABLED`, `REDIS_PUBSUB_ENABLED`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`
- `VITE_API_BASE_URL`

OTP login and password-reset OTP require mail configuration. The backend returns an explicit error if email delivery is not configured.

## Run With Kubernetes

Kubernetes requires:

- `kubectl`
- a running cluster such as `kind`, `minikube`, or Docker Desktop Kubernetes

Build images:

```bash
./scripts/provision.sh k8s-build-images
```

If using `kind`, load images:

```bash
./scripts/provision.sh k8s-load-kind
```

Create:

```text
k8s/secrets.yaml
```

from:

```text
k8s/secrets.example.yaml
```

Then deploy:

```bash
./scripts/provision.sh k8s-up
```

Check status:

```bash
./scripts/provision.sh k8s-status
```

More detail is in [k8s/README.md](./k8s/README.md).

## Realtime and Scaling Design

This project is designed with horizontal scaling in mind.

Key idea:

- a user may be connected to backend pod A
- an admin request may hit backend pod B
- Redis pub/sub lets backend pod B publish an event that backend pod A can still deliver through SSE

That is the core mechanism that keeps realtime behavior working across multiple backend instances.

## Current Production-Oriented Choices

- database-backed notification persistence
- database pagination instead of in-memory pagination
- Redis fallback behavior for cache reads
- async notification dispatch for large fanout
- retry worker for failed deliveries
- Kubernetes manifests with Deployments, StatefulSets, HPA, and PDBs
- frontend production image served by Nginx

## Known Next Improvements

- add automated tests for auth, notification, and retry flows
- expose actuator health endpoints for stronger Kubernetes probes
- move very large fanout delivery to an outbox or queue model such as Redis Streams

## Additional Docs

- [Backend docs](./notification-backend/docs/README.md)
- [Frontend docs](./notification-frontend/README.md)
- [Kubernetes docs](./k8s/README.md)
