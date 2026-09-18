# Stentio — Backend

Backend do projeto Stentio, desenvolvido em Java 25 com Spring Boot, seguindo uma arquitetura de microsserviços. Este serviço utiliza PostgreSQL, MongoDB e RabbitMQ.

## Stack

- Linguagem: Java 25
- Framework: Spring Boot 4.1.1
- Build tool: Gradle
- Bancos de dados: PostgreSQL (relacional) e MongoDB (não relacional)
- Mensageria: RabbitMQ
- Autenticação: Spring Security + JWT (RBAC por role: `ADMIN` e `ATENDENTE`)
- Containerização: Docker / Docker Compose

## Pré-requisitos

Antes de rodar o projeto, você precisa ter instalado:

- JDK 25 (sudo apt install openjdk-25-jdk no Debian/Ubuntu)
- Docker e Docker Compose
- Git

## Como rodar localmente

### 1. Clone o repositório

    git clone https://github.com/Equipe-SUL/5-ADS-API.git
    cd 5-ADS-API

### 2. Configure as variáveis de ambiente

Na raiz do repositório (não dentro de backend/), copie o arquivo de exemplo:

    cp .env.example .env

Os valores padrão já funcionam para desenvolvimento local — não é necessário editar nada para rodar o projeto pela primeira vez.

> O `.env` é lido apenas pelo Docker Compose (item 3). As variáveis de **JWT** e **seed de admin** são consumidas pelo processo Java (item 4) e precisam ser informadas no shell/IDE — veja [Ativando o seed de admin](#ativando-o-seed-de-admin-opcional).

### 3. Suba os containers (Postgres, MongoDB, RabbitMQ)

Ainda na raiz do repositório:

    docker compose up -d

Confirme que os três serviços subiram corretamente:

    docker compose ps

Você deve ver postgresdb, mongodb-local e rabbitmq-local com status Up.

### 4. Rode a aplicação Spring Boot

    cd backend
    chmod +x gradlew
    ./gradlew bootRun

Na primeira execução, o Gradle vai baixar as dependências — isso pode levar alguns minutos. Aguarde até aparecer no log algo como:

    Started StentioApplication in X seconds

A aplicação ficará rodando nesse terminal (isso é esperado, não é um travamento). Para parar, use Ctrl+C.

#### Ativando o seed de admin (opcional)

Por padrão o seed está **desligado**. Para já subir a aplicação criando um usuário `ADMIN` de teste, informe as variáveis no mesmo comando:

    SEED_ADMIN_ENABLED=true \
    ADMIN_INITIAL_EMAIL=admin@stentio.com \
    ADMIN_INITIAL_PASSWORD='SenhaForte@123' \
    ./gradlew bootRun

No IntelliJ IDEA: `Run > Edit Configurations… > StentioApplication > Environment variables` e adicione, separadas por `;`:

    SEED_ADMIN_ENABLED=true;ADMIN_INITIAL_EMAIL=admin@stentio.com;ADMIN_INITIAL_PASSWORD=SenhaForte@123

Comportamento do seed:

- Só executa com `SEED_ADMIN_ENABLED=true` **e** email/senha preenchidos;
- Cria um único `ADMIN` (nome `Administrador`) se o email ainda **não existir** no banco;
- Armazena a senha já com **BCrypt** (não passa pela regra de cadastro, mas fica segura);
- É idempotente: ao reiniciar com os mesmos dados, não duplica o usuário.

Resultado: **email `admin@stentio.com` / senha `SenhaForte@123`** — as credenciais usadas no teste abaixo.

### 5. Verifique se está tudo funcionando

Em outro terminal (sem fechar o anterior), rode:

    curl http://localhost:8080/actuator/health

Se a resposta vier com "status":"UP", o backend está rodando corretamente e conectado aos bancos de dados.

## Autenticação (JWT)

- `POST /api/v1/usuarios/login` é público e retorna o token (`token`, `tipo`, `expiresIn`, `email`, `role`);
- As demais rotas de `/api/v1/usuarios/**` exigem `Authorization: Bearer <token>` e role `ADMIN`;
- Sem token → `401`; token válido sem role → `403`;
- No cadastro, `senha` exige mínimo 8 caracteres, um dígito e um caractere especial; sem `role`, assume `ATENDENTE`;
- Em produção, defina `JWT_SECRET` com uma chave forte (o padrão é só para dev).

## Teste rápido — login e listagem (com seed)

Objetivo: confirmar que o seed criou o admin e que o token funciona.

1. Na collection do Postman, defina `base_url = http://localhost:8080/api/v1/usuarios`;
2. No request de **login**, aba *Tests*: `pm.collectionVariables.set('token', pm.response.json().token);`
3. **Login**: `POST {{base_url}}/login`

   ```json
   { "email": "admin@stentio.com", "senha": "SenhaForte@123" }
   ```

   → `200` com o token. O script do passo 2 salva o token automaticamente.
4. **Listar**: `GET {{base_url}}` com *Authorization* → **Bearer Token** → `{{token}}` → `200` com a lista contendo o admin.

> Opcional: login com senha errada retorna `401` — prova que a senha é conferida via hash.

**Mesmo fluxo via curl** (quem não usa Postman):

```bash
# 1. Login — retorna o JSON com o token
curl -s -X POST http://localhost:8080/api/v1/usuarios/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@stentio.com","senha":"SenhaForte@123"}'

# 2. Listar — troque <token> pelo valor do campo "token" da resposta acima
curl -s http://localhost:8080/api/v1/usuarios \
  -H "Authorization: Bearer <token>"
```

## Estrutura do projeto

    backend/
    ├── src/
    │   ├── main/
    │   │   ├── java/com/example/stentio/
    │   │   │   ├── StentioApplication.java
    │   │   │   ├── controller/     # Endpoints REST
    │   │   │   ├── service/        # Regras de negócio
    │   │   │   ├── repository/     # Acesso a dados (JPA / MongoDB)
    │   │   │   ├── model/          # Entidades
    │   │   │   ├── dto/            # Objetos de transferência (request/response)
    │   │   │   ├── config/         # Segurança/JWT, seed de admin, filas
    │   │   │   └── exception/      # Tratamento de exceções
    │   │   └── resources/
    │   │       └── application.yaml
    │   └── test/
    ├── build.gradle
    └── settings.gradle

## Portas utilizadas

| Serviço          | Porta  |
|------------------|--------|
| Aplicação (API)  | 8080   |
| PostgreSQL       | 5432   |
| MongoDB          | 27017  |
| RabbitMQ (AMQP)  | 5672   |
| RabbitMQ (painel)| 15672  |

O painel de gerenciamento do RabbitMQ fica disponível em http://localhost:15672 (usuário/senha definidos no .env).

## Observações

- O health-check do MongoDB (/actuator/health) está desativado (management.health.mongodb.enabled: false) devido a um bug conhecido do Spring Boot 4.x nessa verificação específica. A conexão real com o MongoDB funciona normalmente para leitura e escrita — apenas o indicador de saúde automático está desligado.
- Para parar os containers Docker: docker compose down (na raiz do repositório).
- Para parar os containers e apagar os dados (reset completo dos bancos): docker compose down -v.