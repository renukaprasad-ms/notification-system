#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
ENV_FILE="$ROOT_DIR/infra/.env"
POSTGRES_COMPOSE="$ROOT_DIR/infra/postgres.compose.yml"
BACKEND_COMPOSE="$ROOT_DIR/infra/backend.compose.yml"
FRONTEND_COMPOSE="$ROOT_DIR/infra/frontend.compose.yml"

usage() {
  cat <<'USAGE'
Usage:
  ./scripts/provision.sh <command> [service]

Commands:
  up [all|postgres|backend|frontend]       Start services
  build [all|postgres|backend|frontend]    Build or pull service images
  restart [all|postgres|backend|frontend]  Restart services
  logs [all|postgres|backend|frontend]     Follow service logs
  stop [all|postgres|backend|frontend]     Stop services
  down                            Stop and remove infra containers
  status                          Show running infra services

Examples:
  ./scripts/provision.sh up
  ./scripts/provision.sh build backend
  ./scripts/provision.sh logs postgres
  ./scripts/provision.sh restart backend
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

compose_files_for() {
  local service="${1:-all}"

  case "$service" in
    all)
      echo "-f $POSTGRES_COMPOSE -f $BACKEND_COMPOSE -f $FRONTEND_COMPOSE"
      ;;
    postgres)
      echo "-f $POSTGRES_COMPOSE"
      ;;
    backend)
      echo "-f $POSTGRES_COMPOSE -f $BACKEND_COMPOSE"
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

ensure_env_file

case "$command" in
  up)
    if [[ "$service" == "backend" ]]; then
      compose "$service" up -d postgres notification-backend
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
  *)
    echo "Unknown command: $command"
    usage
    exit 1
    ;;
esac
