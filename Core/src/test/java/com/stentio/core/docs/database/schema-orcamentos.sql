-- Opcional: alternativa ao ddl-auto=update em um banco já contendo clientes.
-- Reconciliar com o modelo de orçamento da equipe antes de aplicar em outro schema.
-- US-11: um item é um par de idiomas (orcamento_item) com serviços precificados (orcamento_item_servico).
--        A antiga tabela orcamento_itens (colunas de serviço na própria linha) foi substituída por elas.
-- StatusOrcamento: EM_ELABORACAO (inicial), AGUARDANDO_APROVACAO, APROVADO, REJEITADO.
BEGIN;
CREATE TABLE IF NOT EXISTS orcamentos (
    id UUID PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    numero_versao INTEGER NOT NULL DEFAULT 1,
    cliente_id UUID NOT NULL REFERENCES clientes(id),
    solicitacao_id UUID REFERENCES solicitacoes(id),
    status VARCHAR(30) NOT NULL,
    valor_total NUMERIC(19,2) NOT NULL,
    data_expiracao TIMESTAMP WITH TIME ZONE,
    token_hash VARCHAR(64),
    token_utilizado_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_orcamentos_token_hash UNIQUE(token_hash),
    CONSTRAINT ck_orcamentos_status CHECK(status IN ('EM_ELABORACAO','AGUARDANDO_APROVACAO','APROVADO','REJEITADO'))
);
CREATE INDEX IF NOT EXISTS idx_orcamentos_status ON orcamentos(status);
CREATE INDEX IF NOT EXISTS idx_orcamentos_cliente ON orcamentos(cliente_id);
CREATE INDEX IF NOT EXISTS idx_orcamentos_solicitacao ON orcamentos(solicitacao_id);
CREATE TABLE IF NOT EXISTS orcamento_item (
    id UUID PRIMARY KEY,
    orcamento_id UUID NOT NULL REFERENCES orcamentos(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    idioma_origem_id UUID NOT NULL REFERENCES idiomas(id),
    idioma_destino_id UUID NOT NULL REFERENCES idiomas(id),
    subtotal_item NUMERIC(14,2) NOT NULL,
    CONSTRAINT uk_orcamento_item_ordem UNIQUE(orcamento_id, ordem),
    CONSTRAINT ck_orcamento_item_idiomas_diferentes CHECK(idioma_origem_id <> idioma_destino_id)
);
CREATE INDEX IF NOT EXISTS idx_orcamento_item_orcamento ON orcamento_item(orcamento_id);
CREATE TABLE IF NOT EXISTS orcamento_item_servico (
    id UUID PRIMARY KEY,
    orcamento_item_id UUID NOT NULL REFERENCES orcamento_item(id) ON DELETE CASCADE,
    tipo_servico VARCHAR(150) NOT NULL,
    quantidade NUMERIC(14,2) NOT NULL,
    unidade VARCHAR(20) NOT NULL,
    valor_unitario NUMERIC(14,4) NOT NULL,
    subtotal NUMERIC(14,2) NOT NULL,
    CONSTRAINT ck_orcamento_item_servico_valores CHECK(quantidade > 0 AND valor_unitario > 0),
    CONSTRAINT ck_orcamento_item_servico_unidade CHECK(unidade IN ('PALAVRA','HORA','PAGINA'))
);
CREATE INDEX IF NOT EXISTS idx_orcamento_item_servico_item ON orcamento_item_servico(orcamento_item_id);
CREATE TABLE IF NOT EXISTS orcamento_historico (
    id UUID PRIMARY KEY,
    orcamento_id UUID NOT NULL REFERENCES orcamentos(id),
    status_anterior VARCHAR(30) NOT NULL,
    status_novo VARCHAR(30) NOT NULL,
    motivo VARCHAR(2000),
    data_decisao TIMESTAMP WITH TIME ZONE NOT NULL,
    origem VARCHAR(30) NOT NULL,
    CONSTRAINT ck_orcamento_historico_status_anterior CHECK(status_anterior IN ('EM_ELABORACAO','AGUARDANDO_APROVACAO','APROVADO','REJEITADO')),
    CONSTRAINT ck_orcamento_historico_status_novo CHECK(status_novo IN ('EM_ELABORACAO','AGUARDANDO_APROVACAO','APROVADO','REJEITADO'))
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
