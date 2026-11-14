# Stentio — Importação de recursos e serviços

Versão baseada no `import-service(1).zip`, com persistência direta pelo ORM SQLAlchemy.

O arquivo inteiro é validado antes da gravação. O importador agrupa profissionais
por e-mail, resolve serviços por nome e monta `Recurso.tipos_servicos`. As tabelas
`recursos`, `tipos_servico` e `recurso_tipos_servicos` são persistidas em uma única
transação. A resposta de sucesso é enviada somente depois de o commit terminar.
Não há tarefa em background nem envio ao Core nesta versão.

## Rodar no Windows

1. Extraia o pacote e use a pasta `import-service`.
2. Preserve seu `.env` atual. Se ainda não existir, copie `.env.example` para `.env`
   e preencha a senha e os dados do seu PostgreSQL. Não substitua sua senha pela
   senha ilustrativa do exemplo.
3. Execute na raiz do serviço:

```powershell
uv sync --frozen
uv run pytest -q
uv run uvicorn src.app:app --app-dir . --reload --port 8000
```

Swagger: http://localhost:8000/docs

O exemplo está em `examples/recursos-servicos.csv`. Para o primeiro teste, use
`POST /upload-csv/validar`, que não acessa o banco. Depois use `POST /upload-csv`.

Se estiver rodando dentro do Docker Compose, configure `POSTGRES_HOST=postgres`
(ou o nome real do serviço) e a porta interna do PostgreSQL, normalmente `5432`.
Fora do Docker, use `localhost` e a porta publicada no computador.
O Dockerfile existente expõe o importador na porta 8000; ele ainda precisa ser
adicionado ao Compose do projeto caso você deseje iniciá-lo junto aos outros serviços.

## Layout provisório do CSV

UTF-8 com ou sem BOM. Separador padrão `;`. Para vírgula, envie o campo de
formulário `separador=,` explicitamente. O schema não descarta colunas extras;
o cabeçalho precisa conter exatamente estas seis colunas, em qualquer ordem:

```csv
ativo;e-mail;nome;telefone;tipo_servico;descricao_servico
1;ana@example.com;Ana;01199990000;Tradução;
1;ana@example.com;Ana;01199990000;Revisão;
1;bruno@example.com;Bruno;;Tradução;
```

| Coluna | Regra |
| --- | --- |
| `ativo` | `0` ou `1`; corresponde ao recurso |
| `e-mail` | E-mail válido; identificação do profissional; normalizado para minúsculas |
| `nome` | Obrigatório; até 255 caracteres |
| `telefone` | Opcional; texto de até 20 caracteres, preservando zeros à esquerda |
| `tipo_servico` | Nome obrigatório; até 255 caracteres |
| `descricao_servico` | Opcional; até 500 caracteres |

Uma linha representa um vínculo. Repetir o e-mail com serviços diferentes
forma um único recurso com vários serviços. Repetir a mesma linha não duplica
cadastros nem vínculos. Os dados pessoais repetidos precisam ser consistentes.

Serviços são identificados por nome com normalização Unicode NFC, espaços
externos/internos e diferenças de maiúsculas/minúsculas. Acentos são preservados:
`Tradução` e `Traducao` são nomes distintos.
Os IDs são objetos UUID gerados na aplicação somente para cadastros novos.

## Regras adotadas para concluir a implementação

- Serviço existente é reutilizado; serviço ausente é criado com `ativo=true`.
- Serviço inativo ou mais de um serviço existente com o mesmo nome normalizado
  causa erro 409 para o arquivo inteiro.
- Descrição vazia não altera a existente. Uma descrição não vazia diferente
  da cadastrada causa erro 409. Descrições divergentes entre linhas do próprio
  CSV causam erro 422.
- Recurso existente é reutilizado por e-mail. Nome, telefone ou status diferente
  do cadastro causam erro 409. Cadastros existentes não são sobrescritos.
- Vínculo existente é preservado. Somente os vínculos ausentes são acrescentados.
- Qualquer falha de validação local impede a abertura da transação de gravação.
- Falha durante as operações de banco reverte a transação completa. Não há sucesso
  parcial por profissional.
- Nenhum recurso, serviço ou vínculo anterior é excluído.

Essas regras são provisórias até alinhar com o cliente o contrato do legado.

## Rotas e respostas

| Método e rota | Função |
| --- | --- |
| `GET /` | Estado básico do processo |
| `GET /api/v1/importacoes/configuracao` | Limites configurados |
| `POST /upload-csv/validar` | Validação local e prévia sem consultar/gravar no banco |
| `POST /upload-csv` | Importação ORM com confirmação ao final |

Os POSTs recebem multipart com `file` e, opcionalmente, `separador`.
A validação local não confirma a existência das tabelas nem conflitos com dados
já cadastrados; essas verificações ocorrem na etapa de importação.

Exemplo de sucesso com banco inicialmente sem esses registros:

```json
{
  "status": "Concluido",
  "filename": "recursos-servicos.csv",
  "total_linhas": 3,
  "total_recursos": 2,
  "total_servicos": 2,
  "recursos_criados": 2,
  "recursos_reutilizados": 0,
  "servicos_criados": 2,
  "servicos_reutilizados": 0,
  "vinculos_criados": 3,
  "vinculos_existentes": 0
}
```

No reenvio, os contadores de criação serão zero; os registros/vínculos serão
contados como reutilizados/existentes.

| HTTP | Significado |
| --- | --- |
| 200 | Validação concluída ou importação confirmada, conforme a rota |
| 400 | Extensão/nome de arquivo inválido |
| 413 | Arquivo acima do limite configurado |
| 422 | CSV, campos ou valores incompatíveis |
| 409 | Dados existentes em conflito ou violação de integridade |
| 503 | Banco inacessível; importação não confirmada |
| 500 | Falha inesperada, sem expor detalhes internos |

Erros usam `detail.codigo`, `detail.mensagem` e, quando aplicável, `detail.linha`.
A linha de validação de dados é a posição lógica do registro mais o cabeçalho;
em campos com quebras de linha ou linhas vazias ela pode divergir da linha física.

Se a conexão cair durante a confirmação, confira os cadastros antes de concluir
que houve rollback. Não há retry automático. O reenvio com os mesmos dados é
tratado para reutilizar os registros e vínculos já confirmados.

## Banco existente

A versão enviada usa `tipos_servico` (singular) no modelo e na FK. Esse nome foi
preservado. Confirme o nome real antes de importar:

```sql
SELECT to_regclass('public.recursos'),
       to_regclass('public.tipos_servico'),
       to_regclass('public.recurso_tipos_servicos');
```

Se o nome real for `tipos_servicos`, altere `__tablename__` de `TipoServico` e o
alvo da FK na tabela intermediária em conjunto.
Não é executado `create_all` nem criada migração no banco de negócio.
Os testes criam somente tabelas temporárias em SQLite; o teste PostgreSQL opcional
cria um schema isolado e o remove no final.

A PK de `recursos` e `tipos_servico` deve ser UUID. A associação deve ter PK
composta por `recurso_id` e `tipo_servico_id` e FKs para as tabelas correspondentes.
Os limites reais das colunas podem ter sido definidos pelo Core; se forem mais
restritivos, alinhe os limites de validação com o DDL real.

Importações executadas por este serviço no PostgreSQL usam um advisory lock
transacional para serializar seus lotes. Isso não impede um serviço externo,
como o Core, de inserir cadastros simultaneamente. A unicidade no banco deve
continuar sendo garantida pelas constraints do projeto. O importador não cria
uma constraint nova de unicidade de nome no catálogo.

## Integração com o frontend

Esta entrega implementa o backend atualizado que escreve diretamente no banco.
O frontend anterior foi criado para outro CSV (UUIDs de idiomas, preços e tipos)
e para `/api/v1/importacoes/recursos*`. Esse contrato não é intercambiável.

A tela deve chamar `/upload-csv/validar` e `/upload-csv` e consumir os novos
schemas descritos em `/docs`. Não basta mudar somente a URL: a prévia agora
contém `servicos` e o resultado contém contadores da transação integral.
A rota de configuração foi mantida para carregar os limites no navegador.
Nenhum arquivo do frontend foi substituído neste pacote.

CORS está configurado para os hosts locais do exemplo e permite credenciais.
A versão recebida não implementava autenticação e ela não foi acrescentada
nesta tarefa; os endpoints são destinados ao ambiente local de teste até a
integração com a autorização do projeto.

## Testes e roteiro de entrega

Execute `uv run pytest -q`.

Os testes de API/ORM usam SQLite assíncrono em memória, FKs ativadas e as classes
reais do serviço. Cobrem agrupamento, compartilhamento do serviço, reenvio,
vínculos já existentes, conflito com dados cadastrados e rollback de todas as
tabelas quando a associação falha após os INSERTs dos novos cadastros.
Há teste que verifica um único commit de escrita por arquivo.

Para testar PostgreSQL real (use um banco de teste e usuário com permissão de
criar schema):

```powershell
$env:TEST_POSTGRES_URL='postgresql+asyncpg://usuario:senha@localhost:5432/banco_de_teste'
uv run pytest tests/test_postgres.py -q
Remove-Item Env:TEST_POSTGRES_URL
```

Esse teste cria um schema `teste_import_<uuid>`, testa criação, reenvio e rollback,
e remove somente esse schema ao concluir. A senha da URL precisa de escape
percentual se contiver caracteres reservados.
Sem essa variável, o teste PostgreSQL é ignorado automaticamente.

Conferência manual:

1. Faça a validação do CSV de exemplo em `/docs`; espere 2 recursos e 3 vínculos.
2. Importe; confira os registros nas três tabelas.
3. Reenvie; confira contadores de criação zerados.
4. Mude o nome da Ana mantendo o e-mail; espere 409 e nenhuma alteração.
5. Use ativo diferente de 0/1; espere 422 sem gravar.
6. Confira no PostgreSQL que nenhum registro foi sobrescrito ou removido.

## Organização do código

| Arquivo | Papel |
| --- | --- |
| `src/app.py` | Upload limitado, HTTP, validação e resposta após commit |
| `src/import_plan.py` | Estrutura CSV, Polars, validação e agrupamento sem banco |
| `src/process.py` | Consulta de existentes, montagem ORM e transação |
| `src/database.py` | Engine e fábrica de sessões sem autoflush |
| `src/models/*` | Mapeamento das três tabelas existentes |
| `src/schemas/upload_schema.py` | Contratos das respostas |

O limite de arquivo é aplicado após o parser multipart do framework. Para limitar
a transmissão antes disso, configure também o servidor de entrada/proxy.

## Verificação realizada nesta entrega

Python 3.14: 34 testes passaram; 1 teste PostgreSQL foi ignorado porque
`TEST_POSTGRES_URL` não estava configurada. A checagem estática de erros de sintaxe
e nomes indefinidos também passou. O PostgreSQL de negócio não foi acessado.
