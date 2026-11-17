#!/usr/bin/env bash
# Starts EcoSwape locally (Linux / macOS). Reads settings from .env in the project root.
set -euo pipefail
cd "$(dirname "$0")/.."

if [[ ! -f .env ]]; then
  echo "No .env file found. Copy .env.example to .env and fill in DB_PASSWORD (and the admin bootstrap values)." >&2
  exit 1
fi

set -a
# shellcheck disable=SC1091
source .env
set +a

: "${DB_PASSWORD:?DB_PASSWORD must be set in .env}"
mkdir -p "${ECOSWAP_STORAGE_DIR:-./storage}"

if [[ "${1:-}" == "--test" ]]; then
  exec ./mvnw clean verify
fi
exec ./mvnw spring-boot:run
