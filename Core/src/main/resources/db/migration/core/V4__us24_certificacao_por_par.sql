-- US-024: certificação do profissional por par de idiomas.
--
-- V4 e não V3: o V3 está reservado para a US-015, que já está em revisão. O Flyway não
-- exige numeração contígua, então a ordem funciona com ou sem ela.

CREATE TABLE public.recursos_certificacoes (
    id uuid NOT NULL,
    recurso_id uuid NOT NULL,
    idioma_origem_id uuid NOT NULL,
    idioma_destino_id uuid NOT NULL,
    arquivo_nome character varying(255) NOT NULL,
    arquivo_caminho character varying(500) NOT NULL,
    status character varying(20) NOT NULL,
    enviado_em timestamp(6) with time zone NOT NULL,
    analisado_em timestamp(6) with time zone,
    analisado_por character varying(150),
    motivo_rejeicao character varying(1000),
    CONSTRAINT ck_recursos_certificacoes_status CHECK (
        status IN ('PENDENTE', 'APROVADA', 'REJEITADA')
    )
);

ALTER TABLE ONLY public.recursos_certificacoes
    ADD CONSTRAINT recursos_certificacoes_pkey PRIMARY KEY (id);

-- RN.2: uma certificação por par e por recurso. O reenvio reaproveita a linha.
ALTER TABLE ONLY public.recursos_certificacoes
    ADD CONSTRAINT uk_recursos_certificacoes_par
        UNIQUE (recurso_id, idioma_origem_id, idioma_destino_id);

ALTER TABLE ONLY public.recursos_certificacoes
    ADD CONSTRAINT fk_recursos_certificacoes_recurso
        FOREIGN KEY (recurso_id) REFERENCES public.recursos(id);

ALTER TABLE ONLY public.recursos_certificacoes
    ADD CONSTRAINT fk_recursos_certificacoes_idioma_origem
        FOREIGN KEY (idioma_origem_id) REFERENCES public.idiomas(id);

ALTER TABLE ONLY public.recursos_certificacoes
    ADD CONSTRAINT fk_recursos_certificacoes_idioma_destino
        FOREIGN KEY (idioma_destino_id) REFERENCES public.idiomas(id);

-- A fila do gestor filtra por status e ordena pela chegada; a elegibilidade pergunta
-- pelo par aprovado de um recurso. Os dois índices cobrem essas duas perguntas.
CREATE INDEX ix_recursos_certificacoes_fila
    ON public.recursos_certificacoes USING btree (status, enviado_em);

CREATE INDEX ix_recursos_certificacoes_par_aprovado
    ON public.recursos_certificacoes USING btree (recurso_id, status);

-- Outbox do aviso de rejeição. Tabela própria porque a notificacao_etapa_outbox tem FK
-- obrigatória para etapa, e aqui não há etapa nenhuma — é um aviso sobre o cadastro.
CREATE TABLE public.certificacao_email_outbox (
    id uuid NOT NULL,
    certificacao_id uuid NOT NULL,
    destinatario character varying(150) NOT NULL,
    payload text,
    criado_em timestamp(6) with time zone NOT NULL,
    enviado_em timestamp(6) with time zone,
    proxima_tentativa_em timestamp(6) with time zone NOT NULL,
    tentativas integer NOT NULL
);

ALTER TABLE ONLY public.certificacao_email_outbox
    ADD CONSTRAINT certificacao_email_outbox_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.certificacao_email_outbox
    ADD CONSTRAINT fk_certificacao_email_outbox_certificacao
        FOREIGN KEY (certificacao_id) REFERENCES public.recursos_certificacoes(id);

CREATE INDEX ix_certificacao_email_outbox_pendentes
    ON public.certificacao_email_outbox USING btree (enviado_em, proxima_tentativa_em);

-- RN.6: nenhuma carga inicial, de propósito. Quem está hoje com verificado = true não
-- ganha certificação automática — passar um booleano global para três pares exigiria
-- afirmar o que ninguém conferiu. Todo recurso envia o documento de cada par.
