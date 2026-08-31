#!/usr/bin/env bash

set -Eeuo pipefail

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
backend_pid=""
frontend_pid=""

cleanup() {
  trap - EXIT INT TERM

  local pid
  for pid in "$frontend_pid" "$backend_pid"; do
    if [[ -n "$pid" ]] && kill -0 -- "-$pid" 2>/dev/null; then
      kill -TERM -- "-$pid" 2>/dev/null || true
    fi
  done

  # Maven and npm can both have descendants. Give each dedicated process group a short
  # graceful shutdown window, then make sure no Java/Vite child is left behind.
  local deadline=$((SECONDS + 5))
  local groups_alive
  while true; do
    groups_alive=0
    for pid in "$frontend_pid" "$backend_pid"; do
      if [[ -n "$pid" ]] && kill -0 -- "-$pid" 2>/dev/null; then
        groups_alive=1
      fi
    done
    if (( groups_alive == 0 || SECONDS >= deadline )); then
      break
    fi
    sleep 0.1
  done

  for pid in "$frontend_pid" "$backend_pid"; do
    if [[ -n "$pid" ]] && kill -0 -- "-$pid" 2>/dev/null; then
      kill -KILL -- "-$pid" 2>/dev/null || true
    fi
  done

  wait "$frontend_pid" "$backend_pid" 2>/dev/null || true
}

trap cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM

command -v mvn >/dev/null 2>&1 || {
  echo "Error: Maven (mvn) is required to start the backend." >&2
  exit 1
}
command -v npm >/dev/null 2>&1 || {
  echo "Error: npm is required to start the frontend." >&2
  exit 1
}
command -v setsid >/dev/null 2>&1 || {
  echo "Error: setsid is required so both service process trees stop cleanly." >&2
  exit 1
}

if [[ ! -d "$repo_dir/frontend/node_modules" ]]; then
  echo "Installing frontend dependencies..."
  npm --prefix "$repo_dir/frontend" ci
fi

echo "Starting backend at http://localhost:8923"
setsid bash -c 'cd -- "$1" && exec mvn spring-boot:run' _ "$repo_dir/backend" &
backend_pid=$!

echo "Starting frontend at http://localhost:5180"
setsid bash -c 'cd -- "$1" && exec npm run dev' _ "$repo_dir/frontend" &
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
