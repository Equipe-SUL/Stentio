# Stentio Auth

Serviço de autenticação, sessão e gerenciamento de usuários do Stentio. O
serviço usa Spring Boot, PostgreSQL, JWT e Docker Compose; as configurações
compartilhadas ficam no `.env` da raiz do repositório.

## Executar

Na raiz do repositório:

```bash
docker compose up -d --build auth
```

O Compose publica a API em `http://localhost:8081` e a aplicação escuta na
porta `8080` dentro do container. O serviço depende do PostgreSQL, MongoDB e
RabbitMQ definidos no Compose.

Para executar fora do Docker, inicie primeiro a infraestrutura na raiz:

```bash
docker compose up -d postgres mongodb rabbitmq
cd Auth
./gradlew bootRun
```

## Administrador inicial

Configure no `.env` da raiz:

```env
SEED_ADMIN_ENABLED=true
ADMIN_INITIAL_EMAIL=admin@stentio.com
ADMIN_INITIAL_PASSWORD=defina-uma-senha-forte
```

O initializer cria o administrador somente quando o e-mail ainda não existe.
Ele não substitui nem redefine a senha de uma conta já cadastrada.

## API e papéis

O login é público:

```http
POST /api/v1/usuarios/login
Content-Type: application/json
```

```json
{
  "email": "admin@stentio.com",
  "senha": "defina-uma-senha-forte"
}
```

A resposta contém o JWT e o papel (`ADMIN`, `ATENDENTE`, `GESTOR_PROJETO`,
`FINANCEIRO` ou `RECURSO`). Envie o token nas rotas protegidas como
`Authorization: Bearer <token>`; no frontend web, a sessão também pode ser
transportada pelo cookie HttpOnly.

O cadastro público `POST /api/v1/public/usuarios` cria contas com papel
`RECURSO`. Após o login, o profissional conclui seu perfil no Core. Os demais
usuários são gerenciados pela equipe conforme as permissões do sistema.

## Testes

```bash
cd Auth
./gradlew test
```
