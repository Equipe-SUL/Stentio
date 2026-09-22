# Configuração de E-mail (SMTP) — Manual do Usuário

Este manual explica como configurar o envio de e-mails do Stentio (servidor SMTP) pela tela **SMTP** e como validar o funcionamento com um e-mail de teste.

## 1. Visão geral

- A tela **SMTP** permite definir qual servidor de e-mail a plataforma usa para enviar notificações e e-mails.
- A configuração é **salva** no email-service, persistida no MongoDB e usada em todo envio.
- É possível **disparar um e-mail de teste** para um destinatário e ver o resultado na própria tela.
- O envio do teste **não altera** a configuração salva: qualquer alteração precisa ser confirmada com o botão **Salvar Configurações**.

## 2. Pré-requisitos

1. **email-service** em execução.
2. **frontend (web ou aplicativo)** em execução.
3. Para usar e-mail real (ex.: Gmail), tenha um **e-mail de aplicativo (App Password)** — ver seção 4.

## 3. Acessando a tela

1. Faça login no sistema (perfil admin).
2. Abra a rota `/smtp` no navegador ou navegue até configurações -> Serviço de Email.

## 4. Campos de configuração

| Campo | Descrição | Exemplo |
|-------|-----------|---------|
| **Host SMTP** | Endereço do servidor de e-mail | `smtp.gmail.com` |
| **Porta SMTP** | Porta de conexão | `587` (TLS), `465` (SSL), `1025` (MailHog local apenas devs) |
| **Usuário** | Usuário de autenticação | `voce@gmail.com` (Gmail) |
| **Senha** | Credencial de autenticação | App Password de 16 caracteres (Gmail) |
| **Criptografia** | `NONE`, `TLS` ou `SSL` | `TLS` (Gmail) |
| **E-mail do Remetente (From)** | Endereço de quem envia; em servidores como Gmail deve ser igual ao usuário | `voce@gmail.com` |
| **Nome de Exibição** | Nome visível ao lado do remetente no e-mail recebido | `Stentio Notificações` |
| **E-mail de teste** | Destinatário do e-mail de teste (seção 7) | qualquer endereço válido |

## 5. Configurando com o Gmail

1. Acesse [myaccount.google.com/security](https://myaccount.google.com/security) e ative a **Verificação em duas etapas** (obrigatória).
2. Acesse [myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords) → **Criar senha de app** → nomeie (ex.: `Stentio SMTP`) → copie o código de **16 caracteres** (sem espaços).
3. Na tela SMTP preencha:
   - **Host SMTP**: `smtp.gmail.com`
   - **Porta SMTP**: `587`
   - **Usuário**: seu e-mail Gmail completo
   - **Senha**: o App Password gerado
   - **Criptografia**: `TLS`
   - **E-mail do Remetente (From)**: seu e-mail Gmail (o Gmail exige que o remetente seja igual ao usuário)
   - **Nome de Exibição**: opcional (ex.: `Stentio Notificações`)
4. Clique em **Salvar Configurações**.
5. Dispare o teste (seção 7). Se não encontrar o email, verifique a **caixa de spam**, pois e-mails para a própria conta podem ser categorizados como spam.

> Contas corporativas (Google Workspace) com políticas restritas podem bloquear senhas de app. Nesse caso, use o servidor SMTP da própria empresa (seção 6).

## 6. Configurando com um servidor corporativo

Preenha os mesmos campos com os dados fornecidos pela operadora do e-mail:

- **TLS** (STARTTLS): normalmente porta `587`.
- **SSL/TLS de ponta a ponta**: normalmente porta `465`.
- **Sem criptografia**: porta `25` ou `1025` — use apenas em redes confiáveis (tráfego em texto puro).

## 7. Enviando e-mail de teste

1. Em **E-mail de teste**, informe um destinatário real (ex.: seu próprio e-mail).
2. Preencha **Nome de Exibição** (opcional) e demais campos.
3. Clique em **Disparar Teste Agora**.
4. O resultado aparece na tela:
   - **sucesso**: "E-mail de teste enviado!" — confira a caixa de entrada (e o spam).
   - **falha**: a mensagem traz o erro do servidor SMTP.

### Erros comuns

| Mensagem | Causa provável |
|----------|----------------|
| `535 5.7.8 Username and Password not accepted` | App Password incorreta ou com espaços |
| `534 ... Application-specific password required` | Verificação em duas etapas desativada |
| `Connection refused` / `Network Error` | email-service fora do ar, host/porta errados ou CORS |
| `Timeout waiting for connection` | Firewall/provedor bloqueando a porta |

### Ambiente de desenvolvimento sem e-mail real (MailHog)

O `docker-compose` sobe o **MailHog** (`mailhog:latest`):
- SMTP local: host `localhost`, porta `1025`, criptografia `NONE` (sem usuário/senha).

- E-mails ficam em `http://localhost:8025` — abra no navegador para ver as mensagens (o remetente e o assunto aparecem, incluindo o **Nome de Exibição** configurado).

## 8. Referência da API

Serviço `email-service` — base `http://localhost:8083`.

| Método | Rota | Descrição |
|--------|------|-----------|
| `GET` | `/api/v1/smtp` | Retorna a configuração salva (ou os padrões) |
| `POST` | `/api/v1/smtp` | Salva a configuração |
| `POST` | `/api/v1/smtp/test` | Envia e-mail de teste **sem salvar** a configuração |
| `GET` | `/actuator/health` | Saúde do serviço |

Exemplo de teste via `curl` com o Gmail:

```bash
curl -X POST http://localhost:8083/api/v1/smtp/test \
  -H "Content-Type: application/json" \
  -d '{
    "host": "smtp.gmail.com",
    "port": 587,
    "username": "voce@gmail.com",
    "password": "suasenhaappde16",
    "encryption": "TLS",
    "fromEmail": "voce@gmail.com",
    "fromName": "Stentio Notificações",
    "testEmail": "voce@gmail.com"
  }'
```

Resposta: `{"success":true,"message":"E-mail de teste enviado!"}`. Em falha, `success` vem `false` com o motivo na mensagem.

## 9. Observações

- **Nome de Exibição** é aplicado ao cabeçalho `From` da mensagem desde 2026-09 (via `MimeMessageHelper`), junto com o e-mail do remetente.
- Credenciais SMTP são armazenadas **em texto puro** na configuração persistida (MongoDB). Para produção, avalie criptografia/secrets ou um provedor de e-mail gerenciado.
- O envio por **fila** (`email.queue`) usa a configuração salva. O teste da tela não passa pela fila.