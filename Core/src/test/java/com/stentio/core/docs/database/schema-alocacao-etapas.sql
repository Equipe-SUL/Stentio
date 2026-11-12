-- US-012: alocação de recurso às etapas da Ordem de Serviço.
-- Referência do que o Hibernate gera com ddl-auto: update. As colunas sobem sozinhas;
-- em banco que já existe, confira que apareceram antes de usar a tela.

-- ---------------------------------------------------------------- colunas de alocação

-- recurso_id já veio na US-011, para a RN.3 daquela US. O que a US-012 acrescenta:
ALTER TABLE etapas_os ADD COLUMN IF NOT EXISTS data_limite_entrega TIMESTAMP(6) WITH TIME ZONE;
ALTER TABLE etapas_os ADD COLUMN IF NOT EXISTS alocado_em          TIMESTAMP(6) WITH TIME ZONE;

-- A tela de alocação procura por recurso; sem índice vira varredura na tabela de etapas.
CREATE INDEX IF NOT EXISTS ix_etapas_os_recurso ON etapas_os (recurso_id);

-- Nada a fazer no ck_etapas_os_status: ALOCADO é situação calculada na API, não valor
-- gravado na coluna. O status continua sendo PENDENTE ou AGUARDANDO_ETAPA_ANTERIOR, que
-- é o que diz qual etapa pode começar — informação que a US-014 vai precisar.
