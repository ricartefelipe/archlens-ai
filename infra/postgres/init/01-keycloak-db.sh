#!/bin/bash
set -euo pipefail
# Banco dedicado ao Keycloak (mesmo cluster Postgres da aplicação).
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" <<-EOSQL
    SELECT 'CREATE DATABASE keycloak OWNER archlens'
    WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'keycloak')\gexec
EOSQL
