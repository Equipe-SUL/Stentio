-- Initial Core schema baseline, including the US-013 lifecycle constraints.


-- Dumped from database version 18.6 (Debian 18.6-1.pgdg13+2)
-- Dumped by pg_dump version 18.6 (Debian 18.6-1.pgdg13+2)




--
-- Name: arquivos_solicitacao; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.arquivos_solicitacao (
    id uuid NOT NULL,
    caminho character varying(500) NOT NULL,
    enviado_em timestamp(6) without time zone NOT NULL,
    nome_original character varying(255) NOT NULL,
    tamanho_bytes bigint NOT NULL,
    tipo_conteudo character varying(120),
    solicitacao_id uuid NOT NULL
);


--
-- Name: categorias_projeto; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.categorias_projeto (
    id uuid NOT NULL,
    nome character varying(255) NOT NULL
);


--
-- Name: clientes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.clientes (
    id uuid NOT NULL,
    cpf_cnpj character varying(14) NOT NULL,
    data_cadastro timestamp(6) without time zone NOT NULL,
    data_modificacao timestamp(6) without time zone NOT NULL,
    email_representante character varying(150) NOT NULL,
    nome_empresa character varying(150) NOT NULL,
    nome_representante character varying(100) NOT NULL,
    telefone character varying(20)
);


--
-- Name: etapas_os; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.etapas_os (
    id uuid NOT NULL,
    alocado_em timestamp(6) with time zone,
    criado_em timestamp(6) with time zone DEFAULT now() NOT NULL,
    data_limite_entrega timestamp(6) with time zone,
    nome character varying(120) NOT NULL,
    ordem integer NOT NULL,
    prazo_estimado_horas integer NOT NULL,
    status character varying(30) NOT NULL,
    os_id uuid NOT NULL,
    recurso_id uuid,
    tipo_servico_id uuid NOT NULL,
    aceito_em timestamp(6) with time zone,
    arquivo_entregue_caminho character varying(500),
    arquivo_entregue_nome character varying(255),
    disparada_em timestamp(6) with time zone,
    entregue_em timestamp(6) with time zone,
    CONSTRAINT ck_etapas_os_status CHECK (((status)::text = ANY ((ARRAY['PENDENTE'::character varying, 'AGUARDANDO_ETAPA_ANTERIOR'::character varying, 'ALOCADO'::character varying, 'DISPARADA'::character varying, 'ACEITA'::character varying, 'ENTREGUE'::character varying])::text[])))
);


--
-- Name: etapas_os_historico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.etapas_os_historico (
    id uuid NOT NULL,
    data_limite_entrega timestamp(6) with time zone,
    evento character varying(30) NOT NULL,
    realizado_em timestamp(6) with time zone NOT NULL,
    realizado_por character varying(150) NOT NULL,
    etapa_id uuid NOT NULL,
    recurso_anterior_id uuid,
    recurso_novo_id uuid,
    CONSTRAINT ck_etapas_os_historico_evento CHECK (((evento)::text = ANY ((ARRAY['ALOCACAO'::character varying, 'REALOCACAO'::character varying, 'DISPARO'::character varying, 'ACEITE'::character varying, 'RECUSA'::character varying, 'ENTREGA'::character varying])::text[])))
);


--
-- Name: idiomas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.idiomas (
    id uuid NOT NULL,
    ativo boolean NOT NULL,
    codigo_iso character varying(10) NOT NULL,
    nome character varying(255) NOT NULL
);


--
-- Name: log_auditoria; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.log_auditoria (
    id uuid NOT NULL,
    data_hora timestamp(6) with time zone NOT NULL,
    entidade character varying(30) NOT NULL,
    entidade_id uuid NOT NULL,
    observacao character varying(2500),
    origem_acao character varying(30) NOT NULL,
    referencia_legivel character varying(200) NOT NULL,
    status_anterior character varying(40),
    status_novo character varying(40) NOT NULL,
    usuario_id uuid,
    usuario_nome character varying(150) NOT NULL,
    usuario_perfil character varying(40) NOT NULL
);


--
-- Name: modelos_workflow; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.modelos_workflow (
    id uuid NOT NULL,
    ativo boolean NOT NULL,
    descricao character varying(500),
    nome character varying(120) NOT NULL
);


--
-- Name: modelos_workflow_etapas; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.modelos_workflow_etapas (
    id uuid NOT NULL,
    nome character varying(120) NOT NULL,
    ordem integer NOT NULL,
    prazo_estimado_horas integer NOT NULL,
    modelo_id uuid NOT NULL,
    tipo_servico_id uuid NOT NULL
);


--
-- Name: notificacao_etapa_outbox; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.notificacao_etapa_outbox (
    id uuid NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    destinatario character varying(150) NOT NULL,
    enviado_em timestamp(6) with time zone,
    payload text,
    proxima_tentativa_em timestamp(6) with time zone NOT NULL,
    tentativas integer NOT NULL,
    etapa_id uuid NOT NULL
);


--
-- Name: orcamento_decisao_outbox; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamento_decisao_outbox (
    id uuid NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    orcamento_id uuid NOT NULL,
    payload text NOT NULL,
    proxima_tentativa_em timestamp(6) with time zone NOT NULL,
    publicado_em timestamp(6) with time zone,
    routing_key character varying(100) NOT NULL,
    tentativas integer NOT NULL
);


--
-- Name: orcamento_email_outbox; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamento_email_outbox (
    id uuid NOT NULL,
    criado_em timestamp(6) with time zone NOT NULL,
    enviado_em timestamp(6) with time zone,
    payload text,
    proxima_tentativa_em timestamp(6) with time zone NOT NULL,
    tentativas integer NOT NULL,
    orcamento_id uuid NOT NULL
);


--
-- Name: orcamento_historico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamento_historico (
    id uuid NOT NULL,
    data_decisao timestamp(6) with time zone NOT NULL,
    motivo character varying(2000),
    origem character varying(30) NOT NULL,
    status_anterior character varying(30) NOT NULL,
    status_novo character varying(30) NOT NULL,
    orcamento_id uuid NOT NULL,
    CONSTRAINT orcamento_historico_status_anterior_check CHECK (((status_anterior)::text = ANY ((ARRAY['EM_ELABORACAO'::character varying, 'AGUARDANDO_APROVACAO'::character varying, 'APROVADO'::character varying, 'REJEITADO'::character varying])::text[]))),
    CONSTRAINT orcamento_historico_status_novo_check CHECK (((status_novo)::text = ANY ((ARRAY['EM_ELABORACAO'::character varying, 'AGUARDANDO_APROVACAO'::character varying, 'APROVADO'::character varying, 'REJEITADO'::character varying])::text[])))
);


--
-- Name: orcamento_item; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamento_item (
    id uuid NOT NULL,
    ordem integer NOT NULL,
    subtotal_item numeric(14,2) NOT NULL,
    idioma_destino_id uuid NOT NULL,
    idioma_origem_id uuid NOT NULL,
    orcamento_id uuid NOT NULL,
    CONSTRAINT ck_orcamento_item_idiomas_diferentes CHECK ((idioma_origem_id <> idioma_destino_id))
);


--
-- Name: orcamento_item_servico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamento_item_servico (
    id uuid NOT NULL,
    quantidade numeric(14,2) NOT NULL,
    subtotal numeric(14,2) NOT NULL,
    tipo_servico character varying(150) NOT NULL,
    unidade character varying(20) NOT NULL,
    valor_unitario numeric(14,4) NOT NULL,
    orcamento_item_id uuid NOT NULL,
    CONSTRAINT ck_orcamento_item_servico_valores CHECK (((quantidade > (0)::numeric) AND (valor_unitario > (0)::numeric))),
    CONSTRAINT orcamento_item_servico_unidade_check CHECK (((unidade)::text = ANY ((ARRAY['PALAVRA'::character varying, 'HORA'::character varying, 'PAGINA'::character varying])::text[])))
);


--
-- Name: orcamentos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.orcamentos (
    id uuid NOT NULL,
    atualizado_em timestamp(6) with time zone DEFAULT now() NOT NULL,
    criado_em timestamp(6) with time zone DEFAULT now() NOT NULL,
    data_expiracao timestamp(6) with time zone,
    numero_versao integer DEFAULT 1 NOT NULL,
    status character varying(30) NOT NULL,
    token_hash character varying(64),
    token_utilizado_em timestamp(6) with time zone,
    valor_total numeric(19,2) NOT NULL,
    versao bigint NOT NULL,
    cliente_id uuid NOT NULL,
    solicitacao_id uuid,
    CONSTRAINT orcamentos_status_check CHECK (((status)::text = ANY ((ARRAY['EM_ELABORACAO'::character varying, 'AGUARDANDO_APROVACAO'::character varying, 'APROVADO'::character varying, 'REJEITADO'::character varying])::text[])))
);


--
-- Name: ordens_servico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.ordens_servico (
    id uuid NOT NULL,
    criado_em timestamp(6) with time zone DEFAULT now() NOT NULL,
    status character varying(30) NOT NULL,
    orcamento_id uuid NOT NULL,
    solicitacao_id uuid NOT NULL,
    CONSTRAINT ordens_servico_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE_ALOCACAO'::character varying, 'EM_PLANEJAMENTO'::character varying])::text[])))
);


--
-- Name: recursos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recursos (
    id uuid NOT NULL,
    ativo boolean NOT NULL,
    email character varying(150) NOT NULL,
    nome character varying(100) NOT NULL,
    telefone character varying(20),
    usuario_id uuid,
    verificado boolean NOT NULL
);


--
-- Name: recursos_precos; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recursos_precos (
    id uuid NOT NULL,
    unidade character varying(20) NOT NULL,
    valor numeric(12,4) NOT NULL,
    idioma_destino_id uuid NOT NULL,
    idioma_origem_id uuid NOT NULL,
    recurso_id uuid NOT NULL,
    CONSTRAINT recursos_precos_unidade_check CHECK (((unidade)::text = ANY ((ARRAY['PALAVRA'::character varying, 'HORA'::character varying, 'PAGINA'::character varying])::text[])))
);


--
-- Name: recursos_tipos_servico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.recursos_tipos_servico (
    recurso_id uuid NOT NULL,
    tipo_servico_id uuid NOT NULL
);


--
-- Name: solicitacoes; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.solicitacoes (
    id uuid NOT NULL,
    data_recebimento timestamp(6) without time zone NOT NULL,
    descricao text,
    status character varying(30) NOT NULL,
    categoria_projeto_id uuid NOT NULL,
    cliente_id uuid NOT NULL,
    idioma_destino_id uuid NOT NULL,
    idioma_origem_id uuid NOT NULL,
    CONSTRAINT solicitacoes_status_check CHECK (((status)::text = ANY ((ARRAY['PENDENTE_ORCAMENTO'::character varying, 'AGUARDANDO_APROVACAO'::character varying, 'ORCAMENTO_REJEITADO'::character varying, 'EM_EXECUCAO'::character varying])::text[])))
);


--
-- Name: tabelas_preco; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tabelas_preco (
    id uuid NOT NULL,
    unidade character varying(20) NOT NULL,
    valor numeric(12,4) NOT NULL,
    idioma_destino_id uuid NOT NULL,
    idioma_origem_id uuid NOT NULL,
    tipo_servico_id uuid NOT NULL,
    CONSTRAINT tabelas_preco_unidade_check CHECK (((unidade)::text = ANY ((ARRAY['PALAVRA'::character varying, 'HORA'::character varying, 'PAGINA'::character varying])::text[])))
);


--
-- Name: tipos_servico; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.tipos_servico (
    id uuid NOT NULL,
    ativo boolean NOT NULL,
    descricao character varying(500),
    nome character varying(255) NOT NULL
);


--
-- Name: arquivos_solicitacao arquivos_solicitacao_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arquivos_solicitacao
    ADD CONSTRAINT arquivos_solicitacao_pkey PRIMARY KEY (id);


--
-- Name: categorias_projeto categorias_projeto_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.categorias_projeto
    ADD CONSTRAINT categorias_projeto_pkey PRIMARY KEY (id);


--
-- Name: clientes clientes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clientes
    ADD CONSTRAINT clientes_pkey PRIMARY KEY (id);


--
-- Name: etapas_os_historico etapas_os_historico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os_historico
    ADD CONSTRAINT etapas_os_historico_pkey PRIMARY KEY (id);


--
-- Name: etapas_os etapas_os_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os
    ADD CONSTRAINT etapas_os_pkey PRIMARY KEY (id);


--
-- Name: idiomas idiomas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.idiomas
    ADD CONSTRAINT idiomas_pkey PRIMARY KEY (id);


--
-- Name: log_auditoria log_auditoria_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.log_auditoria
    ADD CONSTRAINT log_auditoria_pkey PRIMARY KEY (id);


--
-- Name: modelos_workflow_etapas modelos_workflow_etapas_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modelos_workflow_etapas
    ADD CONSTRAINT modelos_workflow_etapas_pkey PRIMARY KEY (id);


--
-- Name: modelos_workflow modelos_workflow_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modelos_workflow
    ADD CONSTRAINT modelos_workflow_pkey PRIMARY KEY (id);


--
-- Name: notificacao_etapa_outbox notificacao_etapa_outbox_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacao_etapa_outbox
    ADD CONSTRAINT notificacao_etapa_outbox_pkey PRIMARY KEY (id);


--
-- Name: orcamento_decisao_outbox orcamento_decisao_outbox_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_decisao_outbox
    ADD CONSTRAINT orcamento_decisao_outbox_pkey PRIMARY KEY (id);


--
-- Name: orcamento_email_outbox orcamento_email_outbox_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_email_outbox
    ADD CONSTRAINT orcamento_email_outbox_pkey PRIMARY KEY (id);


--
-- Name: orcamento_historico orcamento_historico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_historico
    ADD CONSTRAINT orcamento_historico_pkey PRIMARY KEY (id);


--
-- Name: orcamento_item orcamento_item_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_item
    ADD CONSTRAINT orcamento_item_pkey PRIMARY KEY (id);


--
-- Name: orcamento_item_servico orcamento_item_servico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_item_servico
    ADD CONSTRAINT orcamento_item_servico_pkey PRIMARY KEY (id);


--
-- Name: orcamentos orcamentos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT orcamentos_pkey PRIMARY KEY (id);


--
-- Name: ordens_servico ordens_servico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT ordens_servico_pkey PRIMARY KEY (id);


--
-- Name: recursos recursos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos
    ADD CONSTRAINT recursos_pkey PRIMARY KEY (id);


--
-- Name: recursos_precos recursos_precos_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos_precos
    ADD CONSTRAINT recursos_precos_pkey PRIMARY KEY (id);


--
-- Name: recursos_tipos_servico recursos_tipos_servico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos_tipos_servico
    ADD CONSTRAINT recursos_tipos_servico_pkey PRIMARY KEY (recurso_id, tipo_servico_id);


--
-- Name: solicitacoes solicitacoes_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitacoes
    ADD CONSTRAINT solicitacoes_pkey PRIMARY KEY (id);


--
-- Name: tabelas_preco tabelas_preco_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tabelas_preco
    ADD CONSTRAINT tabelas_preco_pkey PRIMARY KEY (id);


--
-- Name: tipos_servico tipos_servico_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tipos_servico
    ADD CONSTRAINT tipos_servico_pkey PRIMARY KEY (id);


--
-- Name: clientes uk_clientes_cpf_cnpj; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.clientes
    ADD CONSTRAINT uk_clientes_cpf_cnpj UNIQUE (cpf_cnpj);


--
-- Name: etapas_os uk_etapas_os_ordem; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os
    ADD CONSTRAINT uk_etapas_os_ordem UNIQUE (os_id, ordem);


--
-- Name: modelos_workflow_etapas uk_modelos_workflow_etapas_ordem; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modelos_workflow_etapas
    ADD CONSTRAINT uk_modelos_workflow_etapas_ordem UNIQUE (modelo_id, ordem);


--
-- Name: modelos_workflow uk_modelos_workflow_nome; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modelos_workflow
    ADD CONSTRAINT uk_modelos_workflow_nome UNIQUE (nome);


--
-- Name: orcamento_item uk_orcamento_item_ordem; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_item
    ADD CONSTRAINT uk_orcamento_item_ordem UNIQUE (orcamento_id, ordem);


--
-- Name: orcamentos uk_orcamentos_token_hash; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT uk_orcamentos_token_hash UNIQUE (token_hash);


--
-- Name: ordens_servico uk_ordens_servico_orcamento; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT uk_ordens_servico_orcamento UNIQUE (orcamento_id);


--
-- Name: recursos uk_recursos_email; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos
    ADD CONSTRAINT uk_recursos_email UNIQUE (email);


--
-- Name: recursos_precos uk_recursos_precos_par_unidade; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos_precos
    ADD CONSTRAINT uk_recursos_precos_par_unidade UNIQUE (recurso_id, idioma_origem_id, idioma_destino_id, unidade);


--
-- Name: recursos uk_recursos_usuario; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos
    ADD CONSTRAINT uk_recursos_usuario UNIQUE (usuario_id);


--
-- Name: tabelas_preco uk_tabelas_preco_combinacao; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tabelas_preco
    ADD CONSTRAINT uk_tabelas_preco_combinacao UNIQUE (tipo_servico_id, idioma_origem_id, idioma_destino_id, unidade);


--
-- Name: idx_clientes_cpf_cnpj; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_clientes_cpf_cnpj ON public.clientes USING btree (cpf_cnpj);


--
-- Name: idx_orcamento_email_outbox_pendente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamento_email_outbox_pendente ON public.orcamento_email_outbox USING btree (enviado_em, proxima_tentativa_em, criado_em);


--
-- Name: idx_orcamento_historico_orcamento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamento_historico_orcamento ON public.orcamento_historico USING btree (orcamento_id);


--
-- Name: idx_orcamento_item_orcamento; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamento_item_orcamento ON public.orcamento_item USING btree (orcamento_id);


--
-- Name: idx_orcamento_item_servico_item; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamento_item_servico_item ON public.orcamento_item_servico USING btree (orcamento_item_id);


--
-- Name: idx_orcamento_outbox_pendente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamento_outbox_pendente ON public.orcamento_decisao_outbox USING btree (publicado_em, proxima_tentativa_em, criado_em);


--
-- Name: idx_orcamentos_cliente; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamentos_cliente ON public.orcamentos USING btree (cliente_id);


--
-- Name: idx_orcamentos_solicitacao; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamentos_solicitacao ON public.orcamentos USING btree (solicitacao_id);


--
-- Name: idx_orcamentos_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_orcamentos_status ON public.orcamentos USING btree (status);


--
-- Name: ix_etapas_os_historico_etapa; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_etapas_os_historico_etapa ON public.etapas_os_historico USING btree (etapa_id);


--
-- Name: ix_log_auditoria_data_hora; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_log_auditoria_data_hora ON public.log_auditoria USING btree (data_hora);


--
-- Name: ix_log_auditoria_entidade; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_log_auditoria_entidade ON public.log_auditoria USING btree (entidade, entidade_id);


--
-- Name: ix_log_auditoria_usuario; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_log_auditoria_usuario ON public.log_auditoria USING btree (usuario_id);


--
-- Name: ix_notificacao_etapa_outbox_pendentes; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_notificacao_etapa_outbox_pendentes ON public.notificacao_etapa_outbox USING btree (enviado_em, proxima_tentativa_em);


--
-- Name: recursos_tipos_servico fk2sr8q6po45p9pg5al26lm4oth; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos_tipos_servico
    ADD CONSTRAINT fk2sr8q6po45p9pg5al26lm4oth FOREIGN KEY (recurso_id) REFERENCES public.recursos(id);


--
-- Name: etapas_os_historico fk2xk1xmv9yqtm780lchd4injoy; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os_historico
    ADD CONSTRAINT fk2xk1xmv9yqtm780lchd4injoy FOREIGN KEY (recurso_novo_id) REFERENCES public.recursos(id);


--
-- Name: modelos_workflow_etapas fk308fp8x43yu6kr6yjcetm47sw; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modelos_workflow_etapas
    ADD CONSTRAINT fk308fp8x43yu6kr6yjcetm47sw FOREIGN KEY (tipo_servico_id) REFERENCES public.tipos_servico(id);


--
-- Name: solicitacoes fk47ebdqhwraavy9xog7vgx0l0n; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitacoes
    ADD CONSTRAINT fk47ebdqhwraavy9xog7vgx0l0n FOREIGN KEY (categoria_projeto_id) REFERENCES public.categorias_projeto(id);


--
-- Name: orcamento_historico fk6kg7w241d8gw6555ddiefdxbr; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_historico
    ADD CONSTRAINT fk6kg7w241d8gw6555ddiefdxbr FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id);


--
-- Name: orcamento_item fk7eip76y3m3rad54jbbqldhqyi; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_item
    ADD CONSTRAINT fk7eip76y3m3rad54jbbqldhqyi FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id) ON DELETE CASCADE;


--
-- Name: orcamentos fk7schuarwjtqecapo8hbdrat61; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT fk7schuarwjtqecapo8hbdrat61 FOREIGN KEY (solicitacao_id) REFERENCES public.solicitacoes(id);


--
-- Name: arquivos_solicitacao fk80gransvds1im2tac0bijvow1; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.arquivos_solicitacao
    ADD CONSTRAINT fk80gransvds1im2tac0bijvow1 FOREIGN KEY (solicitacao_id) REFERENCES public.solicitacoes(id);


--
-- Name: orcamento_email_outbox fk_orcamento_email_outbox_orcamento; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_email_outbox
    ADD CONSTRAINT fk_orcamento_email_outbox_orcamento FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id);


--
-- Name: etapas_os fkad8b68bg978byl8a73fs36n9p; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os
    ADD CONSTRAINT fkad8b68bg978byl8a73fs36n9p FOREIGN KEY (tipo_servico_id) REFERENCES public.tipos_servico(id);


--
-- Name: etapas_os_historico fkalygbyl273p946h4h17r3gitl; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os_historico
    ADD CONSTRAINT fkalygbyl273p946h4h17r3gitl FOREIGN KEY (recurso_anterior_id) REFERENCES public.recursos(id);


--
-- Name: tabelas_preco fkbma8nw0m6a05muuw7w49cw198; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tabelas_preco
    ADD CONSTRAINT fkbma8nw0m6a05muuw7w49cw198 FOREIGN KEY (tipo_servico_id) REFERENCES public.tipos_servico(id);


--
-- Name: orcamentos fkcsjpjxc7xgdqqg51fe8gl1lh2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamentos
    ADD CONSTRAINT fkcsjpjxc7xgdqqg51fe8gl1lh2 FOREIGN KEY (cliente_id) REFERENCES public.clientes(id);


--
-- Name: notificacao_etapa_outbox fkd1txx52o4j1k5dxuw4a7j4h77; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.notificacao_etapa_outbox
    ADD CONSTRAINT fkd1txx52o4j1k5dxuw4a7j4h77 FOREIGN KEY (etapa_id) REFERENCES public.etapas_os(id);


--
-- Name: recursos_precos fkdqtrye8ptajaj9pnq981slq5c; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos_precos
    ADD CONSTRAINT fkdqtrye8ptajaj9pnq981slq5c FOREIGN KEY (idioma_destino_id) REFERENCES public.idiomas(id);


--
-- Name: recursos_precos fke3efmktvgh3rd1ua8skv615wu; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos_precos
    ADD CONSTRAINT fke3efmktvgh3rd1ua8skv615wu FOREIGN KEY (recurso_id) REFERENCES public.recursos(id);


--
-- Name: tabelas_preco fkffjtxu6dc7pmi1srvvtn475au; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tabelas_preco
    ADD CONSTRAINT fkffjtxu6dc7pmi1srvvtn475au FOREIGN KEY (idioma_origem_id) REFERENCES public.idiomas(id);


--
-- Name: modelos_workflow_etapas fkfv1ixli6643ij38v33jhxoy2e; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.modelos_workflow_etapas
    ADD CONSTRAINT fkfv1ixli6643ij38v33jhxoy2e FOREIGN KEY (modelo_id) REFERENCES public.modelos_workflow(id);


--
-- Name: etapas_os_historico fkkgwxlikmkp400jw2aekmj2vgj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os_historico
    ADD CONSTRAINT fkkgwxlikmkp400jw2aekmj2vgj FOREIGN KEY (etapa_id) REFERENCES public.etapas_os(id);


--
-- Name: recursos_tipos_servico fkkj4at9g4icc06wyc2h4d177yt; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos_tipos_servico
    ADD CONSTRAINT fkkj4at9g4icc06wyc2h4d177yt FOREIGN KEY (tipo_servico_id) REFERENCES public.tipos_servico(id);


--
-- Name: recursos_precos fkko34a29wjblhmsm8hvti49tb2; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.recursos_precos
    ADD CONSTRAINT fkko34a29wjblhmsm8hvti49tb2 FOREIGN KEY (idioma_origem_id) REFERENCES public.idiomas(id);


--
-- Name: orcamento_item fklfpe4cwe8may3wll1ncjdybjj; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_item
    ADD CONSTRAINT fklfpe4cwe8may3wll1ncjdybjj FOREIGN KEY (idioma_origem_id) REFERENCES public.idiomas(id);


--
-- Name: solicitacoes fkmawv6ytamjxq89asuqxdf2eqe; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitacoes
    ADD CONSTRAINT fkmawv6ytamjxq89asuqxdf2eqe FOREIGN KEY (idioma_origem_id) REFERENCES public.idiomas(id);


--
-- Name: orcamento_item_servico fkmui3vyp9a1dvyqg7chlsofc2m; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_item_servico
    ADD CONSTRAINT fkmui3vyp9a1dvyqg7chlsofc2m FOREIGN KEY (orcamento_item_id) REFERENCES public.orcamento_item(id) ON DELETE CASCADE;


--
-- Name: solicitacoes fkn2n6k6893i7eh6x73kt4g5jxo; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitacoes
    ADD CONSTRAINT fkn2n6k6893i7eh6x73kt4g5jxo FOREIGN KEY (idioma_destino_id) REFERENCES public.idiomas(id);


--
-- Name: etapas_os fknd60lpql8lqwf17ylqgti615s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os
    ADD CONSTRAINT fknd60lpql8lqwf17ylqgti615s FOREIGN KEY (os_id) REFERENCES public.ordens_servico(id);


--
-- Name: etapas_os fkoomwcinkhex46j01rr665k20s; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.etapas_os
    ADD CONSTRAINT fkoomwcinkhex46j01rr665k20s FOREIGN KEY (recurso_id) REFERENCES public.recursos(id);


--
-- Name: tabelas_preco fkr0w5bivalmfayh9c7smj4giai; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.tabelas_preco
    ADD CONSTRAINT fkr0w5bivalmfayh9c7smj4giai FOREIGN KEY (idioma_destino_id) REFERENCES public.idiomas(id);


--
-- Name: ordens_servico fkra5ee5bqajsluhxra5g90hhne; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT fkra5ee5bqajsluhxra5g90hhne FOREIGN KEY (orcamento_id) REFERENCES public.orcamentos(id);


--
-- Name: ordens_servico fkrppigt858d7ge3tda7tw75wxx; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.ordens_servico
    ADD CONSTRAINT fkrppigt858d7ge3tda7tw75wxx FOREIGN KEY (solicitacao_id) REFERENCES public.solicitacoes(id);


--
-- Name: orcamento_item fktd5naey2jen3dfl5ewm5lanku; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.orcamento_item
    ADD CONSTRAINT fktd5naey2jen3dfl5ewm5lanku FOREIGN KEY (idioma_destino_id) REFERENCES public.idiomas(id);


--
-- Name: solicitacoes fktnj4xn68h9tl6mtmtewxpix19; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.solicitacoes
    ADD CONSTRAINT fktnj4xn68h9tl6mtmtewxpix19 FOREIGN KEY (cliente_id) REFERENCES public.clientes(id);


--
-- PostgreSQL database dump complete
--

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM pg_roles WHERE rolname = 'agent') THEN
        EXECUTE 'GRANT USAGE ON SCHEMA public TO agent';
        EXECUTE 'GRANT SELECT ON ALL TABLES IN SCHEMA public TO agent';
        EXECUTE 'ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO agent';
    END IF;
END
$$;
