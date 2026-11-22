ALTER TABLE public.ordens_servico
    ADD COLUMN entregue_em timestamp(6) with time zone,
    ADD COLUMN entregue_por character varying(150),
    ADD COLUMN motivo_revisao character varying(1000);

ALTER TABLE public.ordens_servico
    DROP CONSTRAINT ordens_servico_status_check,
    ADD CONSTRAINT ordens_servico_status_check CHECK (
        status IN (
            'PENDENTE_ALOCACAO',
            'EM_PLANEJAMENTO',
            'EM_EXECUCAO',
            'AGUARDANDO_VERIFICACAO',
            'ENCERRADA'
        )
    );

ALTER TABLE public.etapas_os_historico
    DROP CONSTRAINT ck_etapas_os_historico_evento,
    ADD CONSTRAINT ck_etapas_os_historico_evento CHECK (
        evento IN (
            'ALOCACAO',
            'REALOCACAO',
            'DISPARO',
            'ACEITE',
            'RECUSA',
            'ENTREGA',
            'AVANCO',
            'APROVACAO_FINAL',
            'REABERTURA'
        )
    );

CREATE TABLE public.entrega_final_email_outbox (
    id uuid NOT NULL,
    ordem_servico_id uuid NOT NULL,
    idempotency_key character varying(200) NOT NULL,
    aprovado_por character varying(150) NOT NULL,
    payload text,
    criado_em timestamp(6) with time zone NOT NULL,
    enviado_em timestamp(6) with time zone,
    proxima_tentativa_em timestamp(6) with time zone NOT NULL,
    tentativas integer NOT NULL
);

ALTER TABLE ONLY public.entrega_final_email_outbox
    ADD CONSTRAINT entrega_final_email_outbox_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.entrega_final_email_outbox
    ADD CONSTRAINT uk_entrega_final_outbox_idempotency UNIQUE (idempotency_key);

ALTER TABLE ONLY public.entrega_final_email_outbox
    ADD CONSTRAINT fk_entrega_final_outbox_ordem_servico
        FOREIGN KEY (ordem_servico_id) REFERENCES public.ordens_servico(id);

CREATE INDEX ix_entrega_final_outbox_pendentes
    ON public.entrega_final_email_outbox USING btree (enviado_em, proxima_tentativa_em);

ALTER TABLE public.entrega_final_email_outbox
    ADD COLUMN encerrado_em timestamp(6) with time zone;

CREATE INDEX ix_entrega_final_outbox_encerramento
    ON public.entrega_final_email_outbox USING btree (enviado_em, encerrado_em, proxima_tentativa_em);
