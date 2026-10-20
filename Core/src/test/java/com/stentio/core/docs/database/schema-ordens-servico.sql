-- US-16: migration manual, seguindo schema-orcamentos.sql.
-- Executar depois da criação de solicitacoes e orcamentos, com o Core parado.
-- Em desenvolvimento, ddl-auto=update também cria esta tabela a partir da entidade.
BEGIN;
CREATE TABLE IF NOT EXISTS ordens_servico (
    id UUID PRIMARY KEY,
    solicitacao_id UUID NOT NULL REFERENCES solicitacoes(id),
    orcamento_id UUID NOT NULL REFERENCES orcamentos(id),
    status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP(6) WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT uk_ordens_servico_orcamento UNIQUE (orcamento_id),
    CONSTRAINT ck_ordens_servico_status CHECK (status IN ('PENDENTE_ALOCACAO'))
);
COMMIT;
