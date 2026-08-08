#!/usr/bin/env bash
# =============================================================================
# scripts/deploy.sh
# -----------------------------------------------------------------------------
# Reusable EC2 Docker deploy helper for OBS Spring Boot microservices.
#
# Intended to run ON the EC2 host (Jenkins SSHes in and invokes this script).
# Pulls an image that was already pushed to ECR, replaces the running container
# atomically from the caller's perspective (stop → start → health → rollback).
#
# Safety contract:
#   1. Record the currently running image reference BEFORE stopping anything.
#   2. Pull the new image while the old container is still serving traffic.
#   3. Only then stop/remove the old container and start the new one.
#   4. Fail the deploy (and auto-rollback) if HEALTH_ENDPOINT does not come up.
#
# Concurrent deploys of the SAME service on one host are serialized via a
# flock lock file under STATE_DIR. Different services use different locks.
# =============================================================================
set -euo pipefail

# Defaults match product-service (current production target of this monorepo).
SERVICE_NAME="product-service"
IMAGE=""
CONTAINER_PORT="8082"
HEALTH_ENDPOINT="/products"
HEALTH_RETRIES=20
HEALTH_INTERVAL_SEC=6
# Comma-separated KEY=VALUE pairs appended as docker -e flags (no secrets in logs if you avoid printing).
EXTRA_ENV=""
# Optional path to an env-file already present on the EC2 host (never committed to git).
ENV_FILE=""
# Where previous image tags are stored for rollback.sh
STATE_DIR="${OBS_STATE_DIR:-/opt/obs/deploy-state}"
# Docker network / restart policy — restart=always recovers after EC2 reboot.
RESTART_POLICY="always"
# Host bind: by default publish container port on all interfaces (gateway may be public).
HOST_BIND="0.0.0.0"
# Accept 2xx/3xx as healthy; set REQUIRE_2XX=false to also accept 4xx (useful for user-service).
REQUIRE_2XX="true"

usage() {
  cat <<'EOF'
Usage: deploy.sh --image <ecr/repo:tag> [options]

Required:
  --image IMAGE              Full image reference (registry/repo:tag)

Options:
  --service NAME             Container/service name (default: product-service)
  --port PORT                Container + published host port (default: 8082)
  --health PATH              HTTP path on localhost for post-deploy check (default: /products)
  --health-retries N         Attempts before rollback (default: 20)
  --health-interval SEC      Sleep between attempts (default: 6)
  --extra-env "K=V,K2=V2"    Additional -e environment variables
  --env-file PATH            docker --env-file on the host (DB secrets, etc.)
  --state-dir PATH           Directory for previous-image bookkeeping
  --require-2xx true|false   Health requires HTTP 2xx/3xx (default true)
  -h, --help                 Show help
EOF
}

log() { printf '[deploy] %s\n' "$*"; }
die() { printf '[deploy][ERROR] %s\n' "$*" >&2; exit 1; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --service) SERVICE_NAME="${2:?}"; shift 2 ;;
    --image) IMAGE="${2:?}"; shift 2 ;;
    --port) CONTAINER_PORT="${2:?}"; shift 2 ;;
    --health) HEALTH_ENDPOINT="${2:?}"; shift 2 ;;
    --health-retries) HEALTH_RETRIES="${2:?}"; shift 2 ;;
    --health-interval) HEALTH_INTERVAL_SEC="${2:?}"; shift 2 ;;
    --extra-env) EXTRA_ENV="${2:?}"; shift 2 ;;
    --env-file) ENV_FILE="${2:?}"; shift 2 ;;
    --state-dir) STATE_DIR="${2:?}"; shift 2 ;;
    --require-2xx) REQUIRE_2XX="${2:?}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown argument: $1" ;;
  esac
done

[[ -n "$IMAGE" ]] || die "--image is required"
[[ "$CONTAINER_PORT" =~ ^[0-9]+$ ]] || die "--port must be numeric"
command -v docker >/dev/null 2>&1 || die "docker is not installed or not on PATH"
command -v curl >/dev/null 2>&1 || die "curl is required for health checks"

# Container name = service name keeps ops simple; one container per service on a host.
CONTAINER_NAME="${SERVICE_NAME}"
STATE_FILE="${STATE_DIR}/${SERVICE_NAME}.prev_image"
CURRENT_FILE="${STATE_DIR}/${SERVICE_NAME}.current_image"
LOCK_FILE="${STATE_DIR}/${SERVICE_NAME}.lock"

mkdir -p "$STATE_DIR"

# Serialize concurrent deploys of the same service (flock is released on exit).
exec 9>"$LOCK_FILE"
if ! flock -n 9; then
  die "Another deploy is already running for ${SERVICE_NAME} (lock: ${LOCK_FILE})"
fi

# Capture the image the live container is using BEFORE we stop it.
PREV_IMAGE=""
if docker ps -a --format '{{.Names}}' | grep -qx "$CONTAINER_NAME"; then
  PREV_IMAGE="$(docker inspect --format '{{.Config.Image}}' "$CONTAINER_NAME" 2>/dev/null || true)"
fi
if [[ -n "$PREV_IMAGE" ]]; then
  printf '%s\n' "$PREV_IMAGE" >"$STATE_FILE"
  log "Recorded previous image for rollback: ${PREV_IMAGE}"
else
  log "No previous container found for ${CONTAINER_NAME}; first deploy of this service"
  # Leave any older STATE_FILE intact if present (manual rollbacks still possible).
fi

log "Pulling image: ${IMAGE}"
docker pull "$IMAGE"

# Build docker run argument list (array avoids word-splitting bugs in env values).
RUN_ARGS=(
  --name "$CONTAINER_NAME"
  --restart="$RESTART_POLICY"
  -p "${HOST_BIND}:${CONTAINER_PORT}:${CONTAINER_PORT}"
  -e "SERVER_PORT=${CONTAINER_PORT}"
)

# Prod profile + region for all OBS services (SSM/S3 use instance profile / default provider chain).
# Secrets stay in Parameter Store — do not put them in EXTRA_ENV or a host env-file unless needed.
RUN_ARGS+=(-e "SPRING_PROFILES_ACTIVE=prod")
RUN_ARGS+=(-e "AWS_REGION=${AWS_REGION:-ap-south-1}")

if [[ -n "$ENV_FILE" ]]; then
  [[ -f "$ENV_FILE" ]] || die "env-file not found on host: $ENV_FILE"
  RUN_ARGS+=(--env-file "$ENV_FILE")
fi

# Parse EXTRA_ENV as comma-separated KEY=VALUE.
if [[ -n "$EXTRA_ENV" ]]; then
  IFS=',' read -r -a pairs <<<"$EXTRA_ENV"
  for pair in "${pairs[@]}"; do
    pair="$(echo "$pair" | xargs)" # trim
    [[ -z "$pair" ]] && continue
    [[ "$pair" == *=* ]] || die "Invalid --extra-env pair (expected KEY=VALUE): $pair"
    RUN_ARGS+=(-e "$pair")
  done
fi

log "Stopping and removing existing container (if any): ${CONTAINER_NAME}"
# || true: first deploy has nothing to stop; we must not abort.
docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
docker rm "$CONTAINER_NAME" >/dev/null 2>&1 || true

log "Starting container ${CONTAINER_NAME} from ${IMAGE}"
docker run -d "${RUN_ARGS[@]}" "$IMAGE"

printf '%s\n' "$IMAGE" >"$CURRENT_FILE"

health_url="http://127.0.0.1:${CONTAINER_PORT}${HEALTH_ENDPOINT}"
log "Waiting for health: ${health_url} (retries=${HEALTH_RETRIES}, interval=${HEALTH_INTERVAL_SEC}s)"

attempt=1
healthy=0
while (( attempt <= HEALTH_RETRIES )); do
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$health_url" || true)"
  if [[ "$code" =~ ^[0-9]{3}$ ]]; then
    if [[ "$REQUIRE_2XX" == "true" ]]; then
      if (( code >= 200 && code < 400 )); then
        healthy=1
        break
      fi
    else
      # Connection succeeded and server answered (even 4xx) — process is up.
      if (( code >= 100 && code < 500 )); then
        healthy=1
        break
      fi
    fi
  fi
  log "Health attempt ${attempt}/${HEALTH_RETRIES}: HTTP ${code:-000}"
  sleep "$HEALTH_INTERVAL_SEC"
  attempt=$((attempt + 1))
done

if (( healthy != 1 )); then
  log "Health check FAILED for ${SERVICE_NAME}"
  # Auto-rollback keeps the service available when a prior image exists.
  if [[ -n "$PREV_IMAGE" ]]; then
    log "Initiating automatic rollback to ${PREV_IMAGE}"
    # Prefer sibling rollback.sh when co-located (Jenkins scp deploys both to /tmp).
    SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
    if [[ -x "${SCRIPT_DIR}/rollback.sh" ]]; then
      # OBS_SKIP_LOCK=1: we already hold the per-service flock; child must not re-lock.
      # Do not pass --extra-env when empty: rollback uses set -u and ${2:?} rejects "".
      OBS_SKIP_LOCK=1 "${SCRIPT_DIR}/rollback.sh" \
        --service "$SERVICE_NAME" \
        --port "$CONTAINER_PORT" \
        --health "$HEALTH_ENDPOINT" \
        ${EXTRA_ENV:+--extra-env "$EXTRA_ENV"} \
        ${ENV_FILE:+--env-file "$ENV_FILE"} \
        --state-dir "$STATE_DIR" \
        --require-2xx "$REQUIRE_2XX" \
        --image "$PREV_IMAGE" \
        || true
    else
      log "rollback.sh not found next to deploy.sh; attempting inline restart of previous image"
      docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
      docker rm "$CONTAINER_NAME" >/dev/null 2>&1 || true
      # Minimal re-run of previous container (same flags as best effort).
      docker run -d "${RUN_ARGS[@]}" "$PREV_IMAGE" || true
    fi
  else
    log "No previous image to restore — container left stopped/failed for investigation"
    docker logs --tail 100 "$CONTAINER_NAME" || true
  fi
  die "Deployment failed health verification"
fi

log "Deploy succeeded: ${SERVICE_NAME} -> ${IMAGE}"
exit 0
