-- Executar com o Core parado, antes de subir a versão com os novos status.
-- Reutiliza os vínculos existentes; não altera orçamentos nem ordens de serviço.
-- A restrição antiga gerada pelo Hibernate não aceita os novos valores.
BEGIN;

ALTER TABLE solicitacoes DROP CONSTRAINT IF EXISTS solicitacoes_status_check;

UPDATE solicitacoes s
SET status = CASE o.status
    WHEN 'APROVADO' THEN 'EM_EXECUCAO'
    WHEN 'REJEITADO' THEN 'ORCAMENTO_REJEITADO'
    ELSE 'AGUARDANDO_APROVACAO'
END
FROM orcamentos o
WHERE o.solicitacao_id = s.id;

UPDATE solicitacoes s
SET status = 'PENDENTE_ORCAMENTO'
WHERE NOT EXISTS (SELECT 1 FROM orcamentos o WHERE o.solicitacao_id = s.id);

ALTER TABLE solicitacoes ADD CONSTRAINT solicitacoes_status_check
    CHECK (status IN ('PENDENTE_ORCAMENTO', 'AGUARDANDO_APROVACAO', 'ORCAMENTO_REJEITADO', 'EM_EXECUCAO'));

COMMIT;
