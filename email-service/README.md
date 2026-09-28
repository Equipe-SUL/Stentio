# Documentação Técnica do Microserviço de E-mail

## 1. Objetivo

O `email-service` centraliza o envio de e-mails da plataforma Stentio. Ele disponibiliza uma API HTTP para envio e configuração SMTP e um consumidor RabbitMQ para processar mensagens de forma assíncrona.

Responsabilidades atuais:

- receber solicitações de envio por HTTP;
- consumir mensagens da fila `email.queue`;
- enviar mensagens por SMTP usando Spring Mail;
- permitir configurar e testar o servidor SMTP;
- persistir a configuração SMTP ativa no MongoDB.

Templates de e-mail, auditoria detalhada e histórico de envios ainda não estão implementados. Eles são descritos neste documento como evolução planejada.

## 2. Tecnologias

| Componente | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1.1 |
| API HTTP | Spring Web MVC |
| Envio SMTP | Spring Mail / Jakarta Mail |
| Mensageria | Spring AMQP e RabbitMQ |
| Persistência | Spring Data MongoDB |
| Testes | JUnit 5, Mockito e MockMvc |
| Ambiente local | Docker Compose, MailHog |

## 3. Arquitetura geral

O frontend ou outro serviço pode enviar e-mails diretamente pela API HTTP. Para fluxos assíncronos, o produtor publica uma mensagem no RabbitMQ e o `email-service` a consome. Em ambos os casos, o envio final é realizado pelo servidor SMTP configurado.

```mermaid
flowchart LR
    U[Frontend ou cliente HTTP] -->|POST /api/v1/emails| API[EmailController]
    P[Backend ou produtor] -->|publica mensagem| EX[Exchange padrão RabbitMQ]
    EX -->|routing key: email.queue| Q[email.queue]
    Q --> C[EmailConsumer]
    API --> S[EmailService]
    C --> S
    S --> CFG[SmtpConfigurationService]
    CFG --> M[(MongoDB)]
    S --> SMTP[SmtpMailSenderFactory]
    SMTP --> MAIL[Servidor SMTP / MailHog]
```

### Componentes principais

- `EmailController`: recebe solicitações HTTP de envio.
- `SmtpController`: expõe leitura, gravação e teste da configuração SMTP.
- `EmailService`: monta e envia a mensagem.
- `EmailConsumer`: escuta a fila RabbitMQ e delega o envio.
- `SmtpConfigurationService`: converte, persiste e recupera a configuração SMTP.
- `SmtpMailSenderFactory`: cria um `JavaMailSender` com host, porta, autenticação e criptografia configurados.
- `SmtpConfigurationRepository`: acesso ao documento SMTP no MongoDB.

## 4. SMTP e Spring Mail

SMTP (Simple Mail Transfer Protocol) é o protocolo usado para transferir mensagens entre clientes e servidores de e-mail. O fluxo básico é:

1. o cliente abre uma conexão com o host e a porta SMTP;
2. autentica, quando necessário;
3. negocia criptografia, quando configurada;
4. informa remetente e destinatário;
5. transmite cabeçalhos e corpo da mensagem;
6. o servidor SMTP aceita ou rejeita a mensagem.

O serviço cria o remetente SMTP dinamicamente com base na configuração persistida:

- `TLS`: ativa STARTTLS e exige a negociação segura;
- `SSL`: ativa SSL na conexão SMTP;
- `NONE`: não ativa TLS ou SSL;
- usuário e senha preenchidos: habilitam autenticação SMTP.

O corpo atual é texto simples (`SimpleMailMessage`). O `fromEmail` é aplicado à mensagem; `fromName` já é persistido, mas ainda não é aplicado ao cabeçalho do e-mail. A dependência Thymeleaf existe no projeto, mas não há templates sendo carregados neste momento.

## 5. Integração HTTP

### 5.1 Envio de e-mail

```http
POST /api/v1/emails
Content-Type: application/json
```

Request:

```json
{
  "to": "destinatario@exemplo.com",
  "subject": "Assunto do e-mail",
  "body": "Conteudo da mensagem"
}
```

Campos `to`, `subject` e `body` são obrigatórios. O serviço retorna:

```text
E-mail enviado!
```

### 5.2 Buscar configuração SMTP

```http
GET /api/v1/smtp
```

Response:

```json
{
  "host": "smtp.example.com",
  "port": 587,
  "username": "usuario@example.com",
  "password": "senha-ou-app-password",
  "encryption": "TLS",
  "fromEmail": "noreply@example.com",
  "fromName": "Stentio"
}
```

Quando ainda não existe documento no MongoDB, o serviço retorna uma configuração local padrão com `localhost:1025` e criptografia `NONE`.

> A senha ainda é devolvida pela API para manter compatibilidade com o contrato atual do frontend. Em produção, esse comportamento deve ser substituído por senha mascarada ou omitida.

### 5.3 Salvar configuração SMTP

```http
POST /api/v1/smtp
Content-Type: application/json
```

Request:

```json
{
  "host": "smtp.example.com",
  "port": 587,
  "username": "usuario@example.com",
  "password": "senha-ou-app-password",
  "encryption": "TLS",
  "fromEmail": "noreply@example.com",
  "fromName": "Stentio"
}
```

Valores aceitos para `encryption`: `TLS`, `SSL` e `NONE`. A porta deve estar entre `1` e `65535`.

### 5.4 Testar configuração SMTP

```http
POST /api/v1/smtp/test
Content-Type: application/json
```

Request:

```json
{
  "host": "localhost",
  "port": 1025,
  "username": "",
  "password": "",
  "encryption": "NONE",
  "fromEmail": "noreply@stentio.local",
  "fromName": "Stentio Local",
  "testEmail": "destino@stentio.local"
}
```

Response de sucesso:

```json
{
  "success": true,
  "message": "E-mail de teste enviado!"
}
```

Em caso de falha, `success` será `false` e a mensagem conterá o diagnóstico retornado pelo provedor SMTP.

## 6. RabbitMQ

### Configuração atual

| Item | Valor |
|---|---|
| Fila | `email.queue` |
| Tipo da fila | Classic, durável |
| Exchange explícita | Nenhuma |
| Exchange utilizada | Exchange padrão (`amq.default`) |
| Routing key | `email.queue` |
| Consumidor | `EmailConsumer` |
| ACK | Automático após processamento da mensagem |

O código declara somente a fila. Como não existe exchange ou binding customizado, o produtor deve publicar na exchange padrão usando a própria fila como routing key.

Mensagem esperada:

```json
{
  "to": "destinatario@exemplo.com",
  "subject": "Assunto do e-mail",
  "body": "Conteudo da mensagem"
}
```

Fluxo de processamento:

1. o produtor publica a mensagem em `amq.default` com routing key `email.queue`;
2. RabbitMQ entrega a mensagem ao consumidor registrado;
3. `EmailConsumer` converte o JSON em `SendEmailRequest`;
4. `EmailService` carrega a configuração SMTP do MongoDB;
5. a mensagem é enviada ao SMTP;
6. o processamento é reconhecido pelo RabbitMQ. Atualmente, falhas de envio são convertidas em `EMAIL_SEND_FAILED` pelo consumidor e não são relançadas para uma fila de retry ou dead-letter.

Atualmente não existem eventos de sucesso ou falha publicados em outra fila. O próximo passo recomendado é criar eventos como `email.sent` e `email.failed` para rastreabilidade e reprocessamento.

## 7. Persistência MongoDB

### 7.1 Configuração SMTP

Coleção: `smtp_configuration`

Documento singleton atual:

```json
{
  "_id": "default",
  "host": "smtp.example.com",
  "port": 587,
  "username": "usuario@example.com",
  "password": "senha-ou-app-password",
  "encryption": "TLS",
  "fromEmail": "noreply@example.com",
  "fromName": "Stentio"
}
```

O ID fixo `default` garante uma configuração ativa por instância do serviço. O repository utilizado é `SmtpConfigurationRepository`.

### 7.2 Templates de e-mail

Não há model, collection ou endpoint de templates implementado atualmente. A evolução recomendada é uma coleção `email_templates`:

```json
{
  "_id": "welcome-user",
  "name": "welcome-user",
  "subject": "Bem-vindo, {{name}}",
  "body": "Ola, {{name}}",
  "format": "TEXT",
  "version": 1,
  "active": true,
  "updatedAt": "2026-09-18T18:00:00Z"
}
```

O uso de versões permite atualizar templates sem invalidar mensagens já geradas.

### 7.3 Auditoria e histórico

Ainda não existe collection de auditoria. Para a próxima evolução, recomenda-se `email_deliveries`:

```json
{
  "_id": "uuid",
  "to": "destinatario@exemplo.com",
  "subject": "Assunto",
  "template": "welcome-user",
  "status": "SENT",
  "source": "HTTP",
  "messageId": "rabbit-message-id",
  "error": null,
  "createdAt": "2026-09-18T18:00:00Z",
  "sentAt": "2026-09-18T18:00:02Z"
}
```

Essa estrutura deve evitar armazenar senha SMTP e, quando necessário, mascarar o conteúdo da mensagem por privacidade.

## 8. Configurações e variáveis de ambiente

### Aplicação

| Variável | Uso | Padrão |
|---|---|---|
| `SERVER_PORT` | Porta HTTP do email-service | `8080` |
| `MONGODB_URI` | URI principal do MongoDB | `mongodb://root:changeme@localhost:27017/admin?authSource=admin` |
| `SPRING_MONGODB_URI` | URI alternativa reconhecida pelo Spring Boot | mesma URI padrão |
| `MAILHOG_HOST` | Host SMTP local | `localhost` |
| `MAILHOG_PORT_SMTP` | Porta SMTP | `1025` |
| `RABBITMQ_HOST` | Host RabbitMQ | `localhost` |
| `RABBITMQ_PORT` | Porta AMQP | `5672` |
| `RABBITMQ_DEFAULT_USER` | Usuário RabbitMQ | `myuser` |
| `RABBITMQ_DEFAULT_PASS` | Senha RabbitMQ | `changeme` |

Para o ambiente local deste projeto, o `.env` usa portas externas alternativas para evitar conflitos:

| Serviço | Porta externa |
|---|---:|
| Backend | `18080` |
| MongoDB | `27018` |
| RabbitMQ AMQP | `25672` |
| RabbitMQ Management | `25673` |
| MailHog SMTP | `11025` |
| MailHog Web | `18025` |

Exemplo de execução local do serviço:

```bash
cd email-service
SPRING_MONGODB_URI="mongodb://root:changeme@localhost:27018/admin?authSource=admin" \
RABBITMQ_PORT=25672 \
MAILHOG_PORT_SMTP=11025 \
./gradlew bootRun
```

O `docker-compose.yml` atual sobe MongoDB, RabbitMQ e MailHog, mas não declara o container `email-service`; o serviço deve ser iniciado separadamente ou adicionado ao Compose em uma evolução futura.

## 9. Testes

### Cenários implementados

- retorno da configuração SMTP salva;
- persistência da configuração no repository MongoDB;
- validação de host, porta e tipo de criptografia;
- teste da rota `POST /api/v1/smtp/test`;
- envio de e-mail com os campos obrigatórios;
- retorno de sucesso do consumidor RabbitMQ;
- retorno de falha quando o envio SMTP falha;
- carregamento do contexto Spring Boot.

Os testes de controller estão em `SmtpControllerTest` e os testes da persistência/configuração em `SmtpConfigurationServiceTest`.
O teste de persistência verifica o contrato do serviço com um repository mockado; a validação contra uma instância Mongo real deve ser executada como teste de integração ou durante a validação local.

### Execução automatizada

```bash
cd email-service
./gradlew test
```

### Validação local manual

1. Subir a infraestrutura:

   ```bash
   docker compose up -d mongodb rabbitmq mailhog
   ```

2. Iniciar o serviço com as variáveis de ambiente locais.
3. Salvar uma configuração com `POST /api/v1/smtp`.
4. Buscar a configuração com `GET /api/v1/smtp`.
5. Executar `POST /api/v1/smtp/test`.
6. Conferir a mensagem no MailHog em `http://localhost:18025`.
7. Publicar uma mensagem em `email.queue` e verificar o ACK no RabbitMQ Management em `http://localhost:25673`.

### Homologação

Em homologação, os testes devem usar um servidor SMTP de teste ou sandbox, credenciais armazenadas em secret manager e um RabbitMQ separado. Devem ser verificados:

- conexão autenticada e criptografada com o SMTP;
- publicação e consumo da fila;
- comportamento de retry e mensagens rejeitadas;
- persistência e recuperação da configuração no MongoDB;
- ausência de senha em logs, respostas e métricas;
- disponibilidade dos health checks e métricas do Actuator.

## 10. Monitoramento e boas práticas

Recomenda-se evoluir o serviço com:

- métricas de mensagens recebidas, enviadas, rejeitadas e reprocessadas;
- correlação por `traceId` e `messageId`;
- filas de dead-letter para falhas permanentes;
- retry com backoff para indisponibilidade temporária do SMTP;
- idempotência para evitar envio duplicado;
- timeout e circuit breaker para provedores externos;
- logs estruturados sem senha ou corpo sensível;
- health checks de RabbitMQ, MongoDB e SMTP;
- auditoria persistida do status de cada envio.

Essas práticas são especialmente importantes porque o envio de e-mail depende de um sistema externo e pode falhar depois que a mensagem já foi retirada da fila.