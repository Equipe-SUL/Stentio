# API 5º Semestre ADS - Equipe SUL

# Documentação - Sprint 1

<p align="center">
  | <a href ="#objetivo"> Objetivo</a>  |
  <a href ="#us"> User Stories</a>  |
  <a href ="#epicos">Épicos</a>  |
  <a href ="#dor">DoR</a>  |
  <a href ="#dod">DoD</a>  |
</p>

## 🎯 Objetivo <a id="objetivo"></a>
Configurar a arquitetura base do sistema, automação de e-mails, cadastro de recursos, idiomas, preços e a captação inicial de solicitações com a criação de orçamentos.

## 📋 User Stories <a id="us"></a>

| Código | Prioridade | User Story                                                                                                                                                                                                                         | Story Points | Épico | Sprint | Status |
| :----: | :--------: | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | :----------: | :---: | :----: | :----: |
| US-01 |    Alta    | Como Administrador, quero que cada funcionário acesse o sistema com login e senha e tenha permissões baseadas em seu perfil, para proteger os dados e delimitar as ações disponíveis a cada funcionário.                                 |      8       |  E6   |   1    |   ✔️   |
| US-02 |    Alta    | Como Administrador, quero cadastrar os dados da empresa e configurar o servidor SMTP de envio de e-mails, para que as informações da empresa apareçam nos documentos gerados e os disparos de e-mail.       |      5       |  E6   |   1    |   ✔️   |
| US-03 |    Alta    | Como Administrador, quero cadastrar tipos de serviço, categorias de projeto e idiomas disponíveis, para que sirvam de base nos orçamentos, ordens de serviço e cadastros de recursos em todo o sistema.                            |      5       |  E6   |   1    |   ✔️   |
| US-04 |    Alta    | Como Administrador, quero cadastrar recursos associando seus pares de idiomas e valores individuais, para viabilizar a alocação e o cálculo de custo nas ordens de serviço.                                                        |      5       |  E6   |   1    |   ✔️   |
| US-05 |   Média    | Como Administrador, quero cadastrar tabelas de preço associando tipo de serviço, par de idiomas e valor unitário, para que o sistema sugira automaticamente os valores ao montar um orçamento.                                     |      8       |  E6   |   1    |   ⚒️   |
| US-06 |   Média    | Como Administrador, quero cadastrar e gerenciar templates de e-mail com placeholders dinâmicos, para padronizar e automatizar as comunicações com clientes e recursos sem depender de alterações de código.                        |      3       |  E6   |   1    |   🔲   |
| US-07 |    Alta    | Como Atendente, quero cadastrar uma solicitação de serviço vinculando um cliente (novo ou existente), para registrar demandas da empresa.       |      8       | E2/E6 |   1    |   ⚒️   |
| US-08 |    Alta    | Como Atendente, quero elaborar um orçamento com os itens de serviço necessários e enviá-lo por e-mail ao cliente com um link de aprovação, para que o cliente possa aprovar ou recusar de forma autônoma. |      8       |  E2   |   1    |   🔲   |

## 🏆 Épicos <a id="epicos"></a>

| Código | Nome | Justificativa |
|--------|------|---------------|
| **E2** | Captação e Orçamento | Abrange a entrada inicial do fluxo: registro de solicitações do cliente, criação do orçamento pelo atendente, geração de arquivo de orçamento e envio automático ao cliente com link de aprovação por token — sem necessidade de login do cliente. |
| **E6** | Admin & Configurações | Base cadastral e estrutural de todo o sistema: autenticação, controle de acesso por perfil/role, cadastros de clientes, recursos, serviços, idiomas, tabelas de preço e configurações da empresa. Pré-requisito de todos os demais épicos. |

## 🏅 DoR - Definition of Ready <a id="dor"></a>

|             Critério             | Descrição                                                                                         |
| :------------------------------: | ------------------------------------------------------------------------------------------------- |
|       Clareza na Descrição       | A User Story está escrita no formato “Como [persona], quero [ação] para que [objetivo]”           |
| Critérios de Aceitação Definidos | A história possui critérios objetivos que indicam o que é necessário para considerá-la concluída. |
|           Independente           | A história pode ser implementada sem depender de outra tarefa da mesma Sprint.                    |
| Cenários de Teste Especificados  | A história tem pelo menos 1 cenário de teste estruturado (Dado, Quando, Então).                   |
|    Compreensão Compartilhada     | Toda a equipe (incluindo PO e devs) compreende o propósito da história.                           |
|            Estímável             | A história foi pontuada no Planning Poker ou tem uma estimativa clara.                            |
|       Documentos de Apoio        | Se necessário, mockups, fluxos ou modelos de dados estão anexados ou referenciados.               |


## 🏅 DoD - Definition of Done <a id="dod"></a>

|                 Critério                 | Descrição                                                                            |
| :--------------------------------------: | ------------------------------------------------------------------------------------ |
|     Critérios de Aceitação atendidos     | Todos os critérios e regras de negócio da User Story foram atendidos.                 |
|        Testes manuais realizados         | Onde aplicável, os dados são corretamente armazenados e recuperáveis.     |
|             Código revisado              | O código foi revisado por pelo menos um colega de equipe.                            |
|  Integração com outras partes testadas   | As interfaces entre Frontend e Backend foram validadas.                              |
| Testes automatizados (se aplicável) | A funcionalidade não quebra a aplicação e passa nos testes automatizados existentes. |
|             Validação do PO              | O Product Owner validou a entrega com base nos critérios definidos.                  |
