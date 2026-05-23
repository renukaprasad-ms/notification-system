# Infrastructure

Infrastructure files live in the repository root `infra/` folder.

## Compose Files

```text
infra/postgres.compose.yml
infra/redis.compose.yml
infra/backend.compose.yml
infra/frontend.compose.yml
```

Services:

- `postgres`
- `redis`
- `notification-backend`
- `notification-frontend`

Shared Docker network:

```text
notification-network
```

## Dockerfiles

```text
infra/backend.Dockerfile
infra/frontend.Dockerfile
```

Backend Dockerfile:

- Builds the Spring Boot application with the Maven wrapper.
- Runs the packaged jar with a non-root runtime image.

Frontend Dockerfile:

- Builds the Vite frontend image.
- Runs the frontend container with the configured frontend port.

## Provision Script

File:

```text
scripts/provision.sh
```

Commands:

```bash
./scripts/provision.sh up
./scripts/provision.sh up backend
./scripts/provision.sh up redis
./scripts/provision.sh up frontend
./scripts/provision.sh up postgres

./scripts/provision.sh build backend
./scripts/provision.sh restart backend
./scripts/provision.sh logs backend

./scripts/provision.sh status
./scripts/provision.sh down
```

## Environment

Example file:

```text
infra/.env.example
```

Local file:

```text
infra/.env
```

Important backend variables:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `REDIS_HOST`
- `REDIS_PORT`
- `REDIS_ENABLED`
- `REDIS_PUBSUB_ENABLED`
- `ADMIN_NOTIFICATION_RATE_LIMIT`
- `NOTIFICATION_RETRY_ENABLED`

Important frontend variables:

- `FRONTEND_PORT`
- `VITE_API_BASE_URL`

## Current Runtime Design

Local and containerized setup currently works like this:

1. Frontend calls backend REST APIs.
2. Backend persists notifications in PostgreSQL.
3. Backend uses Redis for unread-count cache, rate limits, pub/sub fanout, and retry coordination.
4. Backend delivers live notifications to connected browsers over SSE.

## Kubernetes Direction

Recommended Kubernetes layout:

- One `Deployment` for `notification-backend` with multiple replicas.
- One `Deployment` for `notification-frontend`.
- One `Service` for backend traffic.
- One `Service` for frontend traffic.
- PostgreSQL and Redis provided either by managed services or separate stateful workloads.
- `ConfigMap` for non-secret configuration.
- `Secret` for DB credentials, JWT secret, and mail credentials.

For notification delivery:

1. Backend API writes notification data to PostgreSQL.
2. API returns success after the write succeeds.
3. Backend publishes a delivery event to Redis after commit.
4. Any backend pod with active SSE clients can consume the Redis message.
5. Each pod only pushes to its own connected users.

This is the key reason Redis pub/sub matters in Kubernetes: the user may be connected to pod A, while the admin request is handled by pod B.

Important distinction:

- Redis pub/sub is best for fanout between live backend instances.
- Redis queue or Redis Streams is better when you need durable async work processing.

If the design goal is "frontend gets success immediately, then background workers process delivery later", a queue-style model is more accurate than plain pub/sub.
