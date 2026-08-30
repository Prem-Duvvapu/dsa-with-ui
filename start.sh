#!/usr/bin/env bash

set -Eeuo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
backend_pid=""
frontend_pid=""

cleanup() {
  trap - EXIT INT TERM

  if [[ -n "$frontend_pid" ]] && kill -0 "$frontend_pid" 2>/dev/null; then
    kill "$frontend_pid" 2>/dev/null || true
  fi
  if [[ -n "$backend_pid" ]] && kill -0 "$backend_pid" 2>/dev/null; then
    kill "$backend_pid" 2>/dev/null || true
  fi

  wait "$frontend_pid" "$backend_pid" 2>/dev/null || true
}

trap cleanup EXIT INT TERM

command -v mvn >/dev/null 2>&1 || {
  echo "Error: Maven (mvn) is required to start the backend." >&2
  exit 1
}
command -v npm >/dev/null 2>&1 || {
  echo "Error: npm is required to start the frontend." >&2
  exit 1
}

if [[ ! -d "$repo_dir/frontend/node_modules" ]]; then
  echo "Installing frontend dependencies..."
  npm --prefix "$repo_dir/frontend" ci
fi

echo "Starting backend at http://localhost:8923"
(
  cd "$repo_dir/backend"
  exec mvn spring-boot:run
) &
backend_pid=$!

echo "Starting frontend at http://localhost:5180"
(
  cd "$repo_dir/frontend"
  exec npm run dev
) &
frontend_pid=$!

echo "Both services are running. Press Ctrl+C to stop them."

set +e
wait -n "$backend_pid" "$frontend_pid"
status=$?
set -e

if (( status != 0 )); then
  echo "A service exited with status $status; stopping the other service." >&2
fi
exit "$status"
