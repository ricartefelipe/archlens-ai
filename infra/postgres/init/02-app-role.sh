#!/bin/bash
set -euo pipefail
# Role da aplicação, sem superuser, para que o RLS por tenant valha.
#
# O Liquibase cria o role e concede os privilégios (changeset 009); aqui apenas
# definimos a senha, que vem do ambiente e não do repositório. Roda só na
# criação do cluster: num upgrade de instalação existente, defina a senha à mão
# (ALTER ROLE archlens_app PASSWORD '…') conforme o runbook.
#
# Sem ARCHLENS_APP_DB_PASSWORD definida, o role fica sem senha e a aplicação não
# conecta — falha explícita, melhor que um segredo padrão compartilhado.

if [[ -z "${ARCHLENS_APP_DB_PASSWORD:-}" ]]; then
  echo "02-app-role: ARCHLENS_APP_DB_PASSWORD não definida; role da aplicação ficará sem senha." >&2
  exit 0
fi

psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    DO \$\$
    BEGIN
      IF NOT EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'archlens_app') THEN
        CREATE ROLE archlens_app LOGIN NOSUPERUSER NOBYPASSRLS NOCREATEDB NOCREATEROLE;
      END IF;
    END \$\$;

    ALTER ROLE archlens_app PASSWORD '${ARCHLENS_APP_DB_PASSWORD}';
EOSQL

echo "02-app-role: role archlens_app pronto (senha definida pelo ambiente)."
