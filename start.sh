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
# Each service must lead its own process group, so one signal can stop its whole tree -
# Maven forks a JVM and npm forks Vite, and signalling only the direct child leaves those
# behind. setsid does that on Linux/WSL; macOS has no setsid, so fall back to perl's
# setpgrp, which every macOS ships in /usr/bin/perl.
#
# `set -m` would also give each background job a fresh group and needs no extra binary,
# but it turns on job control for the whole script - stray "[1]+ Terminated" lines on
# stderr and changed foreground/terminal handling. perl's setpgrp does the one thing
# setsid does and nothing else, so both platforms take the same code path below.
#
# Note for anyone testing the INT trap: run this script in the FOREGROUND. A shell started
# with `&` inherits SIGINT ignored, and a trap cannot override an inherited SIG_IGN, so a
# backgrounded launcher looks like it is ignoring Ctrl+C when it is not.
setsid_bin="$(command -v setsid 2>/dev/null || true)"
perl_bin=""
if [[ -z "$setsid_bin" ]]; then
  perl_bin="$(command -v perl 2>/dev/null || true)"
  if [[ -z "$perl_bin" ]]; then
    echo "Error: need setsid or perl to give each service its own process group." >&2
    exit 1
  fi
fi

# Launch "$@" inside "$dir" as its own process group, setting launched_pid to its pid.
# It reports through a variable rather than stdout on purpose: command substitution runs
# in a subshell, so the job would be the subshell's child and `wait` here would refuse it.
launched_pid=""
launch_service() {
  local dir="$1"
  shift
  if [[ -n "$setsid_bin" ]]; then
    "$setsid_bin" bash -c 'cd -- "$1" && shift && exec "$@"' _ "$dir" "$@" &
  else
    "$perl_bin" -e 'setpgrp(0, 0); chdir($ARGV[0]) or die $!; shift @ARGV; exec @ARGV or die $!' \
      "$dir" "$@" &
  fi
  launched_pid=$!
}

# True once "$1" has exited. A finished child stays a zombie until it is reaped, and
# `kill -0` succeeds on a zombie, so the process state is what actually settles it.
service_finished() {
  local state
  state="$(ps -o stat= -p "$1" 2>/dev/null)" || return 0
  [[ -z "$state" || "$state" == Z* ]]
}

if [[ ! -d "$repo_dir/frontend/node_modules" ]]; then
  echo "Installing frontend dependencies..."
  npm --prefix "$repo_dir/frontend" ci
fi

echo "Starting backend at http://localhost:8923"
launch_service "$repo_dir/backend" mvn spring-boot:run
backend_pid="$launched_pid"

echo "Starting frontend at http://localhost:5180"
launch_service "$repo_dir/frontend" npm run dev
frontend_pid="$launched_pid"

echo "Both services are running. Press Ctrl+C to stop them."

# `wait -n` would be the natural call here, but it needs bash 4.3 and macOS ships 3.2.
# Polling both pids is portable and costs one wakeup every 200ms.
set +e
status=0
while true; do
  if service_finished "$backend_pid"; then
    wait "$backend_pid"
    status=$?
    break
  fi
  if service_finished "$frontend_pid"; then
    wait "$frontend_pid"
    status=$?
    break
  fi
  sleep 0.2
done
set -e

if (( status != 0 )); then
  echo "A service exited with status $status; stopping the other service." >&2
fi
exit "$status"
