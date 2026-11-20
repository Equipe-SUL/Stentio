-- US-023: o papel RECURSO precisa caber no check constraint de usuarios.role.
--
-- RODE O ARQUIVO INTEIRO:
--
--   docker compose exec -T postgres psql -U postgres -d stentio < schema-role-recurso.sql
--
-- O ddl-auto: update cria a tabela em banco novo já com o enum completo, mas não altera
-- um CHECK existente. Sem este ALTER, o cadastro público falha com
-- "violates check constraint usuarios_role_check" no primeiro deploy sobre banco existente.

ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_role_check;

ALTER TABLE usuarios
    ADD CONSTRAINT usuarios_role_check
    CHECK (role IN ('ADMIN', 'ATENDENTE', 'GESTOR_PROJETO', 'FINANCEIRO', 'RECURSO'));

-- Conferência — deve listar a constraint com os cinco papéis:
--   SELECT pg_get_constraintdef(oid) FROM pg_constraint WHERE conname = 'usuarios_role_check';
