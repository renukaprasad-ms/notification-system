# Kubernetes Manifests

This folder contains a stronger Kubernetes deployment shape for the realtime notification system.

## What Is Included

- `namespace.yaml`
- `configmap.yaml`
- `secrets.example.yaml`
- `postgres.yaml`
- `redis.yaml`
- `backend.yaml`
- `backend-hpa.yaml`
- `backend-pdb.yaml`
- `frontend.yaml`
- `frontend-pdb.yaml`
- `ingress.yaml`
- `kind-config.yaml`

## Production-Oriented Changes

- Backend runs as a rolling `Deployment` with 2 replicas.
- Backend has resource requests and limits.
- Backend has an `HPA` for automatic scale-out on CPU usage.
- Backend has a `PodDisruptionBudget`.
- PostgreSQL runs as a `StatefulSet` with persistent volume claims.
- Redis runs as a `StatefulSet` with persistent volume claims.
- Frontend runs as a built static site behind Nginx instead of Vite dev server.
- Ingress includes Nginx annotations to support SSE properly.

## Important Runtime Idea

This system is still designed around:

1. Backend persists notifications in PostgreSQL.
2. Backend returns API success after persistence succeeds.
3. Backend uses Redis pub/sub to fan out live delivery across backend pods.
4. Browser clients stay connected to backend pods over SSE.

If backend pod A receives the admin request and the user is connected to backend pod B, Redis lets pod B still deliver the live event.

## Before You Run

You need:

- A Kubernetes cluster running locally.
- `kubectl`
- Docker

Good local options:

- `minikube`
- `kind`
- Docker Desktop Kubernetes

You also need real image builds. Kubernetes will not build the app images for you.

## Images To Build

Backend image:

```bash
docker build -t notification-backend:latest -f infra/backend.Dockerfile .
```

Frontend production image:

```bash
docker build -t notification-frontend:latest -f infra/frontend.prod.Dockerfile .
```

You can also use the provision script:

```bash
./scripts/provision.sh k8s-build-images
```

## Local Cluster Run

### Option 1: Minikube

Point Docker to Minikube's Docker daemon:

```bash
eval $(minikube docker-env)
```

Then build the two images:

```bash
docker build -t notification-backend:latest -f infra/backend.Dockerfile .
docker build -t notification-frontend:latest -f infra/frontend.prod.Dockerfile .
```

Enable ingress:

```bash
minikube addons enable ingress
```

### Option 2: Kind

Create the cluster with localhost port mappings:

```bash
./scripts/provision.sh k8s-kind-create
```

Install the ingress controller:

```bash
./scripts/provision.sh k8s-install-ingress
```

Build images locally:

```bash
docker build -t notification-backend:latest -f infra/backend.Dockerfile .
docker build -t notification-frontend:latest -f infra/frontend.prod.Dockerfile .
```

Load them into the cluster:

```bash
kind load docker-image notification-backend:latest
kind load docker-image notification-frontend:latest
```

Or:

```bash
./scripts/provision.sh k8s-load-kind
```

## Configure Secrets

Copy:

```text
k8s/secrets.example.yaml
```

to:

```text
k8s/secrets.yaml
```

Then replace all example secret values with real ones.

## Values You Should Adjust

In `configmap.yaml`:

- `CORS_ALLOWED_ORIGINS`
- `MAIL_FROM`
- `SUPER_ADMIN_EMAIL`

In `ingress.yaml`:

- the default host is `notification.localhost`

For ingress-based local setup, the frontend should use the backend through the same host:

- frontend host: `http://notification.localhost/`
- backend API: `http://notification.localhost/api`

The frontend code now defaults to `/api`, which is the right production-style behavior behind ingress.

## Apply Order

```bash
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secrets.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/backend.yaml
kubectl apply -f k8s/backend-pdb.yaml
kubectl apply -f k8s/backend-hpa.yaml
kubectl apply -f k8s/frontend.yaml
kubectl apply -f k8s/frontend-pdb.yaml
kubectl apply -f k8s/ingress.yaml
```

Or use the provision script:

```bash
./scripts/provision.sh k8s-up
```

With the `kind` config and ingress controller installed, you can open:

```text
http://notification.localhost
```

## Verify

Check pods:

```bash
kubectl get pods -n notification-system
```

Check services:

```bash
kubectl get svc -n notification-system
```

Check ingress:

```bash
kubectl get ingress -n notification-system
```

Check autoscaler:

```bash
kubectl get hpa -n notification-system
```

Or:

```bash
./scripts/provision.sh k8s-status
```

## Notes

- These manifests are much closer to production than the first draft, but real production usually uses managed PostgreSQL and Redis.
- Backend probes are currently TCP-based because the application does not expose Spring Boot actuator health endpoints yet.
- The frontend production image uses Nginx and built static files, which is the right shape for deployment.
