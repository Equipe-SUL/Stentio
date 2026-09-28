# ESUL - API ADS 5º Semestre
# Stentio

<p align="center">
      <img src="docs/logosul.png" alt="logo" width="200">
      <h2 align="center">Equipe SUL</h2>
</p>

<p align="center">
  | <a href ="#desafio"> Desafio</a>  |
  <a href ="#solucao"> Solução</a>  |
  <a href ="#arquitetura"> Arquitetura</a>  |
  <a href ="#tecnologias">Tecnologias</a> |
  <a href ="#backlog"> Backlog do Produto</a>  |
  <a href ="#dor">DoR</a>  |
  <a href ="#dod">DoD</a>  |
  <a href ="#sprint"> Cronograma de Sprints</a>  |
  <a href ="#manual"> Manual de Instalação</a>  | 
  <a href ="#equipe"> Equipe</a> |
</p>

> Status do Projeto: Em Desenvolvimento 🛠
>
> Vídeo do Projeto: Em Desenvolvimento 📽️

## 🏅 Desafio <a id="desafio"></a>
Agências de tradução precisam captar a solicitação do cliente, orçar o serviço, executá-lo em várias etapas e fechar a cobrança. Na prática esse fluxo é apoiado por planilhas, e-mails e conversas avulsas: o atendente monta o orçamento na mão, o gestor distribui as etapas de tradução, revisão e formatação por mensagem, o profissional recebe o arquivo por onde der e, no fim, não existe uma fonte confiável de quem fez o quê, em que prazo e a que custo.

O resultado é atraso na aprovação do orçamento, alocação de profissionais sem conferir compatibilidade de idiomas e tipo de serviço, retrabalho por informação perdida no meio do caminho e dificuldade de emitir a fatura de venda e o repasse ao recurso. Resolver isso exige um sistema que padronize o fluxo inteiro, da captação ao financeiro, e não apenas um lugar para armazenar os arquivos.

## 🏅 Solução <a id="solucao"></a>
O **Stentio** é uma aplicação web que centraliza a operação de uma agência de tradução. O sistema cobre o ciclo completo: cadastros base (empresa, clientes, recursos, idiomas, tipos de serviço, categorias de projeto e tabelas de preço), captação da solicitação, elaboração do orçamento com envio ao cliente e aprovação por link, conversão do orçamento aprovado em Ordem de Serviço com etapas (workflow), execução com alocação de recursos e aceite por link sem necessidade de cadastro, e fechamento financeiro com fatura de venda, fatura de compra e relatório de lucro.

---

## 💻 Tecnologias <a id="tecnologias"></a>
<h4 align="center">
      <img title="Java" alt="Java" src="https://skillicons.dev/icons?i=java" />
      <img title="Spring Boot" alt="Spring Boot" src="https://skillicons.dev/icons?i=springboot" />
      <img title="Gradle" alt="Gradle" src="https://skillicons.dev/icons?i=gradle" />
      <img title="PostgreSQL" alt="PostgreSQL" src="https://skillicons.dev/icons?i=postgresql" />
      <img title="MongoDB" alt="MongoDB" src="https://skillicons.dev/icons?i=mongodb" />
      <img title="RabbitMQ" alt="RabbitMQ" src="https://skillicons.dev/icons?i=rabbitmq" />
      <img src="https://skillicons.dev/icons?i=null">
      <img title="React" alt="React" src="https://skillicons.dev/icons?i=react" />
      <img title="TypeScript" alt="TypeScript" src="https://skillicons.dev/icons?i=typescript" />
      <img title="Expo" alt="Expo" src="https://skillicons.dev/icons?i=expo" />
      <img title="Tailwind CSS" alt="Tailwind CSS" src="https://skillicons.dev/icons?i=tailwind" />
      <img src="https://skillicons.dev/icons?i=null">
      <img title="Docker" alt="Docker" src="https://skillicons.dev/icons?i=docker" />
      <img title="Git" alt="Git" src="https://skillicons.dev/icons?i=git" />
      <img title="GitHub" alt="GitHub" src="https://skillicons.dev/icons?i=github" />
      <img title="Jira" alt="Jira" src="https://skillicons.dev/icons?i=jira" />
      <img title="VS Code" alt="VS Code" src="https://skillicons.dev/icons?i=vscode" />
      <img title="IntelliJ IDEA" alt="IntelliJ IDEA" src="https://skillicons.dev/icons?i=intellij" />
</h4>

---

## 🏗 Arquitetura <a id="arquitetura"></a>
A solução é dividida em microsserviços independentes, deployados com Docker Compose.

| Serviço | Responsabilidade | Porta |
| :------ | :---------------- | :----: |
| **front** | Interface web e mobile (Expo / React Native Web) | 8080 |
| **auth** | Autenticação, usuários, sessão e controle de acesso por perfil (JWT) | 8081 |
| **core** | Domínio de negócio: clientes, recursos, catálogos, tabelas de preço e solicitações | 8082 |
| **email-service** | Envio de e-mails, configuração SMTP e dados da empresa | 8083 |

| Infraestrutura | Finalidade | Porta |
| :-------------- | :---------- | :----: |
| **PostgreSQL** | Dados relacionais | 5432 |
| **MongoDB** | Configurações e documentos não relacionais | 27017 |
| **RabbitMQ** | Mensageria assíncrona | 5672 / 15672 |

---

## 📋 Backlog do Produto <a id="backlog"></a>

> Detalhamento de épicos e histórias em [`docs/processo/product-backlog.md`](./docs/processo/product-backlog.md).

| # | US | Prioridade | User Story | Story Points | Épico | Sprint | Status |
| :--: | :--------- | :--------: | :--------- | :----------: | :---: | :----: | :----: |
| 1 | US-001 | 🔴 ALTA | Como Administrador, quero que cada funcionário acesse o sistema com login e senha e tenha permissões baseadas em seu perfil, para proteger os dados e delimitar as ações disponíveis a cada papel. | 8 | E6 | 1 | 🔲 Não iniciada |
| 2 | US-002 | 🔴 ALTA | Como Administrador, quero cadastrar os dados da empresa e configurar o servidor SMTP de envio de e-mails, para que as informações apareçam nos documentos gerados e os disparos de e-mail funcionem desde o início do fluxo. | 5 | E6 | 1 | 🔲 Não iniciada |
| 3 | US-003 | 🔴 ALTA | Como Administrador, quero cadastrar tipos de serviço, categorias de projeto e idiomas disponíveis, para que sirvam de base nos orçamentos, ordens de serviço e cadastros de recursos em todo o sistema. | 5 | E6 | 1 | 🔲 Não iniciada |
| 4 | US-004 | 🔴 ALTA | Como Administrador, quero cadastrar recursos associando seus pares de idiomas e valores individuais, para viabilizar a alocação e o cálculo de custo nas ordens de serviço. | 5 | E6 | 1 | 🔲 Não iniciada |
| 5 | US-005 | 🟠 MÉDIA | Como Administrador, quero cadastrar tabelas de preço associando tipo de serviço, par de idiomas e valor unitário, para que o sistema sugira automaticamente os valores ao montar um orçamento. | 8 | E6 | 1 | 🔲 Não iniciada |
| 6 | US-006 | 🟠 MÉDIA | Como Administrador, quero cadastrar e gerenciar templates de e-mail com placeholders dinâmicos, para padronizar e automatizar as comunicações com clientes e recursos sem depender de alterações de código. | 3 | E6 | 1 | 🔲 Não iniciada |
| 7 | US-007 | 🔴 ALTA | Como Atendente, quero cadastrar manualmente uma solicitação de tradução vinculando um cliente (novo ou existente), para registrar demandas que chegam por e-mail, telefone ou outros canais de forma estruturada no sistema. | 8 | E2 · E6 | 1 | 🔲 Não iniciada |
| 8 | US-008 | 🔴 ALTA | Como Atendente, quero elaborar um orçamento com os itens de serviço necessários e enviá-lo por e-mail ao cliente com um link de aprovação, para que o cliente possa aprovar ou recusar de forma autônoma. | 8 | E2 | 1 | 🔲 Não iniciada |
| 9 | US-009 | 🟠 MÉDIA | Como Atendente, quero editar um orçamento recusado pelo cliente e reenviar para aprovação, para viabilizar negociações sem precisar criar um novo orçamento do zero. | 0 | E2 | 2 | 🔲 Não iniciada |
| 10 | US-010 | 🔴 ALTA | Como Gestor de Projetos, quero criar, editar e reutilizar modelos de workflow, para agilizar a definição de etapas ao abrir novas ordens de serviço. | 0 | E6 | 2 | 🔲 Não iniciada |
| 11 | US-011 | 🔴 ALTA | Como Gestor de Projetos, quero que ao aprovar um orçamento e uma Ordem de Serviço seja criada automaticamente, e que eu possa definir suas etapas aplicando um modelo de workflow ou montando manualmente, para iniciar o planejamento da produção. | 0 | E3 | 2 | 🔲 Não iniciada |
| 12 | US-012 | 🔴 ALTA | Como Gestor de Projetos, quero selecionar e alocar um recurso a cada etapa da OS verificando compatibilidade de idiomas e tipo de serviço, para garantir que cada etapa seja atribuída ao profissional adequado antes de iniciar a execução. | 0 | E3 | 2 | 🔲 Não iniciada |
| 13 | US-013 | 🔴 ALTA | Como Recurso, quero receber um e-mail com link exclusivo para aceitar ou rejeitar um serviço alocado e, após conclusão, fazer o upload do arquivo entregue, sem precisar criar conta ou senha no sistema. | 0 | E4 | 3 | 🔲 Não iniciada |
| 14 | US-014 | 🟠 MÉDIA | Como Gestor de Projetos, quero que ao registrar a entrega de uma etapa o sistema avance automaticamente para a próxima etapa do workflow notificando o recurso alocado, para reduzir a intervenção manual no roteamento entre etapas. | 0 | E4 | 3 | 🔲 Não iniciada |
| 15 | US-015 | 🔴 ALTA | Como Gestor de Projetos, quero revisar todos os arquivos entregues nas etapas concluídas, validar o pacote final e enviar os documentos traduzidos ao cliente por e-mail, para encerrar formalmente a Ordem de Serviço. | 0 | E4 | 3 | 🔲 Não iniciada |
| 16 | US-016 | 🟠 MÉDIA | Como Assistente Financeiro, quero gerar a fatura de venda de uma OS entregue, para formalizar a cobrança ao cliente com os dados corretos do serviço prestado. | 0 | E5 | 3 | 🔲 Não iniciada |
| 17 | US-017 | 🟠 MÉDIA | Como Assistente Financeiro, quero calcular e gerar a fatura de compra do trabalho executado por cada recurso nas etapas da OS, para organizar o repasse e registrar o pagamento aos profissionais envolvidos. | 0 | E5 | 3 | 🔲 Não iniciada |
| 18 | US-018 | 🟡 BAIXA | Como Assistente Financeiro, quero visualizar um relatório de lucro por Ordem de Serviço, para acompanhar a rentabilidade de cada projeto. | 0 | E5 | 3 | 🔲 Não iniciada |
| 19 | US-019 | 🟡 BAIXA | Como Administrador, quero visualizar um log de auditoria com todas as alterações de status realizadas em orçamentos, OS e etapas, para fins de rastreabilidade interna. | 0 | E6 | 3 | 🔲 Não iniciada |
| 20 | US-020 | 🟠 MÉDIA | Como Gestor de Projetos, quero visualizar um dashboard com contadores e listas dinâmicas das principais pendências operacionais, para gerir a operação a partir de uma única tela. | 0 | E1 | 3 | 🔲 Não iniciada |

---

## 🏃‍ DoR - Definition of Ready <a id="dor"></a>

| Critério | Descrição |
| :------------------------------: | :----------------------------------------------------------------------------------------------- |
| **Clareza na Descrição** | A User Story está escrita no formato "Como [persona], quero [ação] para que [objetivo]". |
| **Critérios de Aceitação Definidos** | A história possui critérios objetivos que indicam o que é necessário para considerá-la concluída. |
| **Independente** | A história pode ser implementada sem depender de outra tarefa da mesma Sprint. |
| **Cenários de Teste Especificados** | A história tem pelo menos 1 cenário de teste estruturado (Dado, Quando, Então). |
| **Compreensão Compartilhada** | Toda a equipe (incluindo PO e devs) compreende o propósito da história. |
| **Estímável** | A história foi pontuada no Planning Poker ou tem uma estimativa clara. |
| **Documentos de Apoio** | Se necessário, mockups, fluxos ou modelos de dados estão anexados ou referenciados. |

## 🏆 DoD - Definition of Done <a id="dod"></a>

| Critério | Descrição |
| :--------------------------------------: | :------------------------------------------------------------------------------------- |
| **Critérios de Aceitação atendidos** | Todos os critérios e regras de negócio da User Story foram atendidos. |
| **Testes manuais realizados** | Onde aplicável, os dados são corretamente armazenados e recuperáveis. |
| **Código revisado** | O código foi revisado por pelo menos um colega de equipe. |
| **Integração com outras partes testadas** | As interfaces entre Frontend e Backend foram validadas. |
| **Testes automatizados (se aplicável)** | A funcionalidade não quebra a aplicação e passa nos testes automatizados existentes. |
| **Validação do PO** | O Product Owner validou a entrega com base nos critérios definidos. |

---

## 📅 Cronograma de Sprints <a id="sprint"></a>

| Sprint | Período | Documentação |
| :--- | :-----------: | :--- |
| 🔖 **SPRINT 1** | 07/09 - 27/09 | [Sprint 1 Docs](./docs/processo/sprint-1/) |
| 🔖 **SPRINT 2** | 05/10 - 25/10 | a documentar |
| 🔖 **SPRINT 3** | 02/11 - 22/11 | a documentar |

---

## 📖 Manual de Instalação <a id="manual"></a>

### 🛠 Pré-requisitos

- **Git** — clonagem do repositório
- **Docker** e **Docker Compose** — sobe o frontend, os microsserviços e a infraestrutura

Para desenvolver um serviço fora do Docker, instale também:

- **JDK 25** — microsserviços Java (Spring Boot 4.1.1); use o Gradle embutido (`./gradlew`)
- **Node.js 20+** — frontend (Expo SDK 57; o Dockerfile usa Node 24)

### 1. Clonar o projeto

```bash
git clone https://github.com/Equipe-SUL/Stentio.git
cd Stentio
```

### 2. Configurar as variáveis de ambiente

```bash
cp .env.example .env
```

Os valores padrão já funcionam para desenvolvimento local. Para mais detalhes de cada variável, veja o [`.env.example`](./.env.example).

### 3. Subir a aplicação (Docker)

```bash
docker compose up -d --build
```

Para incluir o MailHog — que captura os e-mails enviados em vez de entregá-los — use o perfil `dev`:

```bash
docker compose --profile dev up -d --build
```

Acompanhe a subida com `docker compose ps` e os logs com `docker compose logs -f core`.

| Serviço | URL |
| :------ | :-- |
| Frontend | http://localhost:8080 |
| Auth (API) | http://localhost:8081 |
| Core (API) | http://localhost:8082 |
| email-service (API) | http://localhost:8083 |
| RabbitMQ (painel) | http://localhost:15672 |

### 4. Rodar os serviços fora do Docker (opcional)

Suba apenas a infraestrutura:

```bash
docker compose up -d postgres mongodb rabbitmq
docker compose --profile dev up -d mailhog
```

E depois, cada serviço em um terminal separado:

```bash
# Autenticação e usuários
cd Auth && ./gradlew bootRun

# Domínio de negócio
cd Core && ./gradlew bootRun

# E-mail
cd email-service && ./gradlew bootRun

# Frontend (Expo Web)
cd frontend && npm install && npx expo start --web
```

O Expo serve o frontend em `http://localhost:19006`. Aponte o frontend para as APIs em `frontend/.env`:

```env
EXPO_PUBLIC_API_URL=http://localhost:8081
EXPO_PUBLIC_CORE_API_URL=http://localhost:8082
EXPO_PUBLIC_EMAIL_API_URL=http://localhost:8083
```

> Para testar no Expo Go ou emulador, troque `localhost` pelo IP da máquina (ex.: `http://192.168.0.10:8081`) e libere a origem em `APP_CORS_ALLOWED_ORIGINS`.

### 5. Criar o primeiro usuário (admin)

O Auth pode criar um administrador automaticamente no primeiro boot. Com Docker, ative no `.env`:

```env
SEED_ADMIN_ENABLED=true
ADMIN_INITIAL_EMAIL=admin@stentio.com
ADMIN_INITIAL_PASSWORD=SenhaForte@123
```

```bash
docker compose up -d auth
```

Rodando localmente, informe as variáveis no mesmo comando:

```bash
cd Auth
SEED_ADMIN_ENABLED=true \
ADMIN_INITIAL_EMAIL=admin@stentio.com \
ADMIN_INITIAL_PASSWORD='SenhaForte@123' \
./gradlew bootRun
```

O seed é idempotente: só cria o usuário se o e-mail ainda não existir no banco. Depois é possível criar os demais usuários pela tela de **Configurações → Usuários**.

### 6. Parar os serviços

```bash
# Parar os containers
docker compose down

# Parar os containers e apagar os volumes (reset completo dos bancos)
docker compose down -v
```

### 7. Documentação complementar

- [`Auth/README.md`](./Auth/README.md) — autenticação, JWT, seed de admin e testes de API
- [`email-service/README.md`](./email-service/README.md) — arquitetura do serviço de e-mail
- [`docs/usuario/configuracao-smtp.md`](./docs/usuario/configuracao-smtp.md) — manual de configuração do SMTP
- [`docs/processo/product-backlog.md`](./docs/processo/product-backlog.md) — épicos, histórias e resumo por sprint

## 🎓 Equipe <a id="equipe"></a>

| Função | Nome | GitHub | LinkedIn |
| :-------------- | :---------------- | :------ | :-------- |
| **Product Owner** | João Álvaro | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/JoaoAlv4ro) | [![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/joaoalv4ro) |
| **Scrum Master** | Raul Germano | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/Raul-Germano-Rosendo) | [![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/raul-germano-rod/) |
| **Desenvolvedor** | Celso Moreira | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/yCels) | [![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/celso-moreira-freitas-957832222) |
| **Desenvolvedor** | Leo Naito | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/LNaito) |  |
| **Desenvolvedor** | Rafael Candido | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/rafa2-bit) | [![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/rafael-candido-155705317) |
| **Desenvolvedor** | Uanderson Leonardo | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/uandleon) | [![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/uanderson-leonardo-1aaa722a0/) |
| **Desenvolvedor** | Vivian Santos | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/vivianSantos0101) | [![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/vivianstoliveira) |
| **Desenvolvedor** | Wesley Xavier | [![GitHub](https://img.shields.io/badge/GitHub-100000?style=for-the-badge&logo=github&logoColor=white)](https://github.com/xvierdev) | [![LinkedIn](https://img.shields.io/badge/LinkedIn-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/in/xvierbr) |
