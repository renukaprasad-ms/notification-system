#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/infra/.env"
K8S_DIR="$ROOT_DIR/k8s"
K8S_NAMESPACE="notification-system"
POSTGRES_COMPOSE="$ROOT_DIR/infra/postgres.compose.yml"
REDIS_COMPOSE="$ROOT_DIR/infra/redis.compose.yml"
BACKEND_COMPOSE="$ROOT_DIR/infra/backend.compose.yml"
FRONTEND_COMPOSE="$ROOT_DIR/infra/frontend.compose.yml"
BACKEND_IMAGE="notification-backend:latest"
FRONTEND_IMAGE="notification-frontend:latest"

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/provision.sh <command> [service]

Commands:
  up [all|postgres|redis|backend|frontend]       Start services
  build [all|postgres|redis|backend|frontend]    Build or pull service images
  restart [all|postgres|redis|backend|frontend]  Restart services
  logs [all|postgres|redis|backend|frontend]     Follow service logs
  stop [all|postgres|redis|backend|frontend]     Stop services
  down                            Stop and remove infra containers
  status                          Show running infra services
  k8s-build-images                Build Kubernetes app images locally
  k8s-kind-create                 Create a kind cluster configured for ingress on localhost
  k8s-install-ingress             Install the NGINX ingress controller into the cluster
  k8s-up                          Apply Kubernetes manifests
  k8s-down                        Delete Kubernetes manifests
  k8s-status                      Show Kubernetes workload status
  k8s-load-kind                   Load built images into a kind cluster

Examples:
  ./scripts/provision.sh up
  ./scripts/provision.sh build backend
  ./scripts/provision.sh logs postgres
  ./scripts/provision.sh restart backend
  ./scripts/provision.sh k8s-build-images
  ./scripts/provision.sh k8s-kind-create
  ./scripts/provision.sh k8s-install-ingress
  ./scripts/provision.sh k8s-up
USAGE
}

ensure_env_file() {
  if [[ ! -f "$ENV_FILE" ]]; then
    echo "Missing infra/.env"
    echo "Create it from infra/.env.example and update the secrets:"
    echo "  cp infra/.env.example infra/.env"
    exit 1
  fi
}

ensure_k8s_secret_file() {
  if [[ ! -f "$K8S_DIR/secrets.yaml" ]]; then
    echo "Missing k8s/secrets.yaml"
    echo "Create it from k8s/secrets.example.yaml and update the secret values:"
    echo "  cp k8s/secrets.example.yaml k8s/secrets.yaml"
    exit 1
  fi
}

ensure_kubectl() {
  if ! command -v kubectl >/dev/null 2>&1; then
    echo "kubectl is required for Kubernetes commands." >&2
    exit 1
  fi
}

ensure_kind() {
  if ! command -v kind >/dev/null 2>&1; then
    echo "kind is required for this command." >&2
    exit 1
  fi
}

build_k8s_images() {
  docker build -t "$BACKEND_IMAGE" -f "$ROOT_DIR/infra/backend.Dockerfile" "$ROOT_DIR"
  docker build -t "$FRONTEND_IMAGE" -f "$ROOT_DIR/infra/frontend.prod.Dockerfile" "$ROOT_DIR"
}

apply_k8s_manifests() {
  kubectl apply -f "$K8S_DIR/namespace.yaml"
  kubectl apply -f "$K8S_DIR/configmap.yaml"
  kubectl apply -f "$K8S_DIR/secrets.yaml"
  kubectl apply -f "$K8S_DIR/postgres.yaml"
  kubectl apply -f "$K8S_DIR/redis.yaml"
  kubectl apply -f "$K8S_DIR/backend.yaml"
  kubectl apply -f "$K8S_DIR/backend-pdb.yaml"
  kubectl apply -f "$K8S_DIR/backend-hpa.yaml"
  kubectl apply -f "$K8S_DIR/frontend.yaml"
  kubectl apply -f "$K8S_DIR/frontend-pdb.yaml"
  kubectl apply -f "$K8S_DIR/ingress.yaml"
}

rollout_restart_k8s_apps() {
  kubectl rollout restart deployment/notification-backend -n "$K8S_NAMESPACE"
  kubectl rollout restart deployment/notification-frontend -n "$K8S_NAMESPACE"
  kubectl rollout status deployment/notification-backend -n "$K8S_NAMESPACE" --timeout=240s
  kubectl rollout status deployment/notification-frontend -n "$K8S_NAMESPACE" --timeout=240s
}

delete_k8s_manifests() {
  kubectl delete -f "$K8S_DIR/ingress.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/frontend-pdb.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/frontend.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/backend-hpa.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/backend-pdb.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/backend.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/redis.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/postgres.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/secrets.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/configmap.yaml" --ignore-not-found
  kubectl delete -f "$K8S_DIR/namespace.yaml" --ignore-not-found
}

show_k8s_status() {
  kubectl get pods -n "$K8S_NAMESPACE"
  kubectl get svc -n "$K8S_NAMESPACE"
  kubectl get ingress -n "$K8S_NAMESPACE"
  kubectl get hpa -n "$K8S_NAMESPACE"
}

load_kind_images() {
  kind load docker-image "$BACKEND_IMAGE"
  kind load docker-image "$FRONTEND_IMAGE"
}

create_kind_cluster() {
  kind create cluster --config "$K8S_DIR/kind-config.yaml"
}

install_ingress_nginx() {
  kubectl apply -f https://raw.githubusercontent.com/kubernetes/ingress-nginx/main/deploy/static/provider/kind/deploy.yaml
  kubectl wait --namespace ingress-nginx \
    --for=condition=ready pod \
    --selector=app.kubernetes.io/component=controller \
    --timeout=180s
}

compose_files_for() {
  local service="${1:-all}"

  case "$service" in
    all)
      echo "-f $POSTGRES_COMPOSE -f $REDIS_COMPOSE -f $BACKEND_COMPOSE -f $FRONTEND_COMPOSE"
      ;;
    postgres)
      echo "-f $POSTGRES_COMPOSE"
      ;;
    redis)
      echo "-f $REDIS_COMPOSE"
      ;;
    backend)
      echo "-f $POSTGRES_COMPOSE -f $REDIS_COMPOSE -f $BACKEND_COMPOSE"
      ;;
    frontend)
      echo "-f $FRONTEND_COMPOSE"
      ;;
    *)
      echo "Unknown service: $service" >&2
      usage
      exit 1
      ;;
  esac
}

compose_service_name_for() {
  local service="${1:-all}"

  case "$service" in
    postgres)
      echo "postgres"
      ;;
    redis)
      echo "redis"
      ;;
    backend)
      echo "notification-backend"
      ;;
    frontend)
      echo "notification-frontend"
      ;;
    all)
      echo ""
      ;;
    *)
      echo "Unknown service: $service" >&2
      usage
      exit 1
      ;;
  esac
}

compose() {
  local service="${1:-all}"
  shift || true

  # shellcheck disable=SC2046
  docker compose --env-file "$ENV_FILE" $(compose_files_for "$service") "$@"
}

command="${1:-}"
service="${2:-all}"

if [[ -z "$command" || "$command" == "-h" || "$command" == "--help" ]]; then
  usage
  exit 0
fi

case "$command" in
  up|build|restart|logs|stop|down|status)
    ensure_env_file
    ;;
esac

case "$command" in
  up)
    if [[ "$service" == "backend" ]]; then
      compose "$service" up -d postgres redis notification-backend
    elif [[ "$service" == "all" ]]; then
      compose "$service" up -d
    else
      compose "$service" up -d "$(compose_service_name_for "$service")"
    fi
    ;;
  build)
    if [[ "$service" == "all" ]]; then
      compose "$service" build
    else
      compose "$service" build "$(compose_service_name_for "$service")"
    fi
    ;;
  restart)
    if [[ "$service" == "all" ]]; then
      compose "$service" restart
    else
      compose "$service" restart "$(compose_service_name_for "$service")"
    fi
    ;;
  logs)
    if [[ "$service" == "all" ]]; then
      compose "$service" logs -f
    else
      compose "$service" logs -f "$(compose_service_name_for "$service")"
    fi
    ;;
  stop)
    if [[ "$service" == "all" ]]; then
      compose "$service" stop
    else
      compose "$service" stop "$(compose_service_name_for "$service")"
    fi
    ;;
  down)
    compose all down
    ;;
  status)
    compose all ps
    ;;
  k8s-build-images)
    build_k8s_images
    ;;
  k8s-kind-create)
    ensure_kind
    create_kind_cluster
    ;;
  k8s-install-ingress)
    ensure_kubectl
    install_ingress_nginx
    ;;
  k8s-up)
    ensure_kubectl
    ensure_k8s_secret_file
    apply_k8s_manifests
    rollout_restart_k8s_apps
    ;;
  k8s-down)
    ensure_kubectl
    delete_k8s_manifests
    ;;
  k8s-status)
    ensure_kubectl
    show_k8s_status
    ;;
  k8s-load-kind)
    ensure_kind
    load_kind_images
    ;;
  *)
    echo "Unknown command: $command"
    usage
    exit 1
    ;;
esac
