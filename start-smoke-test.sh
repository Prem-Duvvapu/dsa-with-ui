#!/usr/bin/env bash

set -Eeuo pipefail

# When invoked through the fake mvn/npm links below, act as a long-running service with
# one child process. That gives the smoke test a real process tree without starting Java,
# Vite, or touching project dependencies.
case "$(basename -- "$0")" in
  mvn|npm)
    for argument in "$@"; do
      if [[ "$argument" == "ci" ]]; then
        exit 0
      fi
    done
    : "${START_SMOKE_LOG:?START_SMOKE_LOG must name the smoke-test process log}"
    sleep 300 &
    child_pid=$!
    printf '%s %s %s\n' "$(basename -- "$0")" "$$" "$child_pid" >> "$START_SMOKE_LOG"
    wait "$child_pid"
    exit $?
    ;;
esac

repo_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
smoke_dir="$(mktemp -d)"
launcher_pid=""

cleanup() {
  trap - EXIT INT TERM
  if [[ -n "$launcher_pid" ]] && kill -0 "$launcher_pid" 2>/dev/null; then
    kill -TERM -- "-$launcher_pid" 2>/dev/null || true
    sleep 0.2
    kill -KILL -- "-$launcher_pid" 2>/dev/null || true
    wait "$launcher_pid" 2>/dev/null || true
  fi
  rm -rf -- "$smoke_dir"
}
trap cleanup EXIT INT TERM

mkdir -p "$smoke_dir/bin"
cp "$repo_dir/start-smoke-test.sh" "$smoke_dir/bin/mvn"
cp "$repo_dir/start-smoke-test.sh" "$smoke_dir/bin/npm"
chmod +x "$smoke_dir/bin/mvn" "$smoke_dir/bin/npm"
process_log="$smoke_dir/processes.log"
output_log="$smoke_dir/start.log"
: > "$process_log"

PATH="$smoke_dir/bin:$PATH" START_SMOKE_LOG="$process_log" \
  setsid "$repo_dir/start.sh" > "$output_log" 2>&1 &
launcher_pid=$!

deadline=$((SECONDS + 5))
while (( $(wc -l < "$process_log") < 2 )); do
  if ! kill -0 "$launcher_pid" 2>/dev/null; then
    printf 'start.sh exited before both service stubs launched:\n' >&2
    sed -n '1,120p' "$output_log" >&2
    exit 1
  fi
  if (( SECONDS >= deadline )); then
    printf 'Timed out waiting for both service stubs:\n' >&2
    sed -n '1,120p' "$output_log" >&2
    exit 1
  fi
  sleep 0.1
done

# Signal only the launcher, as a service manager would. The launcher must explicitly
# stop every descendant; relying on the terminal broadcasting a signal would hide leaks.
kill -TERM "$launcher_pid"

deadline=$((SECONDS + 7))
while process_state="$(ps -o stat= -p "$launcher_pid" 2>/dev/null)" \
  && [[ "$process_state" != Z* ]]; do
  if (( SECONDS >= deadline )); then
    printf 'start.sh did not finish cleanup within seven seconds:\n' >&2
    sed -n '1,120p' "$output_log" >&2
    exit 1
  fi
  sleep 0.1
done

set +e
wait "$launcher_pid"
launcher_status=$?
set -e
launcher_pid=""

if (( launcher_status != 143 )); then
  printf 'Expected start.sh to exit 143 after SIGTERM, got %s\n' "$launcher_status" >&2
  sed -n '1,120p' "$output_log" >&2
  exit 1
fi

while read -r command parent_pid child_pid; do
  for pid in "$parent_pid" "$child_pid"; do
    if kill -0 "$pid" 2>/dev/null; then
      printf '%s process %s survived start.sh cleanup\n' "$command" "$pid" >&2
      exit 1
    fi
  done
done < "$process_log"

printf 'start.sh stopped both service process groups cleanly\n'
