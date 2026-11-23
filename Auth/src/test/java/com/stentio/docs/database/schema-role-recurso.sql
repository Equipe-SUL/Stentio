-- US-023: o papel RECURSO no CHECK da tabela usuarios.
--
-- RODE O ARQUIVO INTEIRO, em todo ambiente que já tinha a tabela:
--
--   docker compose exec -T postgres psql -U postgres -d stentio < schema-role-recurso.sql
--
-- Por que é obrigatório: ddl-auto: update acrescenta colunas e tabelas, mas NUNCA altera
-- um CHECK que já existe. A US-023 acrescentou RECURSO ao enum Role; em um banco criado
-- antes disso, o CHECK continua listando só os quatro papéis antigos e o INSERT do
-- auto-cadastro é recusado pelo banco.
--
-- Em banco novo, criado depois da US-023, o Hibernate já gera o CHECK completo e este
-- arquivo não muda nada — ele é idempotente.

-- São dois DROP porque o nome depende de quem criou a constraint: o Hibernate gera
-- <tabela>_<coluna>_check e um ajuste manual anterior pode ter deixado ck_<tabela>_<coluna>.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_role_check;
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS ck_usuarios_role;

ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_role CHECK (role IN (
    'ADMIN',
    'ATENDENTE',
    'GESTOR_PROJETO',
    'FINANCEIRO',
    'RECURSO'
));

-- Conferência — deve sobrar uma linha, com os cinco papéis:
--   SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE contype = 'c' AND conrelid = 'usuarios'::regclass;
