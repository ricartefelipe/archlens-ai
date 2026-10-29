#!/usr/bin/env bash
# Prepara o role da aplicação para o RLS efetivo (release 0.4.0) e verifica que
# o isolamento por tenant realmente passou a valer.
#
# Necessário em instalação que já existia antes da 0.4.0: a migration cria o
# role archlens_app e os privilégios, mas não define senha — de propósito, para
# não haver segredo padrão. Este script define a senha e comprova o resultado.
#
# Uso (na máquina onde o Compose roda, com o Postgres de pé):
#   ARCHLENS_APP_DB_PASSWORD='<segredo>' ./scripts/upgrade-rls-app-role.sh
#
# Variáveis:
#   ARCHLENS_APP_DB_PASSWORD  obrigatória — senha do role da aplicação
#   PG_CONTAINER              nome do container do Postgres (default: archlens-postgres)
#   PG_SUPERUSER              owner do schema (default: archlens)
#   PG_DATABASE               banco (default: archlens)
#   COMPOSE_FILE              compose a usar se precisar subir (default: docker-compose.prod.yml)
#
# Saída: 0 upgrade verificado | 1 falha | 2 pré-requisito ausente (inconclusivo).
set -euo pipefail

PG_CONTAINER="${PG_CONTAINER:-archlens-postgres}"
PG_SUPERUSER="${PG_SUPERUSER:-archlens}"
PG_DATABASE="${PG_DATABASE:-archlens}"

fail()        { echo "✖ $*" >&2; exit 1; }
inconclusive(){ echo "⚠ $*" >&2; exit 2; }

if [[ -z "${ARCHLENS_APP_DB_PASSWORD:-}" ]]; then
  inconclusive "Defina ARCHLENS_APP_DB_PASSWORD com a senha do role da aplicação (a mesma que irá no ambiente do backend)."
fi

if ! command -v docker >/dev/null 2>&1; then
  inconclusive "docker não encontrado no PATH."
fi

if ! docker exec "$PG_CONTAINER" pg_isready -U "$PG_SUPERUSER" >/dev/null 2>&1; then
  inconclusive "Container '$PG_CONTAINER' não responde. Suba o Postgres antes (docker compose -f \${COMPOSE_FILE:-docker-compose.prod.yml} up -d postgres)."
fi

psql_su() { docker exec -i "$PG_CONTAINER" psql -U "$PG_SUPERUSER" -d "$PG_DATABASE" -tAq "$@"; }

echo "▸ 1/5 Conferindo que a migration 009 já rodou"
APPLIED=$(psql_su -c "SELECT count(*) FROM databasechangelog WHERE id LIKE '009-%';" 2>/dev/null || echo 0)
if [[ "${APPLIED//[^0-9]/}" -lt 3 ]]; then
  inconclusive "A migration 009 não consta aplicada (encontrados: ${APPLIED}). Suba o backend 0.4.0 uma vez para o Liquibase aplicá-la, depois rode este script."
fi
echo "  ok: changesets 009 aplicados"

echo "▸ 2/5 Definindo a senha do role archlens_app"
docker exec -i "$PG_CONTAINER" psql -U "$PG_SUPERUSER" -d "$PG_DATABASE" -v ON_ERROR_STOP=1 \
  -v pwd="$ARCHLENS_APP_DB_PASSWORD" >/dev/null <<'EOSQL'
DO $$
BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'archlens_app') THEN
    CREATE ROLE archlens_app LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
  ELSE
    ALTER ROLE archlens_app NOSUPERUSER NOBYPASSRLS;
  END IF;
END $$;
EOSQL
docker exec -i "$PG_CONTAINER" psql -U "$PG_SUPERUSER" -d "$PG_DATABASE" -v ON_ERROR_STOP=1 >/dev/null <<EOSQL
ALTER ROLE archlens_app PASSWORD '${ARCHLENS_APP_DB_PASSWORD//\'/\'\'}';
EOSQL
echo "  ok: senha definida e role sem superuser/bypassrls"

echo "▸ 3/5 Conferindo FORCE ROW LEVEL SECURITY"
NOT_FORCED=$(psql_su -c "SELECT count(*) FROM pg_class c JOIN pg_namespace n ON n.oid=c.relnamespace WHERE n.nspname='public' AND c.relkind='r' AND c.relrowsecurity AND NOT c.relforcerowsecurity;")
[[ "${NOT_FORCED//[^0-9]/}" -eq 0 ]] || fail "Há ${NOT_FORCED} tabela(s) com RLS sem FORCE — a migration 009 não completou."
echo "  ok: todas as tabelas com RLS também têm FORCE"

echo "▸ 4/5 Provando o isolamento com o role da aplicação"
export PGPASSWORD_APP="$ARCHLENS_APP_DB_PASSWORD"
app_psql() {
  docker exec -i -e PGPASSWORD="$ARCHLENS_APP_DB_PASSWORD" "$PG_CONTAINER" \
    psql -h 127.0.0.1 -U archlens_app -d "$PG_DATABASE" -tAq "$@"
}

if ! app_psql -c "SELECT 1" >/dev/null 2>&1; then
  fail "Não foi possível conectar como archlens_app. Confira pg_hba.conf (precisa aceitar senha para host 127.0.0.1)."
fi

SEM_TENANT=$(app_psql -c "SELECT count(*) FROM projects;")
[[ "${SEM_TENANT//[^0-9]/}" -eq 0 ]] || fail "Sem tenant no contexto, o role ainda vê ${SEM_TENANT} projeto(s): o RLS não está isolando."
echo "  ok: sem tenant no contexto, nenhuma linha visível"

TOTAL=$(psql_su -c "SELECT set_config('app.rls_bypass','on',false); SELECT count(*) FROM projects;" | tail -1)
ALGUM_TENANT=$(psql_su -c "SELECT set_config('app.rls_bypass','on',false); SELECT tenant_id FROM projects LIMIT 1;" | tail -1)
if [[ -n "$ALGUM_TENANT" ]]; then
  VISTOS=$(app_psql -c "SELECT set_config('app.current_tenant','${ALGUM_TENANT}',false); SELECT count(*) FROM projects;" | tail -1)
  DO_TENANT=$(psql_su -c "SELECT set_config('app.rls_bypass','on',false); SELECT count(*) FROM projects WHERE tenant_id='${ALGUM_TENANT}';" | tail -1)
  [[ "${VISTOS//[^0-9]/}" -eq "${DO_TENANT//[^0-9]/}" ]] \
    || fail "Com tenant '${ALGUM_TENANT}' o role vê ${VISTOS}, mas o tenant tem ${DO_TENANT} projeto(s)."
  echo "  ok: com tenant '${ALGUM_TENANT}' vê exatamente os ${VISTOS} dele (de ${TOTAL} no total)"
else
  echo "  (banco sem projetos: isolamento positivo não exercitado, o negativo acima vale)"
fi

echo "▸ 5/5 Conferindo que gravar em outro tenant é recusado"
if [[ -n "$ALGUM_TENANT" ]]; then
  OUTRO="tenant-inexistente-$RANDOM"
  if app_psql -c "SELECT set_config('app.current_tenant','${OUTRO}',false); INSERT INTO projects (id,tenant_id,name) VALUES (gen_random_uuid(),'${ALGUM_TENANT}','rls-check');" >/dev/null 2>&1; then
    psql_su -c "SELECT set_config('app.rls_bypass','on',false); DELETE FROM projects WHERE name='rls-check';" >/dev/null
    fail "O role conseguiu gravar linha de outro tenant: a cláusula WITH CHECK não está ativa."
  fi
  echo "  ok: INSERT com tenant alheio recusado pela política"
fi

cat <<FIM

✔ RLS efetivo e verificado.

Próximo passo, no ambiente do backend:
  ARCHLENS_APP_DB_PASSWORD='<a senha que você acabou de definir>'

O backend passa a conectar como archlens_app; as migrations seguem com o owner
via ARCHLENS_MIGRATION_DB_USERNAME/ARCHLENS_MIGRATION_DB_PASSWORD.
Detalhes em docs/RUNBOOK-PRODUCAO.md secao 9.
FIM
