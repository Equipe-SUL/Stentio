-- US-012 CA.4: histórico de alocação das etapas.
-- A tabela sobe sozinha com ddl-auto: update. O que NÃO sobe sozinho é alteração de CHECK
-- em banco que já tenha esta tabela — ver a nota no final.

CREATE TABLE IF NOT EXISTS etapas_os_historico (
    id                   UUID PRIMARY KEY,
    etapa_id             UUID NOT NULL REFERENCES etapas_os (id),
    evento               VARCHAR(30) NOT NULL,
    -- Nulo na primeira alocação; preenchido quando houve troca, que é o caso interessante.
    recurso_anterior_id  UUID REFERENCES recursos (id),
    recurso_novo_id      UUID NOT NULL REFERENCES recursos (id),
    data_limite_entrega  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    -- E-mail, e não id de usuário: o JWT emitido pelo Auth traz e-mail e perfil, e nenhum
    -- id de usuário atravessa a fronteira entre os dois serviços.
    realizado_por        VARCHAR(150) NOT NULL,
    realizado_em         TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_etapas_os_historico_evento CHECK (evento IN ('ALOCACAO', 'REALOCACAO'))
);

CREATE INDEX IF NOT EXISTS ix_etapas_os_historico_etapa
    ON etapas_os_historico (etapa_id);

-- ---------------------------------------------------------------- nota para as US-013 e US-014

-- Cada evento novo em EventoEtapa (aceite, recusa, entrega…) exige recriar este CHECK em
-- qualquer banco que já exista, porque o ddl-auto: update nunca altera um CHECK existente:
--
--   ALTER TABLE etapas_os_historico DROP CONSTRAINT IF EXISTS etapas_os_historico_evento_check;
--   ALTER TABLE etapas_os_historico DROP CONSTRAINT IF EXISTS ck_etapas_os_historico_evento;
--   ALTER TABLE etapas_os_historico ADD CONSTRAINT ck_etapas_os_historico_evento
--       CHECK (evento IN ('ALOCACAO', 'REALOCACAO', ...os novos...));
--
-- São dois DROP porque o nome depende de quem criou: o Hibernate gera
-- <tabela>_<coluna>_check e o ajuste manual costuma usar ck_<tabela>_<coluna>.
