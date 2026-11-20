# Plano de conformidade do chatbot

## Objetivo

Registrar o estado atual do chatbot em relação à arquitetura, às regras de negócio e aos critérios de aceite descritos no README do serviço. Este documento separa o que já existe, o que está parcialmente atendido e o que diverge.

## O que já está implementado

- **Serviço e integração:** FastAPI registra as rotas do chatbot. O Docker Compose configura `chatbot-service` na rede da aplicação, conecta PostgreSQL e MongoDB e publica a porta `8000`. O Nginx encaminha `/api/v1/ia/chatbot` ao serviço.
- **Envio de mensagens:** `POST /api/v1/ia/chatbot/mensagens` autentica o usuário, lê mensagens da conversa, persiste pergunta e resposta no MongoDB e usa Groq com function calling.
- **Histórico:** `GET /api/v1/ia/chatbot/historico` consulta mensagens do MongoDB filtrando por usuário e conversa; sem `conversaId`, escolhe a conversa mais recente do usuário.
- **Consultas no PostgreSQL:** existem tools para buscar status de OS por UUID ou cliente, orçamento por UUID ou cliente e valores de orçamentos aprovados.
- **RBAC financeiro nas tools:** os perfis `TRADUTOR` e `RECURSO` não recebem valores de orçamento nem totais consolidados. Os perfis são normalizados antes da decisão.
- **Atalhos:** o schema e o frontend reconhecem `OS`, `ORCAMENTO` e `CLIENTE`; as tools atuais geram atalhos de OS e orçamento.
- **RAG:** o serviço carrega o manual e seleciona até três seções por sobreposição de palavras, com peso maior para termos presentes no título. O arquivo real `manual_operacional_do_sistema_stentio.md` é localizado pelo carregador.
- **Autenticação híbrida:** aceita token no cookie `stentio_token` ou no header `Authorization` e valida JWT, incluindo expiração. Também há fallback para role no JWT quando o usuário não está no banco consultado pelo chatbot.
- **Testes automatizados:** a suíte pytest tem 51 testes e passou na última execução. Ela cobre rotas, histórico, autenticação, tools, RBAC, RAG, schemas e encerramento da sessão de banco.

## Divergências e riscos verificados

### 1. Contexto do chat usa as mensagens mais antigas

Em [chatbot.py](./chatbot/app/api/chatbot.py), a consulta do contexto ordena por `criada_em` crescente e aplica `limit(10)`. Com mais de dez mensagens, isso seleciona as dez primeiras da conversa, não as dez mais recentes. O critério de memória recente fica parcialmente atendido.

**Ajuste planejado:** buscar as dez mais recentes em ordem decrescente e inverter o resultado antes de enviá-lo ao modelo, para preservar a ordem cronológica no prompt.

### 2. Atalho de cliente não é produzido pelo backend

O tipo `CLIENTE` é permitido pelo schema e o frontend sabe direcioná-lo, mas as tools em [os_tools.py](./chatbot/app/tools/os_tools.py) só criam atalhos `OS` e `ORCAMENTO`. Assim, a promessa de botões de cliente não está atendida.

**Ajuste planejado:** definir de quais respostas deve surgir o atalho de cliente, gerar o `Atalho(tipo="CLIENTE", ...)` com o identificador correto e validar a rota de destino. Não criar o atalho usando somente nome da empresa como se fosse ID.

### 3. “Faturamento deste mês” não é calculado

O schema da tool em [chatbot_ia.py](./chatbot/app/services/chatbot_ia.py) anuncia faturamento mensal ou consolidado. A implementação em [os_tools.py](./chatbot/app/tools/os_tools.py) soma valores de todos os orçamentos com status `APROVADO`, sem filtro de data. Isso não responde “deste mês” e não representa necessariamente faturamento realizado.

O manual informa que os módulos de faturamento/faturas ainda não estão disponíveis. Portanto, é necessário escolher e documentar uma definição de negócio:

- calcular o **total de orçamentos aprovados no mês**, usando uma data de aprovação confiável; ou
- deixar claro que o sistema só informa **total de orçamentos aprovados**, sem afirmar que é faturamento mensal/realizado.

Até a definição, a descrição da tool e as respostas não devem chamar o total histórico de “faturamento do mês”.

### 4. Atalho literal permite ignorar a validação JWT

Em [security.py](./chatbot/app/core/security.py), os textos literais `ADMIN` e `TRADUTOR` são aceitos como perfis de teste antes da validação JWT. Isso facilita o roteiro do Swagger, mas também permite autenticar sem JWT se o serviço estiver acessível. Contradiz a exigência de segurança estrita e não deve ficar ativo em produção.

**Ajuste planejado:** remover o atalho ou habilitá-lo somente por uma configuração explícita de desenvolvimento, desativada por padrão e proibida em produção. Para testes automatizados, usar tokens de teste assinados ou overrides de dependência. Revisar também as instruções do Swagger.

### 5. CORS documentado como configurável, mas fixo no código

O README sugere configurar `APP_CORS_ALLOWED_ORIGINS`, mas [main.py](./chatbot/app/main.py) usa uma lista fixa e não lê essa variável. A configuração `.env` não altera as origens permitidas pelo chatbot.

**Ajuste planejado:** carregar a lista da configuração, separar e validar as origens, mantendo valores locais seguros como padrão. Testar origem permitida, origem negada e credenciais.

### 6. RBAC depende parcialmente das instruções do modelo

As tools bloqueiam deterministamente os valores financeiros para `TRADUTOR` e `RECURSO`. Porém, a regra também aparece no prompt do modelo; respostas sem tool call são texto livre do LLM. A regra no prompt não é uma fronteira de autorização determinística.

**Ajuste planejado:** manter a autorização nas tools e definir uma estratégia para impedir que respostas finais a perfis restritos divulguem valores financeiros vindos de contexto ou histórico. Cobrir o comportamento com testes, sem depender apenas do texto do prompt.

### 7. Nome do manual difere do nome citado na especificação

A especificação cita `docs/manual_stentio.md`, mas o arquivo presente é `docs/manual_operacional_do_sistema_stentio.md`. O carregador atual aceita ambos, então a busca funciona no layout existente; ainda assim, manter dois nomes na documentação pode confundir manutenção e deploy.

**Ajuste planejado:** escolher um nome canônico e usá-lo no manual, carregador, README e imagem Docker.

### 8. Atalho CLIENTE leva à listagem

O frontend direciona o tipo `CLIENTE` para `/clientes`, sem navegar para um registro individual. Caso o critério exija abrir o cliente citado, a rota e o identificador do atalho também precisam ser definidos.

## Sequência recomendada

1. **Fechar definições de negócio:** esclarecer o significado de faturamento mensal e o destino esperado para atalho de cliente.
2. **Corrigir integridade do acesso:** retirar ou isolar o bypass literal `ADMIN`/`TRADUTOR` e testar autenticação com JWT real.
3. **Corrigir contexto recente:** selecionar as dez mensagens mais recentes e manter ordem cronológica para o modelo.
4. **Implementar atalhos de cliente:** retornar um ID válido e confirmar a rota de destino no frontend.
5. **Alinhar o indicador financeiro:** implementar o cálculo de acordo com a definição aprovada ou alterar o texto e a descrição da tool para o total que realmente é calculado.
6. **Alinhar configuração e documentação:** tornar CORS configurável e padronizar o nome do manual.
7. **Revalidar:** executar `uv run pytest`, testar os endpoints via TestClient, verificar o contrato OpenAPI e fazer smoke test do container com PostgreSQL/MongoDB.

## Critérios de aceite para as correções

- Com mais de dez mensagens, o prompt recebe exatamente as dez mais recentes, em ordem cronológica.
- `TRADUTOR` e `RECURSO` nunca recebem valores financeiros por tools nem por resposta baseada em contexto/histórico; demais perfis seguem a matriz autorizada.
- Tokens literais de perfil não autenticam em produção; JWT ausente, inválido ou expirado é rejeitado.
- A pergunta sobre o mês retorna somente o indicador definido pelo negócio, com filtro de período verificável; se faturamento não estiver implementado, o bot informa isso sem apresentar orçamento como receita realizada.
- Uma resposta que mencione cliente produz atalho com ID e navegação corretos, ou a documentação deixa explícito que esse atalho não faz parte do aceite.
- Origens CORS vêm da configuração e as origens não permitidas continuam bloqueadas.
- O RAG localiza o manual canônico tanto no ambiente de desenvolvimento quanto na imagem Docker.
- A suíte pytest passa e os endpoints permanecem disponíveis após rebuild do serviço.
