# Manual Operacional do Sistema Stentio (TMS)

## 1. Visão Geral do Sistema
O **Stentio** é um Sistema de Gestão de Tradução (TMS - *Translation Management System*) voltado para o gerenciamento completo do ciclo de projetos de tradução, revisão e formatação. O fluxo principal abrange desde a captação da demanda do cliente e envio de orçamento com link público de aprovação até a abertura automática da Ordem de Serviço (OS), definição de etapas de workflow e alocação de profissionais (recursos).

---

## 2. Perfis de Acesso e Permissões (RBAC)
O acesso às funcionalidades e dados do sistema é controlado pelo perfil (*role*) vinculado ao usuário autenticado:

* **Administrador (`ADMIN`):** Possui acesso irrestrito a todos os módulos, configurações do sistema, catálogos, relatórios e cadastros. É o **único perfil com permissão para excluir clientes** do sistema.
* **Atendente (`ATENDENTE`):** Responsável pelo atendimento comercial. Pode cadastrar e editar clientes (sem permissão de exclusão), registrar solicitações de tradução, elaborar orçamentos, editar orçamentos recusados e disparar o envio de aprovação por e-mail para o cliente. Também pode consultar recursos cadastrados.
* **Gestor de Projetos (`GESTOR_PROJETO`):** Responsável pela operação e produção. Gerencia Ordens de Serviço, configura etapas de produção, cria e aplica modelos de workflow reutilizáveis, realiza a alocação de recursos nas etapas, administra tabelas de preço e acompanha solicitações e orçamentos.
* **Financeiro (`FINANCEIRO`):** Possui acesso aos catálogos de base (categorias de projeto, idiomas e tipos de serviço) e consulta de clientes e recursos. *(Nota: Os módulos de geração de faturas de venda/compra e relatório de lucro por OS ainda não estão disponíveis na versão atual).*
* **Tradutor / Recurso (`TRADUTOR`):** Perfil operacional sem acesso a dados financeiros, valores de orçamentos ou faturamento da empresa.

---

## 3. Cadastros Base e Configurações (Administração)

### 3.1. Dados da Empresa e Catálogos
Disponível na área de **Configurações** (`/configs`):
* **Dados da Empresa:** Cadastro das informações institucionais e logotipo que aparecem nos orçamentos e documentos gerados.
* **Catálogos do Sistema:** Cadastro de **Tipos de Serviço** (ex.: Tradução, Revisão, Formatação), **Categorias de Projeto** (ex.: Tradução Técnica, Jurídica, Comercial), **Idiomas** disponíveis (com código ISO, ex.: `pt-BR`, `en`, `es`, `fr`) e **Unidades de Cobrança** (ex.: por Palavra ou por Página).
* **Tabelas de Preço:** Associação entre Tipo de Serviço, Par de Idiomas (origem e destino) e valor unitário para cálculo nos orçamentos. Gerenciado exclusivamente por `ADMIN` e `GESTOR_PROJETO`.

### 3.2. Clientes e Recursos (Profissionais)
* **Clientes (`/clientes`):** Cadastro de empresas ou pessoas físicas solicitantes, com validação de unicidade de CPF/CNPJ e dados do representante. Qualquer usuário autenticado pode cadastrar ou editar clientes, mas somente `ADMIN` pode excluí-los.
* **Recursos (`/recursos`):** Cadastro de tradutores, revisores e diagramadores, associando seus pares de idiomas atendidos, tipos de serviço e valores individuais de custo.
* **Importação em Lote (`/importacoes`):** Permite importar recursos e serviços via arquivo **CSV**, exibindo uma etapa de prévia e validação antes de confirmar a gravação e gerando um relatório final da importação.

---

## 4. Fluxo de Captação e Orçamentos

### 4.1. Registro de Solicitações (`/solicitacoes`)
1. O **Atendente** (ou `ADMIN` / `GESTOR_PROJETO`) registra uma nova solicitação vinculando um cliente já cadastrado (ou cadastrando um novo).
2. Define a categoria do projeto, o idioma de origem, o idioma de destino, a descrição da demanda e anexa os arquivos enviados pelo cliente.

### 4.2. Elaboração e Envio de Orçamento (`/orcamentos`)
1. A partir de uma solicitação, o **Atendente** monta o orçamento adicionando os itens por par de idiomas e os serviços necessários (ex.: Tradução + Revisão), informando quantidade, unidade de cobrança (`PALAVRA` ou `PAGINA`) e valor unitário.
2. O sistema calcula automaticamente os subtotais de cada serviço, o subtotal do item e o valor total do orçamento.
3. Ao acionar o envio para aprovação, o sistema dispara um e-mail automático ao cliente contendo os detalhes do orçamento e um **link público exclusivo (tokenizado)**.

### 4.3. Aprovação Pública pelo Cliente e Status do Orçamento
* **Sem necessidade de login:** O cliente acessa o link recebido por e-mail (`/orcamento`), visualiza os dados da empresa, o resumo da solicitação, os valores e pode **Aprovar** ou **Rejeitar** o orçamento diretamente.
* **Status do Orçamento:**
  * `EM_ELABORACAO`: Orçamento criado e em edição interna antes do envio.
  * `AGUARDANDO_APROVACAO`: Orçamento enviado por e-mail ao cliente, aguardando resposta no link público.
  * `APROVADO`: Cliente aceitou a proposta. **Ao aprovar, o sistema dispara automaticamente a criação da Ordem de Serviço (OS) correspondente.**
  * `REJEITADO`: Cliente recusou a proposta. O Atendente pode editar os valores/itens desse orçamento recusado e reenviá-lo para uma nova rodada de aprovação (controle por número de versão), sem precisar recriar do zero.

---

## 5. Ordens de Serviço (OS) e Workflow (`/ordens`)

### 5.1. Abertura e Etapas da OS
* Toda Ordem de Serviço é originada automaticamente após a aprovação de um orçamento pelo cliente.
* Na tela de detalhes da OS (`/ordens/[id]`), o **Gestor de Projetos** (ou `ADMIN`) define a sequência de etapas de produção (ex.: Etapa 1: Tradução -> Etapa 2: Revisão -> Etapa 3: Formatação).
* As etapas podem ser montadas manualmente ou carregadas a partir de **Modelos de Workflow** pré-cadastrados e reutilizáveis.

### 5.2. Alocação de Recursos
* Em cada etapa da Ordem de Serviço, o **Gestor de Projetos** seleciona e aloca um recurso (profissional).
* O sistema valida a **compatibilidade** entre o recurso selecionado, o par de idiomas exigido no item da OS e o tipo de serviço da etapa antes de confirmar a alocação.

---

## 6. Configuração de E-mail (Servidor SMTP)

A tela **SMTP** (`/smtp`, acessível por `ADMIN` em Configurações -> Serviço de Email) define o servidor utilizado para disparar e-mails do sistema (como o envio de orçamentos para aprovação).

### 6.1. Campos de Configuração
* **Host SMTP:** Endereço do servidor de e-mail (ex.: `smtp.gmail.com`).
* **Porta SMTP:** `587` (TLS), `465` (SSL) ou `25` (sem criptografia).
* **Usuário e Senha:** Credenciais de autenticação do e-mail remetente.
* **Criptografia:** `TLS`, `SSL` ou `NONE`.
* **E-mail do Remetente (From):** Endereço exibido como remetente (no Gmail, deve ser idêntico ao Usuário).
* **Nome de Exibição:** Nome amigável exibido ao lado do e-mail (ex.: `Stentio Notificações`).
* **E-mail de Teste:** Endereço de destino para validar as configurações antes ou depois de salvar.

### 6.2. Passo a Passo para Configurar com Gmail
1. Ative a **Verificação em duas etapas** na conta Google (`myaccount.google.com/security`).
2. Acesse **Senhas de app** (`myaccount.google.com/apppasswords`), crie uma senha para `Stentio SMTP` e copie o código de **16 caracteres** (sem espaços).
3. Preencha na tela SMTP do Stentio:
   * **Host SMTP:** `smtp.gmail.com` | **Porta:** `587` | **Criptografia:** `TLS`
   * **Usuário** e **E-mail do Remetente (From):** seu endereço Gmail completo.
   * **Senha:** os 16 caracteres da App Password.
4. Clique em **Salvar Configurações** e utilize o botão **Disparar Teste Agora** para confirmar o recebimento (verifique também a caixa de spam). *Nota: Disparar o teste não salva alterações automaticamente; é obrigatório clicar em Salvar Configurações.*

### 6.3. Solução de Erros Comuns no Envio de E-mail
* **`535 5.7.8 Username and Password not accepted`:** Senha de aplicativo incorreta, digitada com espaços ou e-mail do usuário inválido.
* **`534 ... Application-specific password required`:** A conta Gmail está sem a verificação em duas etapas ativada ou tentou usar a senha normal da conta em vez da App Password.
* **`Connection refused` ou `Timeout waiting for connection`:** Host/porta incorretos, serviço de e-mail indisponível ou bloqueio de firewall na porta configurada.

---

## 7. Funcionalidades em Desenvolvimento (Ainda não disponíveis)
Caso o usuário pergunte sobre funcionalidades futuras ainda não implementadas no sistema, informe que ainda não estão disponíveis na versão atual:
* Portal externo para o tradutor aceitar/rejeitar tarefas e fazer upload do arquivo traduzido diretamente por link.
* Envio automatizado do pacote final traduzido ao cliente e encerramento formal da OS pela plataforma.
* Geração de faturas de venda (cobrança ao cliente), faturas de compra (repasse aos recursos) e relatório de lucro por OS.
* Cadastro dinâmico de templates de e-mail com placeholders pela interface.
* Log de auditoria visual de alterações de status.