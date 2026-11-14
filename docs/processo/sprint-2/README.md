# API 5º Semestre ADS - Equipe SUL

# Documentação - Sprint 2

<p align="center">
  | <a href ="#objetivo"> Objetivo</a>  |
  <a href ="#us"> User Stories</a>  |
  <a href ="#epicos">Épicos</a>  |
  <a href ="#dor">DoR</a>  |
  <a href ="#dod">DoD</a>  |
</p>

## 🎯 Objetivo <a id="objetivo"></a>

### Meta da Sprint

Entregar o fluxo inicial de captação e orçamento, permitindo que a equipe registre solicitações de tradução, elabore orçamentos e os envie automaticamente ao cliente para aprovação ou recusa por meio de um link recebido por e-mail.

| Meta de negócio | User Stories relacionadas | Situação |
| :-------------- | :------------------------ | :------- |
| Gestão de solicitações | US-07 | ✔️ Concluída |
| Criação de orçamentos | US-08 e US-09 | ✔️ Concluída |
| Envio automático de orçamentos e aprovação pelo cliente por e-mail | US-08 | ✔️ Concluída |

As metas representam os resultados de negócio esperados para a Sprint 2. As User Stories detalham as necessidades dos usuários e as `T-US` representam os desdobramentos técnicos necessários para implementar essas histórias.

## 📋 User Stories <a id="us"></a>

| Código | Prioridade | User Story | Story Points | Épico | Sprint | Status |
| :----: | :--------: | :--------- | :----------: | :---: | :----: | :----: |
| US-07 | Alta | Como Atendente, quero cadastrar uma solicitação de serviço vinculando um cliente (novo ou existente), para registrar demandas da empresa. | 8 | E2/E6 | 1 e 2 | ✔️ |
| US-08 | Alta | Como Atendente, quero elaborar um orçamento com os itens de serviço necessários e enviá-lo por e-mail ao cliente com um link de aprovação, para que o cliente possa aprovar ou recusar de forma autônoma. | 8 | E2 | 1 e 2 | ✔️ |
| US-09 | Média | Como Atendente, quero editar um orçamento recusado pelo cliente e reenviar para aprovação, para viabilizar negociações sem precisar criar um novo orçamento do zero. | 8 | E2 | 2 | ✔️ |
| US-11 | Alta | Como Gestor de Projetos, quero que ao aprovar um orçamento uma Ordem de Serviço seja criada automaticamente, que eu possa definir suas etapas aplicando um modelo de workflow ou montando manualmente e que possa consultar seus detalhes e progresso, para acompanhar o planejamento e a execução da produção. | — | E3 | 2 | ⚒️ Em progresso |


## 🛠️ User Story Tasks

As tarefas abaixo representam desdobramentos técnicos das User Stories do Product Backlog. Os números originais foram preservados para manter a rastreabilidade com commits, issues e Pull Requests da Sprint 2.

| Código | Tipo | Tarefa | User Story relacionada | Story Points | Épico | Sprint | Status |
| :----: | :--: | :----- | :--------------------: | :----------: | :---: | :----: | :----: |
| T-US-09 | Backend | Como Atendente, quero registrar uma solicitação de tradução no sistema vinculando o cliente, a categoria do projeto e anexando os arquivos base, para estruturar e armazenar as demandas que chegam por e-mail ou telefone. | US-07 | 3 | E2/E6 | 2 | ✔️ |
| T-US-10 | Frontend | Como Atendente, quero acessar uma tela de solicitações para selecionar um cliente, informar a categoria, o par de idiomas, a descrição e anexar arquivos (opcional), para registrar a demanda de tradução de forma intuitiva. | US-07 | 3 | E2 | 2 | ✔️ |
| T-US-11 | Backend | Como Atendente, quero cadastrar no sistema um orçamento vinculado a uma solicitação, para que a gestão dos custos do serviço. | US-08 | 5 | E2 | 2 | ✔️ |
| T-US-12 | Frontend | Como Atendente, quero preencher a tela de orçamento com os valores da tradução para gerar a proposta do cliente. | US-08 | 5 | E2 | 2 | ✔️ |
| T-US-13 | Backend | Como Atendente, quero que o sistema dispare o orçamento finalizado para o e-mail do cliente automaticamente, para agilizar o envio da proposta comercial evitando o trabalho manual. | US-08 | 3 | E2 | 2 | ✔️ |
| T-US-14 | Backend | Como Cliente, quero acessar a proposta através de um link seguro para consultar os valores e registrar minha aprovação ou rejeição de forma rápida. | US-08 | 3 | E2 | 2 | ✔️ |
| T-US-15 | Frontend | Como Cliente, quero abrir a página web através do link do e-mail para visualizar os itens e aprovar/recusar o orçamento com um clique. | US-08 | 2 | E2 | 2 | ✔️ |
| T-US-16 | Backend | Como Atendente/Gestor de Projeto, quero que o sistema crie automaticamente a Ordem de Serviço (OS) assim que o orçamento for aprovado, para iniciar a fase operacional. | US-11 | 3 | E3 | 2 | ✔️ |
| T-US-17 |  | Como Gestor de Projeto, quero consultar e filtrar as Ordens de Serviço cadastradas para acompanhar o status e o progresso dos projetos. | US-11 | 3 | E3 | 2 | ✔️ |
| T-US-18 | — | Como Gestor de Projeto, quero visualizar os detalhes da OS gerada para acompanhar o andamento do projeto. | US-11 | 3 | E3 | 2 | ✔️ |


## 🏆 Épicos <a id="epicos"></a>

| Código | Nome | Justificativa |
|--------|------|---------------|
| **E2** | Captação e Orçamento | Abrange a entrada inicial do fluxo: registro de solicitações do cliente, criação do orçamento pelo atendente, geração de arquivo de orçamento e envio automático ao cliente com link de aprovação por token — sem necessidade de login do cliente. |
| **E3** | Ordem de Serviço e Workflow | Converte orçamentos aprovados em Ordens de Serviço, permitindo a definição de workflows do serviço (tradução, revisão, formatação) e a alocação de recursos em cada etapa. Inclui cadastro de modelos de workflow reutilizáveis. |
| **E6** | Admin & Configurações | Base cadastral e estrutural de todo o sistema: autenticação, controle de acesso por perfil (RBAC), cadastros de clientes, recursos, serviços, idiomas, tabelas de preço, modelos de workflow, templates de e-mail e configurações da empresa. Pré-requisito de todos os demais épicos. Logs de auditoria armazenados no MongoDB. |

## Sobre as entregas

O Product Backlog apresenta as User Stories de negócio consolidadas. As User Stories registradas nas documentações das sprints são mantidas como histórico, pois seus códigos podem estar relacionados a commits e Pull Requests. Portanto, a consolidação abaixo não renomeia nem remove códigos históricos: ela apenas estabelece a correspondência entre a entrega de negócio e suas implementações ou desdobramentos anteriores.

| User Story consolidada | Códigos históricos relacionados | Escopo consolidado |
| :--------------------: | :-----------------------------: | :----------------- |
| US-07 | Sprint 1: US-007; Sprint 2: T-US-07, T-US-09 e T-US-10 | Registrar uma solicitação de tradução, vinculando o cliente, a categoria, o par de idiomas, a descrição e os arquivos necessários. |
| US-08 e US-09 | Sprint 1: US-008; Sprint 2: T-US-08, US-11, T-US-12, T-US-13, T-US-14 e T-US-15 | Elaborar o orçamento, informar os valores, enviá-lo ao cliente e permitir a consulta e a aprovação ou recusa por link seguro. |
| US-11 | Sprint 2: T-US-16, T-US-17 e T-US-18 | Criar a Ordem de Serviço após a aprovação do orçamento e permitir seu acompanhamento pelo Gestor de Projetos. |

---

## 🏅 DoR - Definition of Ready <a id="dor"></a>

|             Critério             | Descrição                                                                                         |
| :------------------------------: | ------------------------------------------------------------------------------------------------- |
| **Formato Centrado no Usuário** | A história inicia explicitamente no formato: **"Como `[persona]`, quero `[ação única]` para que `[valor gerado]`"**. |
| **Responsabilidade Única (SRP)** | A US resolve um único problema de um único domínio. Se houver a palavra **"E"** na ação principal (ex.: cadastrar e notificar), deve ser fatiada. |
| **Tamanho Controlado (Small)** | A estimativa não deve ultrapassar **8 Story Points**. Indicado estimar em até **5 Story Points**, para que não fique maçante para o desenvolvedor. |
| **Escopo Arquitetural** | Indica qual microsserviço e qual banco de dados será alterado. |
| **Contratos e Dependências** | Se for backend, o payload esperado (**JSON de request/response**) está na descrição da US. Se for frontend, a rota da API consumida está clara. Qualquer dependência deve ser listada. |
| **Critérios de Aceitação (CA)** | Possui uma lista numerada (**CA.1, CA.2...**) objetiva do que precisa ser entregue (campos obrigatórios, cálculos, retornos da API etc.). |
| **Regras de Negócio (RN)** | Lista explícita (**RN.1, RN.2...**) das restrições, limites e regras do sistema aplicáveis àquela US. |
| **Cenários de Teste (BDD)** | Possui os testes de aceitação escritos no formato **Dado / Quando / Então** (Gherkin), cobrindo o fluxo principal e os principais erros. |
| **Apoio Visual / Técnico** | Quando aplicável, protótipos, mockups, diagramas, fluxos ou demais artefatos técnicos estão anexados à descrição da US. |


## 🏅 DoD - Definition of Done <a id="dod"></a>

|                 Critério                 | Descrição                                                                            |
| :--------------------------------------: | ------------------------------------------------------------------------------------ |
|     Critérios de Aceitação atendidos     | Todos os critérios e regras de negócio da User Story foram atendidos.                 |
|        Testes manuais realizados         | Onde aplicável, os dados são corretamente armazenados e recuperáveis.     |
|             Código revisado              | O código foi revisado por pelo menos um colega de equipe.                            |
|  Integração com outras partes testadas   | As interfaces entre Frontend e Backend foram validadas.                              |
| Testes automatizados (se aplicável) | A funcionalidade não quebra a aplicação e passa nos testes automatizados existentes. |
|             Validação do PO              | O Product Owner validou a entrega com base nos critérios definidos.                  |
