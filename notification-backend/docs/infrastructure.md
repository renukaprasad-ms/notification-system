# Infrastructure

Infrastructure files live in the repository root `infra/` folder.

## Compose Files

```text
infra/postgres.compose.yml
infra/backend.compose.yml
infra/frontend.compose.yml
```

Services:

- `postgres`
- `notification-backend`
- `notification-frontend`

Shared Docker network:

```text
notification-network
```

Redis is not added yet.

## Dockerfiles

```text
infra/backend.Dockerfile
infra/frontend.Dockerfile
```

Backend Dockerfile:

- Builds Spring Boot app using Maven wrapper.
- Runs app with Eclipse Temurin JRE.
- Uses non-root app user.

Frontend Dockerfile:

- Uses Node Alpine.
- Installs dependencies with `npm ci`.
- Runs Vite dev server on port `5173`.

## Provision Script

File:

```text
scripts/provision.sh
```

Commands:

```bash
./scripts/provision.sh up
./scripts/provision.sh up backend
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

`infra/.env` is ignored by Git and should contain local secrets.

Important backend variables:

- `POSTGRES_DB`
- `POSTGRES_USER`
- `POSTGRES_PASSWORD`
- `JWT_SECRET`
- `SEED_ENABLED`
- `SUPER_ADMIN_EMAIL`
- `SUPER_ADMIN_PASSWORD`
- `SUPER_ADMIN_FULL_NAME`
- `OTP_EXPIRY_MINUTES`
- `OTP_MAX_ATTEMPTS`

Frontend variables:

- `FRONTEND_PORT`
- `VITE_API_BASE_URL`

## Run Locally

From repository root:

```bash
cp infra/.env.example infra/.env
./scripts/provision.sh up
```

Backend:

```text
http://localhost:8080
```

Frontend:

```text
http://localhost:5173
```
