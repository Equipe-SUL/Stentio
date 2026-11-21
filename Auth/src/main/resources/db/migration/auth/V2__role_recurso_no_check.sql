-- Bancos criados pelo Hibernate antes do perfil RECURSO ficaram com um CHECK que só
-- aceita ADMIN, ATENDENTE, GESTOR_PROJETO e FINANCEIRO. Com o baseline 1 o Flyway pula
-- a V1, então esse CHECK antigo sobrevive e todo auto-cadastro de recurso é recusado
-- pelo banco (o Auth devolve 409 e a tela mostra "e-mail já cadastrado").
-- Recria o CHECK com todos os perfis do enum Role. Em banco novo não muda nada.
ALTER TABLE usuarios DROP CONSTRAINT IF EXISTS usuarios_role_check;
ALTER TABLE usuarios ADD CONSTRAINT usuarios_role_check
    CHECK (role IN ('ADMIN', 'ATENDENTE', 'GESTOR_PROJETO', 'FINANCEIRO', 'RECURSO'));
