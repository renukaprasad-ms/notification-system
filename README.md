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
2. Update `infra/.env` if needed.
3. Start the stack:

```bash
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
- add a formal root AI usage declaration if required separately from this README

## Additional Docs

- [Backend docs](./notification-backend/docs/README.md)
- [Frontend docs](./notification-frontend/README.md)
- [Kubernetes docs](./k8s/README.md)
