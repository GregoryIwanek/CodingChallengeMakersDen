#!/usr/bin/env bash
# Guided start for the :proxy server: checks, API key, start, wait until ready, test calls.
# Each step prints the equivalent manual command, so it doubles as a walkthrough.
# Usage: ./proxy/run.sh   (PORT=8081 ./proxy/run.sh for another port)
# Written for macOS's bash 3.2, so no associative arrays or ${var,,}.

set -uo pipefail

cd "$(dirname "$0")/.." || exit 1

PORT="${PORT:-8080}"
LOG="proxy/build/proxy-run.log"
READY_TIMEOUT_S=180
GRADLE_PID=""
TAIL_PID=""

if [[ -t 1 ]]; then
  BOLD=$'\e[1m' DIM=$'\e[2m' GREEN=$'\e[32m' YELLOW=$'\e[33m' RED=$'\e[31m' RESET=$'\e[0m'
else
  BOLD="" DIM="" GREEN="" YELLOW="" RED="" RESET=""
fi

step() { printf '\n%s▶ %s%s\n' "$BOLD" "$1" "$RESET"; }
cmd() { printf '  %s$ %s%s\n' "$DIM" "$1" "$RESET"; }
ok() { printf '  %s✔ %s%s\n' "$GREEN" "$1" "$RESET"; }
warn() { printf '  %s! %s%s\n' "$YELLOW" "$1" "$RESET"; }
fail() { printf '  %s✘ %s%s\n' "$RED" "$1" "$RESET"; }

# Ctrl+C reaches this script, not the background Gradle client (non-interactive shells
# start & jobs with SIGINT ignored), so stop it here. SIGTERM makes Gradle cancel the
# build, which also stops the server.
cleanup() {
  trap - INT TERM EXIT
  [[ -n "$TAIL_PID" ]] && kill "$TAIL_PID" 2>/dev/null
  if [[ -n "$GRADLE_PID" ]] && kill -0 "$GRADLE_PID" 2>/dev/null; then
    printf '\n%s▶ Stopping the server...%s\n' "$BOLD" "$RESET"
    kill -TERM "$GRADLE_PID" 2>/dev/null
    wait "$GRADLE_PID" 2>/dev/null
    ok "Stopped. Port $PORT is free again."
  fi
}
trap cleanup EXIT
trap 'exit 130' INT TERM

port_owner() { lsof -nP -iTCP:"$1" -sTCP:LISTEN 2>/dev/null | awk 'NR > 1 { print $1 " (pid " $2 ")"; exit }'; }

# --- 1. Preflight -----------------------------------------------------------------------
step "1/5 Preflight checks"

if [[ -n "${JAVA_HOME:-}" && -x "$JAVA_HOME/bin/java" ]] || command -v java >/dev/null; then
  ok "Java found (runs Gradle; Gradle fetches the JDK 21 the proxy needs by itself)"
else
  fail "No Java found. Install a JDK or set JAVA_HOME, then run again."
  exit 1
fi

for tool in curl lsof; do
  command -v "$tool" >/dev/null || { fail "$tool is missing."; exit 1; }
done

cmd "lsof -iTCP:$PORT -sTCP:LISTEN"
while owner=$(port_owner "$PORT") && [[ -n "$owner" ]]; do
  warn "Port $PORT is already used by $owner. Maybe a proxy is already running?"
  warn "The emulator build expects 8080, so another port only suits curl testing."
  read -r -p "  Another port to use (Enter to quit): " answer
  [[ -z "$answer" ]] && exit 1
  [[ "$answer" =~ ^[0-9]+$ ]] && PORT="$answer" || warn "Not a number: $answer"
done
ok "Port $PORT is free"

# --- 2. API key -------------------------------------------------------------------------
step "2/5 Anthropic API key"

if [[ -n "${ANTHROPIC_API_KEY:-}" ]]; then
  ok "Using ANTHROPIC_API_KEY already exported in this shell"
else
  echo "  Not set in this shell. Paste it below; input is hidden and nothing is saved."
  echo "  Manual equivalent, which also keeps it out of shell history:"
  cmd "read -s \"ANTHROPIC_API_KEY?Anthropic API key: \" && export ANTHROPIC_API_KEY   # zsh"
  while [[ -z "${ANTHROPIC_API_KEY:-}" ]]; do
    read -r -s -p "  Anthropic API key: " ANTHROPIC_API_KEY
    echo
    [[ -z "$ANTHROPIC_API_KEY" ]] && warn "Empty input, try again (Ctrl+C to quit)."
  done
  export ANTHROPIC_API_KEY
  ok "Key received (kept in this script's memory only; gone when it exits)"
fi
[[ "$ANTHROPIC_API_KEY" == sk-ant-* ]] || warn "Key doesn't start with sk-ant-; calls to Claude will likely fail."

# --- 3. Start ---------------------------------------------------------------------------
step "3/5 Starting the server (first run compiles, so it can take a minute)"
cmd "PORT=$PORT ./gradlew :proxy:run --console=plain"

mkdir -p "$(dirname "$LOG")"
: >"$LOG"
PORT="$PORT" ./gradlew :proxy:run --console=plain -q >"$LOG" 2>&1 &
GRADLE_PID=$!

waited=0
until grep -q "Responding at" "$LOG"; do
  if ! kill -0 "$GRADLE_PID" 2>/dev/null; then
    GRADLE_PID=""
    echo
    fail "The server stopped during startup. Last lines of $LOG:"
    tail -n 15 "$LOG" | sed 's/^/    /'
    grep -q "ANTHROPIC_API_KEY is not set" "$LOG" && warn "The key didn't reach the server."
    grep -qi "address already in use" "$LOG" && warn "Port $PORT got taken; try PORT=8081 $0"
    exit 1
  fi
  if ((waited >= READY_TIMEOUT_S)); then
    echo
    fail "Not ready after ${READY_TIMEOUT_S}s. See $LOG"
    exit 1
  fi
  printf '.'
  sleep 1
  waited=$((waited + 1))
done
((waited > 0)) && echo
ok "Ready after ${waited}s: $(grep -o 'Responding at .*' "$LOG")"

BASE="http://127.0.0.1:$PORT"

# --- 4. Free connection check -----------------------------------------------------------
step "4/5 Connection check (no Claude call, no cost)"
cmd "curl -N \"$BASE/explain?repo=bad\""
reply=$(curl -sN -m 10 "$BASE/explain?repo=bad")
if grep -q "invalid_repo" <<<"$reply"; then
  ok "Server answered with the expected invalid_repo error"
else
  fail "Unexpected reply: ${reply:-<nothing>}"
fi

# --- 5. Optional paid smoke test --------------------------------------------------------
step "5/5 Real request through Claude (optional)"
cmd "curl -N \"$BASE/explain?repo=square/okhttp\""
read -r -p "  Send it? It costs a few cents. [y/N] " answer
if [[ "$answer" =~ ^[Yy]$ ]]; then
  headers=$(mktemp)
  echo
  # Prints text frames as they arrive, then a one-line summary of the named events.
  curl -sN -m 120 -D "$headers" "$BASE/explain?repo=square/okhttp" | awk '
    function dispatch() {
      if (ev == "") { printf "%s", buf; fflush(); chunks++ }
      else if (ev == "stop") stop = buf
      else if (ev == "error") err = buf
      else if (ev == "truncated") trunc = buf
      ev = ""; buf = ""; has = 0
    }
    { sub(/\r$/, "") }
    /^event:/ { ev = substr($0, 7); sub(/^ /, "", ev); next }
    /^data:/ { d = substr($0, 6); sub(/^ /, "", d); buf = (has ? buf "\n" : "") d; has = 1; next }
    /^$/ { if (has || ev != "") dispatch(); next }
    END {
      if (has || ev != "") dispatch()
      printf "\n\n"
      if (trunc != "") printf "  truncated: %s\n", trunc
      if (err != "") printf "  RESULT error=%s chunks=%d\n", err, chunks
      else printf "  RESULT stop=%s chunks=%d\n", (stop == "" ? "<none>" : stop), chunks
    }' | tee /dev/stderr | grep -q "RESULT stop=end_turn" && streamed=1 || streamed=0
  status=$(awk 'NR == 1 { print $2 }' "$headers")
  rm -f "$headers"
  if [[ "$status" == "429" ]]; then
    warn "Rate limited (429): over 10 /explain calls this minute. Wait and retry."
  elif ((streamed)); then
    ok "Streamed and finished normally"
  else
    fail "Didn't finish with end_turn (HTTP ${status:-?}); server log below may say why."
  fi
else
  echo "  Skipped."
fi

# --- Running ----------------------------------------------------------------------------
printf '\n%s%s● Proxy running%s\n' "$BOLD" "$GREEN" "$RESET"
echo "  From this Mac:      $BASE"
echo "  From the emulator:  http://10.0.2.2:$PORT"
echo "  Try:                curl -N \"$BASE/explain?repo=square/okhttp\""
echo "  Stop:               Ctrl+C"
printf '\n%sServer log:%s\n' "$DIM" "$RESET"

tail -n +1 -f "$LOG" &
TAIL_PID=$!
wait "$GRADLE_PID"
GRADLE_PID=""
fail "The server exited on its own; see the log above."
exit 1
