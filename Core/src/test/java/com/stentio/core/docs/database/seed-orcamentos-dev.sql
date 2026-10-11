-- Somente desenvolvimento: tokens fixos para a coleção Postman. Não executar em produção.
-- Reexecutar este script reinicia APENAS os cinco orçamentos demonstrativos.
BEGIN;
INSERT INTO clientes (id,nome_empresa,nome_representante,email_representante,telefone,cpf_cnpj,data_cadastro,data_modificacao)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e220301','Cliente de Testes Orçamento','Cliente Demonstração','orcamento@teste.invalid','11999999999','11222333000181',CURRENT_TIMESTAMP,CURRENT_TIMESTAMP)
ON CONFLICT (cpf_cnpj) DO NOTHING;
-- aprovar: 6bbd9908-4783-4991-a4f9-b0fc1807115a
DELETE FROM orcamento_decisao_outbox WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221301';
DELETE FROM orcamento_historico WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221301';
INSERT INTO orcamentos (id,versao,cliente_id,status,valor_total,data_expiracao,token_hash,token_utilizado_em)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e221301',0,(SELECT id FROM clientes WHERE cpf_cnpj='11222333000181'),'AGUARDANDO_APROVACAO',1500.00,CURRENT_TIMESTAMP + INTERVAL '7 days','f7c46bc13eb9b6960a8e24ad36fb5a360edbc976b5857f47f0945cab9a632fee',NULL)
ON CONFLICT (id) DO UPDATE SET status=EXCLUDED.status,valor_total=EXCLUDED.valor_total,data_expiracao=EXCLUDED.data_expiracao,token_hash=EXCLUDED.token_hash,token_utilizado_em=EXCLUDED.token_utilizado_em,versao=orcamentos.versao+1;
INSERT INTO orcamento_itens (id,orcamento_id,ordem,tipo_servico,quantidade,unidade,valor_unitario,subtotal)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e222301','bf049c86-7252-41bd-9ce3-761d6e221301',0,'Tradução Juramentada',5000,'Palavras',0.30,1500.00)
ON CONFLICT (id) DO NOTHING;
-- rejeitar: 6bbd9908-4783-4991-a4f9-b0fc1807115b
DELETE FROM orcamento_decisao_outbox WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221302';
DELETE FROM orcamento_historico WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221302';
INSERT INTO orcamentos (id,versao,cliente_id,status,valor_total,data_expiracao,token_hash,token_utilizado_em)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e221302',0,(SELECT id FROM clientes WHERE cpf_cnpj='11222333000181'),'AGUARDANDO_APROVACAO',1500.00,CURRENT_TIMESTAMP + INTERVAL '7 days','a7b9d5e85d07a82d9b1fdaa227b24cfb9581df7fcfdcab16c7336c42615a4b23',NULL)
ON CONFLICT (id) DO UPDATE SET status=EXCLUDED.status,valor_total=EXCLUDED.valor_total,data_expiracao=EXCLUDED.data_expiracao,token_hash=EXCLUDED.token_hash,token_utilizado_em=EXCLUDED.token_utilizado_em,versao=orcamentos.versao+1;
INSERT INTO orcamento_itens (id,orcamento_id,ordem,tipo_servico,quantidade,unidade,valor_unitario,subtotal)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e222302','bf049c86-7252-41bd-9ce3-761d6e221302',0,'Tradução Juramentada',5000,'Palavras',0.30,1500.00)
ON CONFLICT (id) DO NOTHING;
-- expirado: 6bbd9908-4783-4991-a4f9-b0fc1807115c
DELETE FROM orcamento_decisao_outbox WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221303';
DELETE FROM orcamento_historico WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221303';
INSERT INTO orcamentos (id,versao,cliente_id,status,valor_total,data_expiracao,token_hash,token_utilizado_em)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e221303',0,(SELECT id FROM clientes WHERE cpf_cnpj='11222333000181'),'AGUARDANDO_APROVACAO',1500.00,CURRENT_TIMESTAMP - INTERVAL '1 minute','5e1fe4649578b20bee23c023d8767fa993cae4c226649da8cbff77b9199da85e',NULL)
ON CONFLICT (id) DO UPDATE SET status=EXCLUDED.status,valor_total=EXCLUDED.valor_total,data_expiracao=EXCLUDED.data_expiracao,token_hash=EXCLUDED.token_hash,token_utilizado_em=EXCLUDED.token_utilizado_em,versao=orcamentos.versao+1;
INSERT INTO orcamento_itens (id,orcamento_id,ordem,tipo_servico,quantidade,unidade,valor_unitario,subtotal)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e222303','bf049c86-7252-41bd-9ce3-761d6e221303',0,'Tradução Juramentada',5000,'Palavras',0.30,1500.00)
ON CONFLICT (id) DO NOTHING;
-- utilizado: 6bbd9908-4783-4991-a4f9-b0fc1807115d
DELETE FROM orcamento_decisao_outbox WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221304';
DELETE FROM orcamento_historico WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221304';
INSERT INTO orcamentos (id,versao,cliente_id,status,valor_total,data_expiracao,token_hash,token_utilizado_em)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e221304',0,(SELECT id FROM clientes WHERE cpf_cnpj='11222333000181'),'APROVADO',1500.00,CURRENT_TIMESTAMP + INTERVAL '7 days','9d71bdb5962c88812865fb4293b2a2ddc41bdef7bb1efbde2bfd67e855e68114',CURRENT_TIMESTAMP - INTERVAL '1 minute')
ON CONFLICT (id) DO UPDATE SET status=EXCLUDED.status,valor_total=EXCLUDED.valor_total,data_expiracao=EXCLUDED.data_expiracao,token_hash=EXCLUDED.token_hash,token_utilizado_em=EXCLUDED.token_utilizado_em,versao=orcamentos.versao+1;
INSERT INTO orcamento_itens (id,orcamento_id,ordem,tipo_servico,quantidade,unidade,valor_unitario,subtotal)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e222304','bf049c86-7252-41bd-9ce3-761d6e221304',0,'Tradução Juramentada',5000,'Palavras',0.30,1500.00)
ON CONFLICT (id) DO NOTHING;
-- rascunho: 6bbd9908-4783-4991-a4f9-b0fc1807115e
DELETE FROM orcamento_decisao_outbox WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221305';
DELETE FROM orcamento_historico WHERE orcamento_id='bf049c86-7252-41bd-9ce3-761d6e221305';
INSERT INTO orcamentos (id,versao,cliente_id,status,valor_total,data_expiracao,token_hash,token_utilizado_em)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e221305',0,(SELECT id FROM clientes WHERE cpf_cnpj='11222333000181'),'RASCUNHO',1500.00,CURRENT_TIMESTAMP + INTERVAL '7 days','d4a797c9b2b75c5c0a6658b68f980246a5e8c4e018a12bcc88d790e41e376770',NULL)
ON CONFLICT (id) DO UPDATE SET status=EXCLUDED.status,valor_total=EXCLUDED.valor_total,data_expiracao=EXCLUDED.data_expiracao,token_hash=EXCLUDED.token_hash,token_utilizado_em=EXCLUDED.token_utilizado_em,versao=orcamentos.versao+1;
INSERT INTO orcamento_itens (id,orcamento_id,ordem,tipo_servico,quantidade,unidade,valor_unitario,subtotal)
VALUES ('bf049c86-7252-41bd-9ce3-761d6e222305','bf049c86-7252-41bd-9ce3-761d6e221305',0,'Tradução Juramentada',5000,'Palavras',0.30,1500.00)
ON CONFLICT (id) DO NOTHING;
COMMIT;
