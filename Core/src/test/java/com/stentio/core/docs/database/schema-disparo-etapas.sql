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

-- ---------------------------------------------------------------- outbox das notificações de etapa

-- Sobe sozinha com ddl-auto; fica aqui como referência do que o Hibernate gera.
CREATE TABLE IF NOT EXISTS notificacao_etapa_outbox (
    id                   UUID PRIMARY KEY,
    etapa_id             UUID NOT NULL REFERENCES etapas_os (id),
    destinatario         VARCHAR(150) NOT NULL,
    payload              TEXT,
    criado_em            TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    enviado_em           TIMESTAMP(6) WITH TIME ZONE,
    proxima_tentativa_em TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    tentativas           INTEGER NOT NULL DEFAULT 0
);

-- O dispatcher procura sempre por pendente e vencido; sem o índice, a varredura cresce
-- junto com o histórico de notificações já enviadas.
CREATE INDEX IF NOT EXISTS ix_notificacao_etapa_outbox_pendentes
    ON notificacao_etapa_outbox (enviado_em, proxima_tentativa_em);

-- ---------------------------------------------------------------- histórico da etapa

-- O histórico nasceu na US-012 descrevendo só alocação, com recurso e prazo obrigatórios.
-- O disparo não é dirigido a ninguém e a etapa pode não ter prazo ainda, então as duas
-- colunas passam a aceitar nulo. O ddl-auto não remove NOT NULL, então isto é obrigatório.

ALTER TABLE etapas_os_historico ALTER COLUMN recurso_novo_id      DROP NOT NULL;
ALTER TABLE etapas_os_historico ALTER COLUMN data_limite_entrega  DROP NOT NULL;

-- E o CHECK do evento, pelo mesmo motivo do CHECK do status: ddl-auto nunca altera um
-- CHECK que já existe, e sem isto o primeiro disparo falha na gravação do histórico.
ALTER TABLE etapas_os_historico DROP CONSTRAINT IF EXISTS etapas_os_historico_evento_check;
ALTER TABLE etapas_os_historico DROP CONSTRAINT IF EXISTS ck_etapas_os_historico_evento;
ALTER TABLE etapas_os_historico ADD CONSTRAINT ck_etapas_os_historico_evento CHECK (evento IN (
    'ALOCACAO',
    'REALOCACAO',
    'DISPARO',
    'ACEITE',
    'RECUSA',
    'ENTREGA'
));
