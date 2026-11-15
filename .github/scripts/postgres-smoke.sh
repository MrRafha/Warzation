#!/usr/bin/env bash
# Sobe o Backend contra um PostgreSQL real, aplica um comando, reinicia a
# aplicação e confere que o estado persistiu e que o reenvio não duplica.
set -euo pipefail

API=http://localhost:8080
JAR=$(ls Backend/target/*.jar | grep -v plain | head -1)
PID=""

start_backend() {
  java -jar "$JAR" >> backend.log 2>&1 &
  PID=$!
  for _ in $(seq 1 60); do
    if curl -fsS "$API/api/v1/health" > /dev/null 2>&1; then
      echo "Backend no ar (pid $PID)"
      return 0
    fi
    if ! kill -0 "$PID" 2> /dev/null; then
      echo "Backend encerrou durante o startup" >&2
      return 1
    fi
    sleep 2
  done
  echo "Backend não respondeu em 120s" >&2
  return 1
}

stop_backend() {
  kill "$PID"
  wait "$PID" || true
}

expect_eq() {
  if [ "$1" != "$2" ]; then
    echo "FALHA: $3 (esperado '$2', obtido '$1')" >&2
    exit 1
  fi
  echo "ok: $3"
}

COMMAND_ID=$(cat /proc/sys/kernel/random/uuid)
COMMAND=$(printf '{"commandId":"%s","type":"INCREMENT_COUNTER","payload":{"amount":3}}' "$COMMAND_ID")

start_backend
before=$(curl -fsS "$API/api/v1/rooms/dev" | jq .counter)
status=$(curl -sS -o /tmp/first.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -d "$COMMAND" "$API/api/v1/rooms/dev/commands")
expect_eq "$status" "201" "primeiro envio aplicado"
expect_eq "$(jq -r .status /tmp/first.json)" "APPLIED" "status APPLIED"
stop_backend

start_backend
after=$(curl -fsS "$API/api/v1/rooms/dev" | jq .counter)
expect_eq "$after" "$((before + 3))" "contador persistiu após reinício"
status=$(curl -sS -o /tmp/again.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -d "$COMMAND" "$API/api/v1/rooms/dev/commands")
expect_eq "$status" "200" "reenvio após reinício"
expect_eq "$(jq -r .status /tmp/again.json)" "DUPLICATE" "reenvio marcado como DUPLICATE"
expect_eq "$(curl -fsS "$API/api/v1/rooms/dev" | jq .counter)" "$((before + 3))" "reenvio não duplicou"
expect_eq "$(curl -fsS "$API/api/v1/rooms/dev/commands/$COMMAND_ID" | jq .resultingCounter)" "$((before + 3))" \
  "resultado do comando consultável"
stop_backend

echo "Smoke com PostgreSQL concluído"
