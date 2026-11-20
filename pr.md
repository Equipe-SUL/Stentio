# US20 — Painel Central de Monitoramento (Backend)

## Resumo

Implementa e valida os endpoints de resumo e pendências do dashboard, incluindo contagens agregadas, taxa de conversão, consolidação/ordenação das pendências e controle de acesso por perfil.

## O que foi feito

- `GET /api/v1/dashboard/resumo`:
  - Retorna `total_orcamentos_em_elaboracao`, `total_orcamentos_aguardando_aprovacao`, `total_os_em_andamento`, `total_etapas_atrasadas` e `taxa_conversao_orcamentos`.
  - Conta orçamentos e OS por status usando `countByStatus`, sem carregar as entidades para as contagens.
  - Considera `EM_PLANEJAMENTO` como OS em andamento.
  - Calcula a taxa como aprovados / (aprovados + rejeitados) × 100 e retorna `0.0` quando ainda não há orçamentos decididos.
  - Conta etapas alocadas cuja `data_limite_entrega` já passou.

- `GET /api/v1/dashboard/pendencias`:
  - Retorna `total_itens` e a lista `pendencias`, com `id`, `tipo_entidade`, `titulo`, `status`, `data_limite` e `link_acao`.
  - Consolida orçamentos `EM_ELABORACAO` e `AGUARDANDO_APROVACAO` com etapas vencidas.
  - Usa projeções de consulta para obter somente os campos necessários.
  - Ordena por `data_limite` crescente; prazos nulos ficam no final. Desempata por tipo e id.
  - Usa `data_expiracao` para o prazo do orçamento.
  - Gera atalhos para `/orcamentos/{id}` e `/ordens/{osId}`, rotas do frontend.

- Segurança:
  - Restringe `/api/v1/dashboard/**` a `ADMIN` e `GESTOR_PROJETO`.
  - Esses perfis recebem o panorama completo, incluindo a taxa de conversão.
  - `ATENDENTE` e `FINANCEIRO` recebem HTTP 403; usuários anônimos recebem HTTP 401.

- Documentação:
  - As operações estão identificadas no Swagger com `@Tag` e `@Operation`.

## Como testar pelo Swagger

1. Inicie o PostgreSQL e os demais serviços dos quais a aplicação dependa, conforme a configuração local.
2. Inicie o Core:

   ```bash
   cd Core
   ./gradlew bootRun
   ```

3. Abra `http://localhost:8080/swagger-ui/index.html` (ou `http://localhost:8080/swagger-ui.html`).
4. Faça login pelo fluxo existente e obtenha um JWT de usuário `ADMIN` ou `GESTOR_PROJETO`.
5. No botão **Authorize**, informe o token JWT. O Swagger está configurado para autenticação Bearer.
6. Execute:
   - `GET /api/v1/dashboard/resumo`
   - `GET /api/v1/dashboard/pendencias`
7. Para conferir as permissões, tente as operações com um perfil `ATENDENTE` ou `FINANCEIRO` (403) e sem autenticação (401).

O Swagger UI e os documentos OpenAPI são públicos; os endpoints do dashboard não são. Se a porta padrão do Core estiver ocupada, configure uma porta alternativa e use-a na URL.

## Como rodar os testes

Requisito: JDK compatível com o toolchain Java 25 declarado em `Core/build.gradle`. O wrapper Gradle do módulo está incluído.

Testes focados de serviço, contrato HTTP e autorização:

```bash
./Core/gradlew -p Core test \
  --tests com.stentio.core.service.DashboardServiceTest \
  --tests com.stentio.core.controller.DashboardControllerTest \
  --rerun-tasks
```

Os testes cobrem contagens, taxa de conversão com zero decisões, ordenação e consolidação, resposta HTTP 200 para perfis autorizados e respostas 401/403 para acessos não autorizados.

Suíte completa do Core:

```bash
./Core/gradlew -p Core test
```

## Testes PostgreSQL de integração

Os testes PostgreSQL de etapas e das projeções do dashboard são opt-in e ficam desabilitados por padrão. Para executá-los:

1. Disponibilize um PostgreSQL de teste acessível, com o banco `stentio_test` criado e usuário/senha configurados.
2. Configure, se necessário, `ORCAMENTO_TEST_JDBC_URL` e `ORCAMENTO_TEST_DB_USER` (o teste também permite ajustar as configurações de conexão declaradas na anotação `@SpringBootTest`).
3. Execute:

   ```bash
   ORCAMENTO_INTEGRATION_TESTS=true ./Core/gradlew -p Core test \
     --tests com.stentio.core.service.AlocacaoEtapaPostgresTest
   ```

Essa integração exercita a contagem de etapas atrasadas e as projeções de pendência diretamente no PostgreSQL. Ela não foi executada com sucesso neste ambiente: anteriormente, o Hibernate não conseguiu obter metadados JDBC da conexão configurada. O teste compila, mas precisa ser executado em um ambiente com datasource corretamente configurado.

## Fora do escopo / pendências conhecidas

- **Interface do dashboard:** esta alteração entrega o backend. A tela com cards, tabela/lista de pendências, ação manual de atualização e auto-refresh de 60 segundos não foi implementada.
- **Filtro por perfil Recurso:** não existe atualmente um perfil `RECURSO` no enum do Core nem no tipo de sessão do frontend. A autorização desta alteração é restrita aos perfis atuais: Admin e Gestor de Projeto.
- **Excluir etapas cujo arquivo final já foi entregue:** o modelo atual da etapa ainda não registra a submissão do arquivo final. Por isso, a contagem atual considera atrasada uma etapa alocada com prazo vencido. A exclusão correta de entregas depende da fonte de verdade que será introduzida/alinhada à US-014.
- **US20-T1 e US20-T2:** as descrições recebidas são duplicadas. O prazo atual é `data_limite_entrega`, preenchido na alocação; `prazo_estimado_horas` é estimativa e não representa uma data limite.
- **Execução completa da suíte:** a suíte inteira deve ser validada com o datasource local configurado. A execução anterior apresentou falhas de inicialização JDBC em `CoreApplicationTests` e `ClienteRepositoryTest`; os testes direcionados da US20 passaram.
