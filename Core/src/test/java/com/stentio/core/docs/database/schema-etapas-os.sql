-- US-011: estruturação das etapas da Ordem de Serviço e os modelos de workflow.
-- Referência do que o Hibernate gera com ddl-auto: update, para conferência e para
-- bancos que já existem. Em ambiente novo as tabelas sobem sozinhas.

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

-- Nota para a US-012: ao acrescentar os estados de execução da etapa
-- (EM_EXECUCAO, CONCLUIDA e afins), o ck_etapas_os_status precisa do mesmo tratamento
-- descrito no final deste arquivo para o ck_ordens_servico_status.
