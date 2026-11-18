# US-021 — Microserviço de Chatbot IA (LLM + RAG + Tool Calling)

## Resumo da Entrega
Implementação do microserviço backend de Inteligência Artificial (`chatbot-service`) em **Python (FastAPI)** integrado ao front-end (`stentio-web` via proxy reverso Nginx), aos bancos de dados **PostgreSQL** (consulta transacional em tempo real de Ordens de Serviço, Orçamentos, Clientes e Usuários) e **MongoDB** (persistência do histórico de conversas), utilizando **Groq API** com suporte a **Function Calling (Tools)** e **RAG (Retrieval-Augmented Generation)** sobre a documentação oficial do sistema (`manual_stentio.md`).

---

## Arquitetura e Principais Alterações

* **`chatbot/` (Novo Microserviço FastAPI):**
  * **`POST /api/v1/ia/chatbot/mensagens`:** Processa mensagens em linguagem natural, consulta dados no PostgreSQL via *Tool Calling*, aplica regras estritas de RBAC e retorna respostas acompanhadas de botões de atalho (`OS`, `ORCAMENTO`, `CLIENTE`).
  * **`GET /api/v1/ia/chatbot/historico`:** Recupera o histórico de mensagens e atalhos da sessão ativa do usuário logado diretamente do MongoDB (`stentio_chatbot.chatbot_mensagens`).
  * **RAG (`app/services/rag_service.py` + `docs/manual_stentio.md`):** Busca contextualizada na base de conhecimento oficial do Stentio para responder dúvidas operacionais (fluxos de telas, configuração SMTP, aprovação pública de orçamento, permissões e limites do escopo).
  * **Segurança Híbrida (`app/core/security.py`):** Validação de JWT compatível tanto com cookie HttpOnly (`stentio_token` utilizado pelo Front-end Web) quanto com header `Authorization: Bearer` (Swagger/Mobile).
* **Infraestrutura & Front-end:**
  * **`docker-compose.yml`:** Inclusão do container `stentio-chatbot-service` na rede `stentio-network` (porta `8000`) conectado ao PostgreSQL e ao MongoDB.
  * **`frontend/nginx.conf`:** Adicionado proxy reverso em `/api/v1/ia/chatbot` redirecionando para `http://chatbot-service:8000`.
  * **`frontend/src/features/chatbot/chatbotApi.ts`:** Ajuste de `baseURL` para roteamento na mesma origem via Nginx, garantindo o envio automático do cookie de sessão `stentio_token`.

---

## Pré-requisitos e Como Subir o Ambiente

### 1. Configurar variáveis de ambiente (`.env`)
Certifique-se de que o arquivo `.env` na raiz do projeto possui a chave da API do Groq e as origens liberadas no CORS:

```env
# Chave da IA (Groq)
GROQ_API_KEY=gsk_sua_chave_aqui

# Segredo JWT compartilhado entre Auth, Core e Chatbot
JWT_SECRET=minha-chave-secreta-super-longa-para-jwt-stentio-2026

# Banco de histórico do Chatbot no MongoDB
MONGO_DB_NAME=stentio_chatbot

# Origens permitidas no CORS
APP_CORS_ALLOWED_ORIGINS=http://localhost,http://localhost:8080,http://localhost:8081,http://localhost:8082,http://localhost:8000,http://localhost:19006
```

### 2. Subir os containers via Docker Compose
Na raiz do projeto, execute o comando abaixo para construir e iniciar os serviços atualizados:

```bash
docker compose up -d --build postgres mongodb auth core front chatbot-service
```

Para acompanhar os logs e verificar se o serviço de IA iniciou sem erros:

```bash
docker compose logs -f chatbot-service
```

* **Interface Web (Front-end + Nginx):** `http://localhost`
* **Swagger do Chatbot (Documentação Interativa da API):** `http://localhost:8000/docs`

---

## Roteiro de Testes para PO e Scrum Master (Critérios de Aceite & BDD)

### Pré-requisito
1. Acesse o sistema em `http://localhost` e faça login com um usuário **ADMIN** (ex: `admin@stentio.com`).
2. Caso o banco esteja vazio, cadastre ao menos **1 Cliente** (ex: `XPTO`), **1 Orçamento** e **1 Ordem de Serviço** (ou habilite `SEED_TEST_DATA_ENABLED=true` no `.env`).

---

### Teste 1: Acesso Global e Persistência de Histórico (CA.1 e CA.4)
1. Faça login na aplicação (`http://localhost`) e verifique se o botão flutuante do **Chatbot** está visível no canto inferior direito em diferentes páginas do sistema (**CA.1**).
2. Abra o chat, envie uma pergunta qualquer e aguarde a resposta da IA.
3. Feche o widget, navegue para outra página do sistema (ex: de *Clientes* para *Orçamentos* ou recarregue a página com `F5`) e abra o chat novamente.
* **Resultado esperado:** O histórico recente da conversa da sessão é recarregado automaticamente do MongoDB sem perda de contexto (**CA.4**).

---

### Teste 2: Consulta de Status no Banco + Botões de Atalho (CA.2, CA.3 e Cenário 1 do BDD)
1. Com o chatbot aberto (logado como `ADMIN` ou `GESTOR_PROJETO`), envie:
   * *"Qual o status da OS do cliente [Nome da Empresa Cadastrada]?"*
   * *"Tem algum orçamento para o cliente [Nome da Empresa]? Qual o valor e o status?"*
* **Resultado esperado:**
  * A IA consulta o PostgreSQL em tempo real e responde o status exato da OS / Orçamento (**CA.2**).
  * Abaixo da resposta da IA, são exibidos **botões de atalho clicáveis** (ex: `Abrir OS #...`, `Cliente: ...`, `Abrir Orçamento #...`) que redirecionam diretamente para o registro mencionado (**CA.3**).

---

### Teste 3: Dúvidas Operacionais sobre o Sistema via Manual / RAG (CA.2)
1. No Chatbot, faça perguntas sobre regras de negócio e operação do Stentio:
   * *"Como eu configuro o servidor SMTP usando uma conta do Gmail?"*
   * *"O envio de e-mail deu o erro '535 Username and Password not accepted'. O que eu faço para corrigir?"*
   * *"O cliente precisa criar login e senha no sistema para aprovar ou recusar um orçamento?"*
   * *"O que acontece automaticamente no sistema depois que o cliente aprova um orçamento?"*
   * *"Quais perfis têm permissão para excluir um cliente?"*
   * *"O sistema Stentio emite nota fiscal ou fatura de venda?"*
* **Resultado esperado:** A IA responde com base na documentação oficial (`manual_stentio.md`), explicando o passo a passo e informando corretamente o que o sistema faz ou não faz (sem inventar funcionalidades não implementadas).

---

### Teste 4: Memória de Contexto da Conversa (CA.4)
1. Envie primeiro: *"Qual o status da OS do cliente [Nome da Empresa]?"*
2. Logo em seguida, sem repetir o nome do cliente, envie: *"E qual é o valor do orçamento dele?"*
* **Resultado esperado:** A IA compreende pelo histórico salvo no MongoDB a qual cliente o pronome *"dele"* se refere e realiza a busca corretamente.

---

### Teste 5: Bloqueio de Acesso por Perfil / RBAC (RN.1, RN.2 e Cenário 2 do BDD)
1. **Pela Interface Web:** Acesse o sistema com um usuário de perfil restrito (`TRADUTOR` / `RECURSO`) e pergunte no chat:
   * *"Qual foi o faturamento total deste mês?"*
   * *"Qual o valor do orçamento do cliente [Nome da Empresa]?"*
2. **Alternativa rápida via Swagger (`http://localhost:8000/docs`):**
   * Clique no botão **Authorize** no topo direito da página, digite `TRADUTOR` no campo *Value* e confirme.
   * No endpoint `POST /api/v1/ia/chatbot/mensagens`, envie `{"mensagem": "Qual foi o faturamento total deste mês?"}`.
   * Para comparar, clique em **Authorize**, troque o valor para `ADMIN` e envie a mesma pergunta.
* **Resultado esperado:**
  * Para `TRADUTOR`: A IA recusa educadamente informando que o perfil não possui permissão para visualizar dados financeiros consolidados ou de clientes, sem expor problemas técnicos (**RN.1 e RN.2**).
  * Para `ADMIN`: A IA soma os valores no banco de dados e informa o faturamento total consolidado.