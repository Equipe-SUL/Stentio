# Importação de recursos e serviços — frontend atualizado

Acesso: **Sistema → Importar recursos** (`/importacoes`).
A tela usa o contrato da versão ORM do Import Service: prévia local, confirmação
e resultado do arquivo inteiro após um único commit.

## Executar

Preserve seu `.env` e confirme a variável abaixo, usando somente a URL base:

```env
EXPO_PUBLIC_IMPORT_API_URL=http://localhost:8000
```

Na pasta `frontend`:

```powershell
npm ci
npx expo start --web --port 8080
```

Para executar com o Nginx do projeto, na pasta do Docker Compose:

```powershell
docker compose up -d --build front
```

Atualize o navegador com Ctrl+Shift+R após o build. As variáveis `EXPO_PUBLIC_*`
são incorporadas na exportação, portanto reiniciar o container sem reconstruir
não atualiza o JavaScript.

O login continua usando a configuração atual do projeto, com base `/` na web.
Os arquivos de Auth, Core, navegação e Nginx não foram modificados nesta adaptação.

O Import Service deve estar iniciado na porta 8000. Na pasta dele:

```powershell
uv run uvicorn src.app:app --app-dir . --reload --port 8000
```

O nome do modelo SQLAlchemy e do `back_populates` precisa estar corrigido conforme
os ajustes já realizados no backend. Esta entrega altera somente o frontend.

## CORS e acesso ao serviço

Se o frontend abre em `http://localhost` (porta 80), essa origem precisa estar
permitida pelo Import Service. Se abre em `http://localhost:8080`, inclua essa
origem também. No `.env` do backend, por exemplo:

```env
CORS_ALLOWED_ORIGINS=["http://localhost","http://localhost:8080","http://127.0.0.1","http://127.0.0.1:8080"]
```

Reinicie o backend após alterar o `.env`.
O client mantém a regra já utilizada de alinhar o hostname ao da página na web.
Em aparelho físico, configure um endereço acessível do computador; em emulador
Android a URL padrão é `http://10.0.2.2:8000` quando não há variável definida.
Para builds nativos, a URL do Auth também precisa ser absoluta, conforme o ambiente.
A versão ORM do backend não acrescenta autenticação nesta tarefa; a tela permanece
no painel protegido pela sessão existente do Stentio.

## Contrato HTTP

| Método e rota | Uso |
| --- | --- |
| `GET /api/v1/importacoes/configuracao` | Carrega limites reais do servidor |
| `POST /upload-csv/validar` | Retorna a prévia sem consultar/gravar no banco |
| `POST /upload-csv` | Retorna os contadores após a confirmação da transação |

Os POSTs enviam multipart com `file` e `separador` explícito (`;` ou `,`).
O padrão selecionado é `;`. A opção de detecção automática foi removida porque
o backend atual não implementa essa detecção. O boundary multipart é definido
pelo client, sem um Content-Type manual.

A prévia usa `status=Validado`, `total_linhas`, `total_recursos`, `total_servicos`,
`total_vinculos` e `amostra_recursos` com nome, e-mail, telefone, ativo e serviços.
O resultado usa `status=Concluido` e os contadores de recursos criados/reutilizados,
serviços criados/reutilizados e vínculos criados/existentes.
A resposta é verificada em runtime para evitar apresentar sucesso com o contrato
antigo ou com dados incompletos. A API não retorna uma lista de resultados por
profissional; a tela apresenta somente os dados efetivamente fornecidos.

O upload para validação mostra a porcentagem de transmissão. Na importação, o
indicador aguarda a confirmação da transação inteira, sem simular progresso de
registros e sem timeout curto no client. O aplicativo não implementa retry de
chamadas de gravação.

## CSV provisório

O botão **Baixar CSV de exemplo** na web gera este layout em UTF-8 com BOM:

```csv
ativo;e-mail;nome;telefone;tipo_servico;descricao_servico
1;ana@example.com;Ana;01199990000;Tradução;
1;ana@example.com;Ana;01199990000;Revisão;
1;bruno@example.com;Bruno;;Tradução;
```

O exemplo representa 2 recursos, 2 serviços distintos e 3 vínculos.
`e-mail` precisa manter o hífen no cabeçalho; o backend o renomeia internamente.
Uma linha representa um vínculo. O mesmo e-mail agrupa um profissional e seus
serviços. Telefone e descrição podem ficar vazios. `ativo` aceita apenas 0 ou 1.
As seis colunas precisam estar no arquivo, mesmo quando seus valores são opcionais.

Serviços são identificados por nome, sem UUIDs no CSV. O backend reutiliza os
existentes e cria os ausentes. Cadastros em conflito não são sobrescritos.
Os campos de idiomas, preços, unidade e valor pertenciam ao contrato anterior e
foram retirados desta tela e do exemplo para download.

## Tratamento dos resultados

- HTTP 200 com `Concluido`: apresenta confirmação e seis contadores.
- Reenvio com todos os contadores de criação zerados: apresenta sucesso com
  reutilização dos cadastros e vínculos existentes.
- HTTP 400/409/413/422: apresenta o erro; nenhuma alteração daquela tentativa é
  apresentada como confirmada. Não há indicação de sucesso parcial.
- HTTP 503, erro inesperado ou perda da resposta: o resultado é tratado como
  não confirmado. A tela orienta conferir os cadastros antes do reenvio.
- Erros estruturados são lidos de `detail.mensagem`, `detail.codigo`, `detail.linha`
  e `detail.campo`, quando fornecidos.
- Um erro mantém o resultado visível e exige iniciar nova tentativa, sem
  reenvio automático do arquivo.

Alterar o arquivo ou o separador descarta a prévia e exige validar novamente.
Os controles ficam bloqueados durante processamento e após uma tentativa de
importação. O estado fica em memória; sair/recarregar a página perde o relatório.

## Roteiro do PO

1. Abra Importar recursos pelo menu ou pela Central do sistema.
2. Baixe o exemplo e selecione o arquivo. O padrão deve ser ponto e vírgula.
3. Clique em Validar arquivo. Confira 3 linhas, 2 recursos, 2 serviços e 3 vínculos.
4. Confira Ana com Tradução e Revisão e Bruno com Tradução.
5. Troque o separador: a prévia deve desaparecer. Volte para `;` e valide novamente.
6. Abra a confirmação e cancele. Nenhuma gravação deve ocorrer.
7. Confirme a importação. Confira os seis contadores e a lista de recursos.
8. Reenvie o mesmo arquivo: os contadores de criação devem ser zero.
9. Mude o nome da Ana mantendo o e-mail. Espere 409 e a mensagem de conflito,
   sem contadores de sucesso e sem cadastro parcial.
10. Envie ativo inválido: espere a mensagem e a linha retornadas pelo backend.
11. Teste a tela em celular e desktop, inclusive prévia, modal e resultado.

## Verificação técnica

```powershell
npx tsc --noEmit
npx eslint src/components/importacoes src/lib/importacaoService.ts
node --test tests/importacao-contract.cjs
npx expo export --platform web
```

Os testes de contrato usam Node 20+ (Docker do projeto usa Node 24), TypeScript e
Axios já presentes nas dependências. Não instalam outro framework de testes.
Eles verificam multipart, endpoints, respostas antigas/incompletas, contadores,
erros 409/503 e limites do arquivo.

A validação de navegador usa HTTP simulado conforme o contrato do backend.
O PostgreSQL e a autenticação real não foram acessados durante esses testes.
Builds Android/iOS não foram executados. O ZIP recebido referencia ícones em
`assets/images` que não foram incluídos; o Expo emite aviso de favicon ausente.

## Arquivos adaptados

Somente os componentes da importação, seus tipos, hook e client HTTP foram
alterados, além desta documentação e do teste de contrato. Nenhuma dependência
foi acrescentada. As alterações integradas das outras branches foram preservadas.

### Resultado desta adaptação

Checagem TypeScript, lint e exportação web concluídos. Seis testes automatizados
de contrato passaram. Foram verificadas dezoito situações no navegador com API
HTTP simulada, incluindo larguras de 390, 768, 1024 e 1440 px, download do modelo,
confirmação, reutilização, conflito e resposta perdida. A tela retorna ao início
ao iniciar a gravação e ao receber o resultado para manter a confirmação visível.
A integração com o PostgreSQL real deve ser conferida pelo roteiro do PO.
