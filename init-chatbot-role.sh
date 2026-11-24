#!/usr/bin/env bash
set -Eeuo pipefail

: "${POSTGRES_USER:?POSTGRES_USER must be configured}"
: "${POSTGRES_DB:?POSTGRES_DB must be configured}"
: "${CHATBOT_DB_USER:?CHATBOT_DB_USER must be configured}"
: "${CHATBOT_DB_PASSWORD:?CHATBOT_DB_PASSWORD must be configured}"

if [[ "$CHATBOT_DB_USER" == "$POSTGRES_USER" ]]; then
    printf 'CHATBOT_DB_USER must be different from POSTGRES_USER.\n' >&2
    exit 1
fi

psql --username "$POSTGRES_USER" --dbname "$POSTGRES_DB" --set=ON_ERROR_STOP=1 <<'SQL'
\getenv chatbot_db_user CHATBOT_DB_USER
\getenv chatbot_db_password CHATBOT_DB_PASSWORD

SELECT format('CREATE ROLE %I LOGIN PASSWORD %L', :'chatbot_db_user', :'chatbot_db_password')
WHERE NOT EXISTS (
    SELECT 1 FROM pg_catalog.pg_roles WHERE rolname = :'chatbot_db_user'
)
\gexec

SELECT format('GRANT CONNECT ON DATABASE %I TO %I', current_database(), :'chatbot_db_user')
\gexec

SELECT format('GRANT USAGE ON SCHEMA public TO %I', :'chatbot_db_user')
\gexec

SELECT format('GRANT SELECT ON ALL TABLES IN SCHEMA public TO %I', :'chatbot_db_user')
\gexec

SELECT format(
    'ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO %I',
    :'chatbot_db_user'
)
\gexec
SQL
