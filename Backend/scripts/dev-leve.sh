#!/usr/bin/env bash
# Sobe o Backend consumindo pouca memória (perfil dev-leve).
# Uso: scripts/dev-leve.sh [--skip-build] [--max-memory 384m]
# A conexão do banco vem de application-local.yml ou de DB_URL/DB_USERNAME/DB_PASSWORD.
set -euo pipefail

skip_build=false
max_memory=384m
while [ $# -gt 0 ]; do
  case "$1" in
    --skip-build) skip_build=true ;;
    --max-memory) max_memory="$2"; shift ;;
    *) echo "Opção desconhecida: $1" >&2; exit 2 ;;
  esac
  shift
done

cd "$(dirname "$0")/.."
if [ ! -f src/main/resources/application-local.yml ] && [ -z "${DB_URL:-}" ]; then
  echo "Aviso: sem application-local.yml e sem DB_URL; será usado o PostgreSQL local padrão." >&2
fi
if [ "$skip_build" = false ]; then
  echo "Compilando o Backend (sem testes)..."
  mvn -q -DskipTests package
fi

jar=$(ls target/*.jar | grep -v -- '-plain.jar' | head -1)
echo "Subindo $(basename "$jar") com até $max_memory de heap (perfis local,dev-leve)..."
exec java "-Xmx$max_memory" -Xss512k -XX:+UseSerialGC -XX:TieredStopAtLevel=1 \
  -jar "$jar" --spring.profiles.active=local,dev-leve
