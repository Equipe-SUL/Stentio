-- US-013: disparo, aceite e entrega da etapa.
--
-- RODE O ARQUIVO INTEIRO:
--
--   docker compose exec -T postgres psql -U postgres -d stentio < schema-disparo-etapas.sql
--
-- As colunas sobem com ddl-auto: update, mas o CHECK do status NÃO — e sem ele a primeira
-- alocação falha com violação de constraint, porque ALOCADO não está na lista antiga.

-- ---------------------------------------------------------------- colunas novas

ALTER TABLE etapas_os ADD COLUMN IF NOT EXISTS disparada_em            TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE etapas_os ADD COLUMN IF NOT EXISTS aceito_em               TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE etapas_os ADD COLUMN IF NOT EXISTS entregue_em             TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE etapas_os ADD COLUMN IF NOT EXISTS arquivo_entregue_nome    VARCHAR(255);
ALTER TABLE etapas_os ADD COLUMN IF NOT EXISTS arquivo_entregue_caminho VARCHAR(500);

-- ---------------------------------------------------------------- OBRIGATÓRIO: o CHECK do status

-- São dois DROP porque o nome depende de quem criou a constraint: o Hibernate gera
-- <tabela>_<coluna>_check e um ajuste manual anterior pode ter deixado ck_<tabela>_<coluna>.
ALTER TABLE etapas_os DROP CONSTRAINT IF EXISTS etapas_os_status_check;
ALTER TABLE etapas_os DROP CONSTRAINT IF EXISTS ck_etapas_os_status;
ALTER TABLE etapas_os ADD CONSTRAINT ck_etapas_os_status CHECK (status IN (
    'PENDENTE',
    'AGUARDANDO_ETAPA_ANTERIOR',
    'ALOCADO',
    'DISPARADA',
    'ACEITA',
    'ENTREGUE'
));

-- Conferência — deve sobrar exatamente uma linha, com os seis valores:
--   SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE contype = 'c' AND conrelid = 'etapas_os'::regclass;

-- ---------------------------------------------------------------- etapas já alocadas

-- Na US-012 a etapa alocada ficava com status PENDENTE e o recurso preenchido; ALOCADO
-- era calculado na resposta. Agora o estado é gravado, então as que já existem precisam
-- ser trazidas para o modelo novo — senão aparecem como livres para disparo.

UPDATE etapas_os SET status = 'ALOCADO' WHERE recurso_id IS NOT NULL AND status = 'PENDENTE';
