-- US-011: estruturação das etapas da Ordem de Serviço e os modelos de workflow.
-- Referência do que o Hibernate gera com ddl-auto: update, para conferência e para
-- bancos que já existem. Em ambiente novo as tabelas sobem sozinhas; o ALTER do final
-- NÃO sobe sozinho e é obrigatório em qualquer banco criado antes desta US.

-- ---------------------------------------------------------------- modelos de workflow

CREATE TABLE IF NOT EXISTS modelos_workflow (
    id          UUID PRIMARY KEY,
    nome        VARCHAR(120) NOT NULL,
    descricao   VARCHAR(500),
    ativo       BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uk_modelos_workflow_nome UNIQUE (nome)
);

CREATE TABLE IF NOT EXISTS modelos_workflow_etapas (
    id                   UUID PRIMARY KEY,
    modelo_id            UUID NOT NULL REFERENCES modelos_workflow (id),
    tipo_servico_id      UUID NOT NULL REFERENCES tipos_servico (id),
    nome                 VARCHAR(120) NOT NULL,
    ordem                INTEGER NOT NULL,
    prazo_estimado_horas INTEGER NOT NULL,
    CONSTRAINT uk_modelos_workflow_etapas_ordem UNIQUE (modelo_id, ordem)
);

-- ---------------------------------------------------------------- etapas da OS

CREATE TABLE IF NOT EXISTS etapas_os (
    id                   UUID PRIMARY KEY,
    os_id                UUID NOT NULL REFERENCES ordens_servico (id),
    tipo_servico_id      UUID NOT NULL REFERENCES tipos_servico (id),
    -- Preenchido pela US-012. Enquanto for nulo, a sequência ainda pode ser remontada (RN.3).
    recurso_id           UUID REFERENCES recursos (id),
    nome                 VARCHAR(120) NOT NULL,
    ordem                INTEGER NOT NULL,
    status               VARCHAR(30) NOT NULL,
    prazo_estimado_horas INTEGER NOT NULL,
    criado_em            TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_etapas_os_ordem UNIQUE (os_id, ordem),
    CONSTRAINT ck_etapas_os_status CHECK (status IN ('PENDENTE', 'AGUARDANDO_ETAPA_ANTERIOR'))
);

-- A listagem sempre filtra por OS e ordena pela ordem; o índice cobre as duas coisas.
CREATE INDEX IF NOT EXISTS ix_etapas_os_os_ordem ON etapas_os (os_id, ordem);

-- ---------------------------------------------------------------- OBRIGATÓRIO em banco existente

-- O ddl-auto: update cria tabela e coluna, mas NUNCA altera um CHECK que já existe.
-- Sem este ALTER, gravar as etapas falha com violação de CHECK ao tentar mover a OS para
-- EM_PLANEJAMENTO. Rodar uma vez, antes de subir a aplicação.
--
-- São dois DROP porque o nome depende de quem criou a constraint: o Hibernate gera
-- <tabela>_<coluna>_check, e bancos que já passaram por um ajuste manual podem ter também
-- ck_ordens_servico_status. Dropar só um deixa o outro barrando o valor novo em silêncio.

ALTER TABLE ordens_servico DROP CONSTRAINT IF EXISTS ordens_servico_status_check;
ALTER TABLE ordens_servico DROP CONSTRAINT IF EXISTS ck_ordens_servico_status;
ALTER TABLE ordens_servico ADD CONSTRAINT ck_ordens_servico_status
    CHECK (status IN ('PENDENTE_ALOCACAO', 'EM_PLANEJAMENTO'));

-- Conferência — tem que sobrar exatamente uma linha, com os dois valores:
--   SELECT conname, pg_get_constraintdef(oid) FROM pg_constraint
--    WHERE contype = 'c' AND conrelid = 'ordens_servico'::regclass;

-- Nota para a US-012: ao acrescentar os estados de execução da etapa
-- (EM_EXECUCAO, CONCLUIDA e afins), o ck_etapas_os_status precisa do mesmo tratamento.
