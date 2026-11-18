# US-020 Dashboard — integração com o Core (frontend)

## O que está no código

A implementação **consome os endpoints reais** do Core sem criar dados locais e sem alterar o código Java.
O `coreApi` já instalado no projeto envia o cookie/token de sessão existente.

| Task | Implementação |
| --- | --- |
| T6 | `/dashboard`, menu PAINEL e autorização para `ADMIN` e `GESTOR_PROJETO`. |
| T7 | Contratos JSON TypeScript e chamadas via `coreApi`. |
| T8 | 4 cards, total de orçamentos pendentes, taxa de conversão, estados de loading e erro. |
| T9 | Lista de pendências com filtros locais, ordenação por data limite, status e navegação usando `link_acao` do Core. |
| T10 | Atualização manual e automática, padrão de 60s, opções 30/60/120s, pausa se a tela perde foco/aba fica oculta. |

Endpoints:

- `GET /api/v1/dashboard/resumo`
- `GET /api/v1/dashboard/pendencias`

`/pendencias` retorna `total_itens` e a lista `pendencias` com `id`, `tipo_entidade`, `titulo`, `status`, `data_limite`, `link_acao`.
A UI aceita links internos `/ordens/{osId}` e `/orcamentos/{orcamentoId}`. **Não** usa `id` da etapa para construir rota.

## Execução local do frontend com backend no Docker

Na pasta `frontend`, mantenha `.env.local` *local, não versionado*:

```dotenv
EXPO_PUBLIC_API_URL=http://localhost:8081
EXPO_PUBLIC_CORE_API_URL=http://localhost:8082
```

Rode Expo no navegador na porta 3000 para não conflitar com Auth:

```powershell
npx expo start --clear --web --port 3000
```

Os serviços Auth e Core precisam estar rodando, e os dados reais devem existir no PostgreSQL.
Para consumir os serviços em portas separadas a partir do Expo na origem `http://localhost:3000`, **inclua essa origem na variável `APP_CORS_ALLOWED_ORIGINS` do backend** (Auth e Core) e reinicie os serviços. Essa configuração pode ser feita na `.env` local do Docker Compose, sem mexer no Java.
A configuração da origem é necessária no Core; Auth também deve aceitá-la. O login e os endpoints protegidos exigem sessão válida.

Ao usar frontend+backend via o Nginx fornecido pelo projeto (mesma origem), mantenha as URLs de API configuradas conforme o ambiente.

Verificação manual: abra a aba Network do navegador. Para `/api/v1/dashboard/resumo` e `/api/v1/dashboard/pendencias` espere HTTP 200 autenticado. `401` significa ausência/sessão inválida; `403` indica perfil não autorizado; falha de CORS exige conferir `APP_CORS_ALLOWED_ORIGINS`.

## Limites do backend atual, para registrar com a equipe

- **CA.3 / RN.2:** a branch do colega atualmente permite apenas `ADMIN` e `GESTOR_PROJETO`. Não existe perfil `RECURSO` no `Role` do Auth, nem endpoint de dashboard individual por recurso. O frontend segue essa política e não faz filtragem sensível só visualmente.
- **RN.1:** o Core considera uma etapa atrasada quando já passou `dataLimiteEntrega` e há recurso alocado. O comentário do repositório explica que a US-014 ainda deve considerar a entrega do arquivo final. O frontend usa `status=ATRASADO` retornado e não cria uma regra paralela.
- **Contagem de OS:** o Core calcula `total_os_em_andamento` com `countByStatus(EM_PLANEJAMENTO)`; confirmar com a equipe se essa é a semântica esperada para “OS ativas”.
- O contrato atual não retorna responsáveis, localidades, metas mensais, tendência histórica nem distribuição por etapa. O layout não inventa esses dados para reproduzir o mockup.

## Validação

```powershell
cd frontend
node --test tests/dashboard-contract.cjs
npx tsc --noEmit
npx expo start --web --port 3000
```

Testar como ADMIN e GESTOR_PROJETO: cards, lista, filtro, links, 60 segundos, erro e refresh; verificar bloqueio para ATENDENTE e FINANCEIRO e suspensão do polling ao sair da aba.
