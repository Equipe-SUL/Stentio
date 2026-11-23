# API 5º Semestre ADS - Equipe SUL

# Documentação - Sprint 3

<p align="center">
  | <a href="#objetivo"> Objetivo</a> |
  <a href="#us"> User Stories</a> |
  <a href="#tasks"> Tasks</a> |
  <a href="#epicos"> Épicos</a> |
  <a href="#dor"> DoR</a> |
  <a href="#dod"> DoD</a> |
</p>

## 🎯 Objetivo <a id="objetivo"></a>

Entregar o fluxo operacional completo após a aprovação do orçamento: planejamento e workflow das Ordens de Serviço, alocação e execução pelas etapas, atuação dos recursos em portal próprio, entrega e revisão final dos arquivos, auditoria das alterações, dashboard operacional e chatbot assistivo.

### Meta da Sprint

| Meta de negócio | User Stories relacionadas | Situação |
| :-------------- | :------------------------ | :------- |
| Planejar Ordens de Serviço e alocar recursos | US-011, US-012 | ✔️ Concluída |
| Executar, entregar e revisar etapas | US-013, US-014, US-015 | ✔️ Concluída |
| Disponibilizar acesso público e portal do recurso | US-022, US-023, US-024 | ✔️ Concluída |
| Registrar alterações e acompanhar a operação | US-019, US-020 | ✔️ Concluída |
| Disponibilizar assistência operacional por IA | US-021 | ✔️ Concluída |

## 📋 User Stories <a id="us"></a>

| Código | Prioridade | User Story | Story Points | Épico | Sprint | Status |
| :----: | :--------: | :--------- | :----------: | :---: | :----: | :----: |
| US-011 | Alta | Como Gestor de Projetos, quero que ao aprovar um orçamento uma Ordem de Serviço seja criada automaticamente, que eu possa definir suas etapas aplicando um modelo de workflow ou montando manualmente e que possa consultar seus detalhes e progresso, para acompanhar o planejamento e a execução da produção. | 8 | E3 | 2 e 3 | ✔️ Concluída |
| US-012 | Alta | Como Gestor de Projetos, quero alocar um recurso a cada etapa da OS verificando compatibilidade de idiomas e tipo de serviço, para garantir que cada etapa seja atribuída ao profissional adequado antes de iniciar a execução. | 8 | E3 | 3 | ✔️ Concluída |
| US-013 | Alta | Como Gestor de Projetos, quero disparar uma etapa para todos os recursos elegíveis, para que ela seja assumida por quem estiver disponível. | 8 | E4 | 3 | ✔️ Concluída |
| US-014 | Média | Como Gestor de Projetos, quero que ao registrar a entrega de uma etapa o sistema avance automaticamente para a próxima etapa do workflow, notificando o recurso alocado. | 5 | E4 | 3 | ✔️ Concluída |
| US-015 | Alta | Como Gestor de Projetos, quero revisar todos os arquivos entregues nas etapas concluídas, validar o pacote final e enviar os documentos traduzidos ao cliente por e-mail, para encerrar formalmente a Ordem de Serviço. | 8 | E4 | 3 | ✔️ Concluída |
| US-019 | Baixa | Como Administrador, quero visualizar um log de auditoria com todas as alterações de status realizadas em orçamentos, OS e etapas, para fins de rastreabilidade interna. | 5 | E6 | 3 | ✔️ Concluída |
| US-020 | Média | Como Gestor de Projetos, quero visualizar um dashboard com contadores e listas dinâmicas das principais pendências operacionais, para gerir a operação a partir de uma única tela. | 8 | E1 | 3 | ✔️ Concluída |
| US-021 | Média | Como usuário autenticado, quero conversar com um chatbot, para tirar dúvidas operacionais e consultar dados permitidos pelo meu perfil. | 8 | E1 | 3 | ✔️ Concluída |
| US-022 | Alta | Como cliente, quero enviar uma solicitação de serviço de tradução sem criar conta, para cadastrar uma proposta de atendimento na plataforma. | 5 | E2 | 3 | ✔️ Concluída |
| US-023 | Alta | Como recurso, quero me cadastrar e acessar um portal próprio com login, para acompanhar as etapas destinadas a mim. | 8 | E4 | 3 | ✔️ Concluída |
| US-024 | Média | Como Gestor de Projetos, quero analisar as certificações enviadas por recurso para cada par de idiomas e aprová-las ou rejeitá-las, para que somente profissionais comprovadamente capazes recebam etapas daquele par. | 5 | E6 | 3 | ✔️ Concluída |

**Resultado consolidado:** 11 User Stories concluídas. US-011, US-012, US-020 e US-021 possuem Story Points registrados no GitHub Projects.

## 🛠️ User Story Tasks <a id="tasks"></a>

As tarefas abaixo correspondem às issues fechadas e às subtarefas concluídas no GitHub Projects. O status `✔️` indica que a issue foi encerrada como concluída.

### US-011 - Planejamento de etapas da Ordem de Serviço

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US11-T1 | Backend | Criar a entidade de etapas da OS. | US-011 | ✔️ |
| US11-T2 | Backend | Criar a entidade e o cadastro de modelo de workflow. | US-011 | ✔️ |
| US11-T3 | Backend | Adicionar o status `EM_PLANEJAMENTO` na OS. | US-011 | ✔️ |
| US11-T4 | Backend | Implementar montagem manual da sequência de etapas. | US-011 | ✔️ |
| US11-T5 | Backend | Aplicar modelo de workflow na OS. | US-011 | ✔️ |
| US11-T6 | Backend | Implementar a listagem das etapas da OS. | US-011 | ✔️ |
| US11-T7 | Backend | Bloquear alteração da sequência após alocação. | US-011 | ✔️ |
| US11-T8 | Backend | Cobrir etapas e modelos de workflow com testes. | US-011 | ✔️ |
| US11-T9 | Frontend | Implementar tela de etapas da OS. | US-011 | ✔️ |
| US11-T10 | Frontend | Permitir adicionar, remover e reordenar etapas manualmente. | US-011 | ✔️ |
| US11-T11 | Frontend | Permitir carregar modelo de workflow na OS. | US-011 | ✔️ |
| US11-T12 | Frontend | Permitir salvar modelo de workflow. | US-011 | ✔️ |
| US11-T13 | Frontend | Exibir progresso e detalhes das etapas da OS. | US-011 | ✔️ |
| US11-T14 | Frontend | Validar a integração da tela com a API. | US-011 | ✔️ |

### US-012 - Alocação de recursos às etapas

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US12-T1 | Backend | Adicionar campos de alocação na etapa. | US-012 | ✔️ |
| US12-T2 | Backend | Expor recursos disponíveis para uma etapa. | US-012 | ✔️ |
| US12-T3 | Backend | Implementar o endpoint de alocação de recurso. | US-012 | ✔️ |
| US12-T4 | Backend | Bloquear troca de recurso em etapa iniciada. | US-012 | ✔️ |
| US12-T5 | Backend | Registrar auditoria da alocação da etapa. | US-012 | ✔️ |
| US12-T6 | Backend | Cobrir alocação e realocação com testes. | US-012 | ✔️ |
| US12-T7 | Frontend | Criar tipos e integração da API de alocação. | US-012 | ✔️ |
| US12-T8 | Frontend | Adicionar ação de alocar recurso na etapa. | US-012 | ✔️ |
| US12-T9 | Frontend | Implementar modal de alocação. | US-012 | ✔️ |
| US12-T10 | Frontend | Bloquear realocação em etapa iniciada. | US-012 | ✔️ |

### US-013 - Disparo, aceite e entrega das etapas

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US13-T1 | Backend | Criar novos estados da etapa e campos de aceite e entrega. | US-013 | ✔️ |
| US13-T2 | Backend | Implementar disparo da etapa para recursos elegíveis. | US-013 | ✔️ |
| US13-T3 | Backend | Enviar convite por e-mail aos recursos elegíveis. | US-013 | ✔️ |
| US13-T4 | Backend | Implementar aceite atômico da etapa pelo recurso. | US-013 | ✔️ |
| US13-T5 | Backend | Implementar aceite e recusa da alocação direta. | US-013 | ✔️ |
| US13-T6 | Backend | Implementar entrega e armazenamento do arquivo. | US-013 | ✔️ |
| US13-T7 | Backend | Consultar etapas disponíveis e atribuídas ao recurso. | US-013 | ✔️ |
| US13-T8 | Backend | Registrar histórico e auditoria de disparo, aceite e entrega. | US-013 | ✔️ |
| US13-T9 | Backend | Cobrir o fluxo de disparo, aceite e entrega com testes. | US-013 | ✔️ |
| US13-T10 | Frontend | Adicionar botão de disparo para recursos elegíveis. | US-013 | ✔️ |
| US13-T11 | Frontend | Exibir etapas disponíveis e permitir aceite no portal. | US-013 | ✔️ |
| US13-T12 | Frontend | Permitir upload da entrega no portal. | US-013 | ✔️ |
| US13-T13 | Frontend | Exibir novos estados da etapa nas telas de OS. | US-013 | ✔️ |

### US-014 - Avanço automático do workflow

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US14-T0 | Backend | Corrigir a ordem das etapas e implementar a regra de etapa liberada. | US-014 | ✔️ |
| US14-T1 | Backend | Criar novos status da OS e evento de avanço. | US-014 | ✔️ |
| US14-T2 | Backend | Implementar o serviço de avanço da sequência. | US-014 | ✔️ |
| US14-T3 | Backend | Rotear a próxima etapa com convite automático ou recurso alocado. | US-014 | ✔️ |
| US14-T4 | Backend | Notificar o avanço por outbox e email. | US-014 | ✔️ |
| US14-T5 | Backend | Garantir idempotência e histórico do avanço. | US-014 | ✔️ |
| US14-T6 | Backend | Retornar o avanço e os novos status nos endpoints de consulta. | US-014 | ✔️ |
| US14-T7 | Backend | Cobrir o avanço automático com testes. | US-014 | ✔️ |
| US14-T8 | Frontend | Exibir etapa liberada, convite automático e retorno do avanço no portal. | US-014 | ✔️ |
| US14-T10 | Frontend | Exibir novos status da OS nas telas de staff. | US-014 | ✔️ |

### US-015 - Revisão e entrega final

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US15-T1 | Backend | Criar status encerrada, evento reabertura e campos da entrega final. | US-015 | ✔️ |
| US15-T2 | Backend | Expor revisão final e pacote da OS. | US-015 | ✔️ |
| US15-T3 | Backend | Disponibilizar download dos arquivos entregues. | US-015 | ✔️ |
| US15-T4 | Backend | Aprovar a entrega final e gravar a outbox. | US-015 | ✔️ |
| US15-T5 | Backend | Publicar a entrega final e encerrar a OS após ACK. | US-015 | ✔️ |
| US15-T6 | Backend | Consumir a entrega final no email-service e enviar o anexo. | US-015 | ✔️ |
| US15-T7 | Backend | Reprovar a entrega final e reabrir a última etapa. | US-015 | ✔️ |
| US15-T8 | Backend | Retornar a OS para revisão após reentrega. | US-015 | ✔️ |
| US15-T9 | Backend | Atualizar contagens do dashboard para os novos status. | US-015 | ✔️ |
| US15-T10 | Backend | Cobrir revisão, aprovação e reprovação com testes. | US-015 | ✔️ |
| US15-T11 | Frontend | Implementar tela de revisão final da OS. | US-015 | ✔️ |
| US15-T12 | Frontend | Implementar ações de aprovar e reprovar a entrega. | US-015 | ✔️ |
| US15-T13 | Frontend | Exibir status encerrada e motivo da revisão. | US-015 | ✔️ |

### US-019 - Auditoria

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US19-T1 | Backend | Criar entidade imutável de auditoria. | US-019 | ✔️ |
| US19-T2 | Backend | Publicar evento e registrar mudanças de status por listener. | US-019 | ✔️ |
| US19-T3 | Backend | Auditar transições de orçamento. | US-019 | ✔️ |
| US19-T4 | Backend | Auditar transições de OS e etapas. | US-019 | ✔️ |
| US19-T5 | Backend | Implementar consulta paginada e filtrada de logs. | US-019 | ✔️ |
| US19-T6 | Backend | Cobrir auditoria com testes. | US-019 | ✔️ |
| US19-T7 | Frontend | Criar tipos e service de auditoria. | US-019 | ✔️ |
| US19-T8 | Frontend | Implementar tela de logs de auditoria. | US-019 | ✔️ |
| US19-T9 | Frontend | Implementar filtros combináveis da auditoria. | US-019 | ✔️ |

### US-020 - Dashboard operacional

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US20-T1 | Backend | Implementar endpoint de resumo do dashboard. | US-020 | ✔️ |
| US20-T2 | Backend | Contar etapas atrasadas. | US-020 | ✔️ |
| US20-T3 | Backend | Implementar endpoint de pendências. | US-020 | ✔️ |
| US20-T4 | Backend | Aplicar permissões e filtros por perfil. | US-020 | ✔️ |
| US20-T5 | Backend | Cobrir os endpoints do dashboard com testes. | US-020 | ✔️ |
| US20-T6 | Frontend | Adicionar rota e permissões do dashboard. | US-020 | ✔️ |
| US20-T7 | Frontend | Criar tipos e service da API do dashboard. | US-020 | ✔️ |
| US20-T8 | Frontend | Implementar cards de métricas. | US-020 | ✔️ |
| US20-T9 | Frontend | Implementar lista de pendências e filtros. | US-020 | ✔️ |
| US20-T10 | Frontend | Implementar atualização manual e auto-refresh. | US-020 | ✔️ |

### US-021 - Chatbot

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US21-T1 | Backend | Criar a estrutura do serviço de IA e integração com LLM. | US-021 | ✔️ |
| US21-T2 | Backend | Implementar base de conhecimento por RAG. | US-021 | ✔️ |
| US21-T3 | Backend | Consultar orçamento e OS com tool calling. | US-021 | ✔️ |
| US21-T4 | Backend | Implementar endpoint de mensagens. | US-021 | ✔️ |
| US21-T5 | Backend | Persistir e consultar histórico da conversa. | US-021 | ✔️ |
| US21-T6 | Backend | Aplicar permissões e respostas amigáveis por perfil. | US-021 | ✔️ |
| US21-T7 | Backend | Cobrir chatbot e RBAC com testes. | US-021 | ✔️ |
| US21-T8 | Frontend | Criar tipos e API do chatbot. | US-021 | ✔️ |
| US21-T9 | Frontend | Implementar atalho global para o chatbot. | US-021 | ✔️ |
| US21-T10 | Frontend | Implementar janela de conversa. | US-021 | ✔️ |
| US21-T11 | Frontend | Implementar botões de atalho nas respostas. | US-021 | ✔️ |
| US21-T12 | Frontend | Persistir e exibir histórico da sessão. | US-021 | ✔️ |

### US-022 - Solicitação pública

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US22-T1 | Backend | Liberar rotas públicas e limites de upload. | US-022 | ✔️ |
| US22-T2 | Backend | Expor opções públicas de solicitação. | US-022 | ✔️ |
| US22-T3 | Backend | Criar DTO público e validações da solicitação. | US-022 | ✔️ |
| US22-T4 | Backend | Resolver cliente existente ou criar por CPF/CNPJ. | US-022 | ✔️ |
| US22-T5 | Backend | Implementar criação pública da solicitação. | US-022 | ✔️ |
| US22-T6 | Backend | Cobrir solicitação pública com testes. | US-022 | ✔️ |
| US22-T7 | Frontend | Criar service público da solicitação. | US-022 | ✔️ |
| US22-T8 | Frontend | Implementar tela pública da solicitação. | US-022 | ✔️ |
| US22-T9 | Frontend | Implementar anexos, validações e retorno do envio. | US-022 | ✔️ |

### US-023 - Cadastro e portal do recurso

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US23-T1 | Backend | Criar papel recurso e claim userId no JWT. | US-023 | ✔️ |
| US23-T2 | Backend | Implementar cadastro público de usuário recurso. | US-023 | ✔️ |
| US23-T3 | Backend | Propagar papel recurso e usuarioId no Core. | US-023 | ✔️ |
| US23-T4 | Backend | Migrar recurso com usuarioId e verificação mínima. | US-023 | ✔️ |
| US23-T5 | Backend | Implementar auto-cadastro do perfil do recurso. | US-023 | ✔️ |
| US23-T6 | Backend | Implementar verificação mínima do recurso. | US-023 | ✔️ |
| US23-T7 | Backend | Isolar rotas de staff e listar etapas do recurso. | US-023 | ✔️ |
| US23-T8 | Backend | Cobrir o portal e o cadastro do recurso com testes. | US-023 | ✔️ |
| US23-T9 | Frontend | Implementar cadastro público do recurso. | US-023 | ✔️ |
| US23-T10 | Frontend | Configurar sessão, rotas e layout do portal. | US-023 | ✔️ |
| US23-T11 | Frontend | Implementar demandas do recurso e meu perfil. | US-023 | ✔️ |
| US23-T12 | Frontend | Implementar ação de verificar recurso. | US-023 | ✔️ |

### US-024 - Certificações por par de idiomas

| Código | Tipo | Tarefa | US relacionada | Status |
| :----: | :--- | :----- | :------------: | :----: |
| US24-T1 | Backend | Criar entidade e migration de certificações por par. | US-024 | ✔️ |
| US24-T2 | Backend | Implementar envio e consulta de certificações pelo recurso. | US-024 | ✔️ |
| US24-T3 | Backend | Implementar fila e ações de aprovar/rejeitar. | US-024 | ✔️ |
| US24-T4 | Backend | Exigir certificação aprovada na elegibilidade por par. | US-024 | ✔️ |
| US24-T5 | Backend | Restringir download do documento de certificação. | US-024 | ✔️ |
| US24-T6 | Backend | Notificar rejeição da certificação por e-mail. | US-024 | ✔️ |
| US24-T7 | Backend | Cobrir certificações com testes. | US-024 | ✔️ |
| US24-T8 | Frontend | Implementar minhas certificações no portal. | US-024 | ✔️ |
| US24-T9 | Frontend | Implementar análise das certificações pelo Gestor. | US-024 | ✔️ |

## 🏆 Épicos <a id="epicos"></a>

| Código | Nome | Justificativa |
| :----- | :--- | :------------ |
| **E1** | Dashboard & Monitoramento | Painel central de acompanhamento operacional e chatbot assistivo. |
| **E2** | Captação & Orçamento | Cobre o registro público de solicitações e o fluxo de orçamento já iniciado nas sprints anteriores. |
| **E3** | Ordem de Serviço & Workflow | Converte orçamento aprovado em OS, etapas e modelos de workflow reutilizáveis. |
| **E4** | Execução de Tradução | Operacionaliza o acesso do recurso, alocação, disparo, aceite, entrega e avanço das etapas. |
| **E6** | Admin & Configurações | Cobre RBAC, auditoria e análise de certificações administrativas. |

## 🏅 DoR - Definition of Ready <a id="dor"></a>

| Critério | Descrição |
| :------- | :-------- |
| Formato centrado no usuário | A história inicia no formato “Como `[persona]`, quero `[ação única]` para que `[valor gerado]`”. |
| Contratos e dependências | O payload de backend ou os endpoints consumidos pelo frontend estão claros. |
| Critérios de aceitação | Possui critérios objetivos numerados em `CA.1`, `CA.2` etc. |
| Regras de negócio | Possui regras explícitas em `RN.1`, `RN.2` etc. |
| Cenários de teste | Possui cenários principais e erros no formato Dado / Quando / Então. |
| Apoio técnico | Mockups, contratos, fluxos ou scripts técnicos estão disponíveis quando necessários. |

## 🏅 DoD - Definition of Done <a id="dod"></a>

| Critério | Descrição |
| :------- | :-------- |
| Critérios atendidos | Todos os critérios de aceitação e regras de negócio foram atendidos. |
| Integração validada | Frontend, backend e serviços auxiliares foram integrados e validados. |
| Testes automatizados | Os testes aplicáveis foram executados e passaram. |
| Código revisado | As entregas passaram por revisão no repositório e foram mergeadas. |
| Item concluído no board | A User Story e suas subtarefas estão na coluna Done do GitHub Projects. |
