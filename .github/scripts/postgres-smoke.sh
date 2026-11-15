#!/usr/bin/env bash
# Sobe o Backend contra um PostgreSQL real, aplica um comando, reinicia a
# aplicação e confere que o estado persistiu e que o reenvio não duplica.
set -euo pipefail

API=http://localhost:8080
APP_ARGS=()
# Ciclo econômico de 1 s para ver o relógio andar sem esperar uma hora.
export GAME_SPEED=3600
JAR=$(ls Backend/target/*.jar | grep -v plain | head -1)
PID=""

start_backend() { # argumentos extras vão para a JVM/aplicação (ex.: perfil dev-leve)
  java "$@" -jar "$JAR" "${APP_ARGS[@]}" >> backend.log 2>&1 &
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
expect_eq "$(curl -fsS "$API/api/v1/scenarios/nordeste-teste" | jq '.scenario.provinces | length')" "24" \
  "cenário do mapa empacotado no jar e servido pela API"
expect_eq "$(curl -fsS "$API/api/v1/rules" | jq -r '[.buildings[].id] | join(",")')" \
  "FAZENDA,CURRAL,CELEIRO,MINA,ACAMPAMENTO,BIBLIOTECA" "regras de jogo empacotadas no jar"
before=$(curl -fsS "$API/api/v1/rooms/dev" | jq .counter)
status=$(curl -sS -o /dev/null -w '%{http_code}' -H 'Content-Type: application/json' \
  -d "$COMMAND" "$API/api/v1/rooms/dev/commands")
expect_eq "$status" "401" "comando sem jogador identificado é recusado"
status=$(curl -sS -o /tmp/first.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $PLAYER_1_TOKEN" -d "$COMMAND" "$API/api/v1/rooms/dev/commands")
expect_eq "$status" "201" "primeiro envio aplicado"
expect_eq "$(jq -r .status /tmp/first.json)" "APPLIED" "status APPLIED"
expect_eq "$(jq -r .playerId /tmp/first.json)" "jogador-1" "comando registrado com o jogador do token"

post_as() { # token, caminho, corpo -> grava resposta em /tmp/resp.json e imprime o status
  curl -sS -o /tmp/resp.json -w '%{http_code}' -H 'Content-Type: application/json' \
    -H "Authorization: Bearer $1" -d "$3" "$API$2"
}
expect_eq "$(post_as "$PLAYER_1_TOKEN" /api/v1/matches \
  "{\"commandId\":\"$(cat /proc/sys/kernel/random/uuid)\",\"scenarioId\":\"nordeste-teste\",\"name\":\"Smoke\"}")" \
  "201" "partida criada"
MATCH_ID=$(jq -r .match.id /tmp/resp.json)
JOIN_ID=$(cat /proc/sys/kernel/random/uuid)
JOIN_BODY="{\"commandId\":\"$JOIN_ID\",\"nationId\":\"parnaiba\"}"
expect_eq "$(post_as "$PLAYER_1_TOKEN" "/api/v1/matches/$MATCH_ID/participants" "$JOIN_BODY")" "201" "jogador 1 entrou"
expect_eq "$(post_as "$PLAYER_2_TOKEN" "/api/v1/matches/$MATCH_ID/participants" \
  "{\"commandId\":\"$(cat /proc/sys/kernel/random/uuid)\",\"nationId\":\"parnaiba\"}")" "409" "nação ocupada recusada"
expect_eq "$(post_as "$PLAYER_2_TOKEN" "/api/v1/matches/$MATCH_ID/participants" \
  "{\"commandId\":\"$(cat /proc/sys/kernel/random/uuid)\",\"nationId\":\"sao-francisco\"}")" "201" "jogador 2 entrou"
expect_eq "$(post_as "$PLAYER_1_TOKEN" "/api/v1/matches/$MATCH_ID/start" \
  "{\"commandId\":\"$(cat /proc/sys/kernel/random/uuid)\"}")" "201" "partida iniciada com a posição inicial"

# O relógio econômico roda sem nenhum cliente conectado.
sleep 4
CYCLES_BEFORE=$(curl -fsS "$API/api/v1/matches/$MATCH_ID" | jq .economy.cycles)
[ "$CYCLES_BEFORE" -ge 2 ] || { echo "FALHA: relógio econômico parado ($CYCLES_BEFORE ciclos)" >&2; exit 1; }
echo "ok: relógio econômico aplicou $CYCLES_BEFORE ciclos sem clientes conectados"
stop_backend

# Segunda subida com o perfil dev-leve e a JVM limitada, como no script local.
APP_ARGS=(--spring.profiles.active=dev-leve)
start_backend -Xmx384m -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1
after=$(curl -fsS "$API/api/v1/rooms/dev" | jq .counter)
expect_eq "$after" "$((before + 3))" "contador persistiu após reinício"
status=$(curl -sS -o /tmp/again.json -w '%{http_code}' -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $PLAYER_1_TOKEN" -d "$COMMAND" "$API/api/v1/rooms/dev/commands")
expect_eq "$status" "200" "reenvio após reinício"
expect_eq "$(jq -r .status /tmp/again.json)" "DUPLICATE" "reenvio marcado como DUPLICATE"
expect_eq "$(curl -fsS "$API/api/v1/rooms/dev" | jq .counter)" "$((before + 3))" "reenvio não duplicou"
expect_eq "$(curl -fsS "$API/api/v1/rooms/dev/commands/$COMMAND_ID" | jq .resultingCounter)" "$((before + 3))" \
  "resultado do comando consultável"
sleep 2
CYCLES_AFTER=$(curl -fsS "$API/api/v1/matches/$MATCH_ID" | jq .economy.cycles)
[ "$CYCLES_AFTER" -gt "$CYCLES_BEFORE" ] || {
  echo "FALHA: ciclos não continuaram após reinício ($CYCLES_BEFORE -> $CYCLES_AFTER)" >&2; exit 1; }
echo "ok: relógio econômico retomou após reinício ($CYCLES_BEFORE -> $CYCLES_AFTER ciclos)"
expect_eq "$(curl -fsS "$API/api/v1/matches/$MATCH_ID" | jq '.participants | length')" "2" \
  "participantes da partida persistiram após reinício"
STATE=$(curl -fsS -H "Authorization: Bearer $PLAYER_2_TOKEN" "$API/api/v1/matches/$MATCH_ID/state")
expect_eq "$(echo "$STATE" | jq '.provinces | length')" "24" "estado territorial persistiu após reinício"
expect_eq "$(echo "$STATE" | jq '[.provinces[] | select(.ownerNationId == "parnaiba")] | length')" "3" \
  "nação do jogador 1 com 3 províncias"
expect_eq "$(echo "$STATE" | jq '[.provinces[] | select(.ownerNationId != null) | .units[].quantity] | add')" "12" \
  "6 infantarias por jogador"
TERESINA_FOOD=$(echo "$STATE" | jq '.provinces[] | select(.provinceId == "pi-teresina") | .stock.COMIDA')
if [ "$TERESINA_FOOD" -ge 60 ] || [ $((TERESINA_FOOD % 2)) -ne 0 ]; then
  echo "FALHA: comida de Teresina não segue a demanda de 2/ciclo ($TERESINA_FOOD)" >&2; exit 1
fi
echo "ok: demanda alimentar aplicada no PostgreSQL (Teresina com $TERESINA_FOOD de comida)"
expect_eq "$(echo "$STATE" | jq '.provinces[] | select(.provinceId == "pi-teresina") | .economy.foodBalance')" "-2" \
  "saldo de comida por ciclo exposto no estado"
expect_eq "$(echo "$STATE" | jq -c '[.provinces[] | select(.ownerNationId == null) | .economy] | unique')" "[null]" \
  "províncias neutras fora da economia"
expect_eq "$(curl -fsS -H "Authorization: Bearer $PLAYER_1_TOKEN" "$API/api/v1/matches/$MATCH_ID/events" \
  | jq -c '[.[].type]')" '["MATCH_CREATED","PLAYER_JOINED","PLAYER_JOINED","MATCH_STARTED"]' \
  "histórico da partida persistiu em ordem"
expect_eq "$(post_as "$PLAYER_1_TOKEN" "/api/v1/matches/$MATCH_ID/participants" "$JOIN_BODY")" "200" \
  "reenvio da entrada após reinício"
expect_eq "$(jq -r .status /tmp/resp.json)" "DUPLICATE" "entrada reenviada não duplica o participante"
expect_eq "$(curl -fsS -H "Authorization: Bearer $PLAYER_1_TOKEN" "$API/api/v1/matches/$MATCH_ID/events" | jq length)" "4" \
  "reenvio não gera evento"
expect_eq "$(grep -c 'Cenário nordeste-teste carregado.*UNCHANGED' backend.log)" "1" \
  "recarga do cenário no reinício não regrava o banco"
stop_backend

echo "Smoke com PostgreSQL concluído"
