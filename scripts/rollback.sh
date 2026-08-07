#!/usr/bin/env bash
# =============================================================================
# scripts/rollback.sh
# -----------------------------------------------------------------------------
# Restores the previously recorded Docker image for a given OBS service on EC2.
#
# Previous image is written by deploy.sh to:
#   ${STATE_DIR}/${SERVICE_NAME}.prev_image
#
# May be invoked:
#   - automatically by deploy.sh when post-deploy health fails
#   - manually by operators or a Jenkins "Rollback" parameter build
# =============================================================================
set -euo pipefail

SERVICE_NAME="product-service"
CONTAINER_PORT="8082"
HEALTH_ENDPOINT="/products"
HEALTH_RETRIES=20
HEALTH_INTERVAL_SEC=6
EXTRA_ENV=""
ENV_FILE=""
STATE_DIR="${OBS_STATE_DIR:-/opt/obs/deploy-state}"
RESTART_POLICY="always"
HOST_BIND="0.0.0.0"
REQUIRE_2XX="true"
# Optional explicit image override (skips state file). Useful for emergency pin.
IMAGE_OVERRIDE=""

usage() {
  cat <<'EOF'
Usage: rollback.sh [options]

Options:
  --service NAME
  --port PORT
  --health PATH
  --image IMAGE              Explicit image to restore (skips state file)
  --extra-env "K=V,K2=V2"
  --env-file PATH
  --state-dir PATH
  --require-2xx true|false
  -h, --help
EOF
}

log() { printf '[rollback] %s\n' "$*"; }
die() { printf '[rollback][ERROR] %s\n' "$*" >&2; exit 1; }

while [[ $# -gt 0 ]]; do
  case "$1" in
    --service) SERVICE_NAME="${2:?}"; shift 2 ;;
    --port) CONTAINER_PORT="${2:?}"; shift 2 ;;
    --health) HEALTH_ENDPOINT="${2:?}"; shift 2 ;;
    --image) IMAGE_OVERRIDE="${2:?}"; shift 2 ;;
    # Allow empty EXTRA_ENV (deploy may pass nothing); ${2:?} treats "" as unset.
    --extra-env) EXTRA_ENV="${2:-}"; shift 2 ;;
    --env-file) ENV_FILE="${2:?}"; shift 2 ;;
    --state-dir) STATE_DIR="${2:?}"; shift 2 ;;
    --require-2xx) REQUIRE_2XX="${2:?}"; shift 2 ;;
    --health-retries) HEALTH_RETRIES="${2:?}"; shift 2 ;;
    --health-interval) HEALTH_INTERVAL_SEC="${2:?}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) die "Unknown argument: $1" ;;
  esac
done

command -v docker >/dev/null 2>&1 || die "docker is not installed or not on PATH"
command -v curl >/dev/null 2>&1 || die "curl is required for health checks"

CONTAINER_NAME="${SERVICE_NAME}"
STATE_FILE="${STATE_DIR}/${SERVICE_NAME}.prev_image"
CURRENT_FILE="${STATE_DIR}/${SERVICE_NAME}.current_image"
LOCK_FILE="${STATE_DIR}/${SERVICE_NAME}.lock"

mkdir -p "$STATE_DIR"

# Share the same flock as deploy.sh so deploy and rollback never interleave.
# When invoked from deploy.sh, parent already holds the lock and sets OBS_SKIP_LOCK=1
# to avoid deadlock (same lock file, nested process).
if [[ "${OBS_SKIP_LOCK:-0}" != "1" ]]; then
  exec 9>"$LOCK_FILE"
  if ! flock -n 9; then
    log "Waiting for deploy lock on ${SERVICE_NAME}..."
    flock 9
  fi
else
  log "Parent deploy holds the lock; continuing without re-acquiring"
fi

if [[ -n "$IMAGE_OVERRIDE" ]]; then
  TARGET_IMAGE="$IMAGE_OVERRIDE"
elif [[ -f "$STATE_FILE" ]]; then
  TARGET_IMAGE="$(tr -d '[:space:]' <"$STATE_FILE")"
else
  die "No previous image recorded at ${STATE_FILE} and --image not provided"
fi

[[ -n "$TARGET_IMAGE" ]] || die "Previous image reference is empty"

log "Rolling back ${SERVICE_NAME} to image: ${TARGET_IMAGE}"

# Pull ensures layers exist if the host was replaced or local cache was pruned.
docker pull "$TARGET_IMAGE" || log "Warning: docker pull failed; trying local image if present"

RUN_ARGS=(
  --name "$CONTAINER_NAME"
  --restart="$RESTART_POLICY"
  -p "${HOST_BIND}:${CONTAINER_PORT}:${CONTAINER_PORT}"
  -e "SERVER_PORT=${CONTAINER_PORT}"
)

RUN_ARGS+=(-e "SPRING_PROFILES_ACTIVE=prod")
RUN_ARGS+=(-e "AWS_REGION=${AWS_REGION:-ap-south-1}")

if [[ -n "$ENV_FILE" ]]; then
  [[ -f "$ENV_FILE" ]] || die "env-file not found on host: $ENV_FILE"
  RUN_ARGS+=(--env-file "$ENV_FILE")
fi

if [[ -n "$EXTRA_ENV" ]]; then
  IFS=',' read -r -a pairs <<<"$EXTRA_ENV"
  for pair in "${pairs[@]}"; do
    pair="$(echo "$pair" | xargs)"
    [[ -z "$pair" ]] && continue
    [[ "$pair" == *=* ]] || die "Invalid --extra-env pair: $pair"
    RUN_ARGS+=(-e "$pair")
  done
fi

docker stop "$CONTAINER_NAME" >/dev/null 2>&1 || true
docker rm "$CONTAINER_NAME" >/dev/null 2>&1 || true

docker run -d "${RUN_ARGS[@]}" "$TARGET_IMAGE"
printf '%s\n' "$TARGET_IMAGE" >"$CURRENT_FILE"

health_url="http://127.0.0.1:${CONTAINER_PORT}${HEALTH_ENDPOINT}"
log "Verifying rollback health: ${health_url}"

attempt=1
healthy=0
while (( attempt <= HEALTH_RETRIES )); do
  code="$(curl -s -o /dev/null -w '%{http_code}' --max-time 5 "$health_url" || true)"
  if [[ "$code" =~ ^[0-9]{3}$ ]]; then
    if [[ "$REQUIRE_2XX" == "true" ]]; then
      if (( code >= 200 && code < 400 )); then healthy=1; break; fi
    else
      if (( code >= 100 && code < 500 )); then healthy=1; break; fi
    fi
  fi
  log "Rollback health attempt ${attempt}/${HEALTH_RETRIES}: HTTP ${code:-000}"
  sleep "$HEALTH_INTERVAL_SEC"
  attempt=$((attempt + 1))
done

if (( healthy != 1 )); then
  docker logs --tail 100 "$CONTAINER_NAME" || true
  die "Rollback completed container start but health check still failed"
fi

log "Rollback succeeded: ${SERVICE_NAME} is running ${TARGET_IMAGE}"
exit 0
