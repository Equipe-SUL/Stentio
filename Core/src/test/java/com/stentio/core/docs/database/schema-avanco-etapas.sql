ALTER TABLE public.ordens_servico
    DROP CONSTRAINT ordens_servico_status_check,
    ADD CONSTRAINT ordens_servico_status_check CHECK (
        status IN (
            'PENDENTE_ALOCACAO',
            'EM_PLANEJAMENTO',
            'EM_EXECUCAO',
            'AGUARDANDO_VERIFICACAO'
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
            'AVANCO'
        )
    );
