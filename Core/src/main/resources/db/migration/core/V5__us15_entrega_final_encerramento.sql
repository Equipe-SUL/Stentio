ALTER TABLE public.entrega_final_email_outbox
    ADD COLUMN encerrado_em timestamp(6) with time zone;

UPDATE public.entrega_final_email_outbox o
   SET encerrado_em = o.enviado_em
  FROM public.ordens_servico os
 WHERE o.ordem_servico_id = os.id
   AND o.enviado_em IS NOT NULL
   AND os.status = 'ENCERRADA';

CREATE INDEX ix_entrega_final_outbox_encerramento
    ON public.entrega_final_email_outbox USING btree (enviado_em, encerrado_em, proxima_tentativa_em);
