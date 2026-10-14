-- Opcional: alternativa ao ddl-auto=update em um banco já contendo clientes.
-- Reconciliar com o modelo de orçamento da equipe antes de aplicar em outro schema.
BEGIN;
CREATE TABLE IF NOT EXISTS orcamentos (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    status VARCHAR(30) NOT NULL,
    valor_total NUMERIC(19,2) NOT NULL,
    data_expiracao TIMESTAMP WITH TIME ZONE,
    token_hash VARCHAR(64),
    token_utilizado_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_orcamentos_token_hash UNIQUE(token_hash),
    CONSTRAINT ck_orcamentos_status CHECK(status IN ('RASCUNHO','AGUARDANDO_APROVACAO','APROVADO','REJEITADO'))
);
CREATE TABLE IF NOT EXISTS orcamento_itens (
    id UUID PRIMARY KEY,
    orcamento_id UUID NOT NULL REFERENCES orcamentos(id),
    ordem INTEGER NOT NULL,
    tipo_servico VARCHAR(150) NOT NULL,
    quantidade NUMERIC(19,4) NOT NULL,
    unidade VARCHAR(50) NOT NULL,
    valor_unitario NUMERIC(19,6) NOT NULL,
    subtotal NUMERIC(19,2) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_orcamento_itens_orcamento ON orcamento_itens(orcamento_id);
CREATE TABLE IF NOT EXISTS orcamento_historico (
    id UUID PRIMARY KEY,
    orcamento_id UUID NOT NULL REFERENCES orcamentos(id),
    status_anterior VARCHAR(30) NOT NULL,
    status_novo VARCHAR(30) NOT NULL,
    motivo VARCHAR(2000),
    data_decisao TIMESTAMP WITH TIME ZONE NOT NULL,
    origem VARCHAR(30) NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_orcamento_historico_orcamento ON orcamento_historico(orcamento_id);
CREATE TABLE IF NOT EXISTS orcamento_decisao_outbox (
    id UUID PRIMARY KEY,
    orcamento_id UUID NOT NULL REFERENCES orcamentos(id),
    routing_key VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    publicado_em TIMESTAMP WITH TIME ZONE,
    proxima_tentativa_em TIMESTAMP WITH TIME ZONE NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_orcamento_outbox_pendente
    ON orcamento_decisao_outbox(publicado_em,proxima_tentativa_em,criado_em);
CREATE TABLE IF NOT EXISTS orcamento_email_outbox (
    id UUID PRIMARY KEY,
    orcamento_id UUID NOT NULL REFERENCES orcamentos(id),
    payload TEXT,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    enviado_em TIMESTAMP WITH TIME ZONE,
    proxima_tentativa_em TIMESTAMP WITH TIME ZONE NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_orcamento_email_outbox_pendente
    ON orcamento_email_outbox(enviado_em,proxima_tentativa_em,criado_em);
COMMIT;
