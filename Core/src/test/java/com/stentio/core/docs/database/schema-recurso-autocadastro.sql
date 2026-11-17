-- US-023: o recurso ganha conta de acesso e verificação.
-- As colunas sobem com ddl-auto: update. O UPDATE do final NÃO sobe, e sem ele a
-- alocação da US-012 para de funcionar para todo mundo que já está cadastrado.

-- ---------------------------------------------------------------- colunas novas

ALTER TABLE recursos ADD COLUMN IF NOT EXISTS usuario_id UUID;
ALTER TABLE recursos ADD COLUMN IF NOT EXISTS verificado BOOLEAN NOT NULL DEFAULT FALSE;

-- Uma conta de acesso corresponde a no máximo um perfil profissional.
-- Nulo não conflita em UNIQUE no Postgres, então os recursos sem conta convivem.
CREATE UNIQUE INDEX IF NOT EXISTS uk_recursos_usuario ON recursos (usuario_id);

-- usuario_id não é chave estrangeira de propósito: o usuário vive no banco do Auth, e a
-- integridade entre os dois serviços é o JWT, não o Postgres.

-- ---------------------------------------------------------------- OBRIGATÓRIO em banco existente

-- A coluna nasce FALSE, e a RN.5 diz que recurso não verificado não é alocável. Sem este
-- UPDATE, todo profissional já cadastrado fica inalocável no instante do deploy e a
-- US-012 passa a responder 400 sem ninguém entender por quê.
--
-- Pela RN.6, quem a equipe cadastrou pela US-004 já passou pelo crivo de uma pessoa da
-- Aliança: é exatamente o que a verificação atesta. Então os existentes entram
-- verificados, e a verificação pendente passa a valer só para quem chega sozinho.

UPDATE recursos SET verificado = TRUE WHERE usuario_id IS NULL;

-- Conferência — deve voltar zero:
--   SELECT count(*) FROM recursos WHERE verificado = FALSE AND usuario_id IS NULL;
