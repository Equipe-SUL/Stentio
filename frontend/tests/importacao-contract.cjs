const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");
const axios = require("axios");
const code = ts.transpileModule(
  readFileSync(resolve(__dirname, "../src/lib/importacaoService.ts"), "utf8"),
  {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
    },
  },
).outputText;
const moduleObject = { exports: {} };
vm.runInNewContext(code, {
  exports: moduleObject.exports,
  module: moduleObject,
  require: (name) =>
    name === "react-native"
      ? { Platform: { OS: "web" } }
      : name === "./auth"
        ? { getToken: async () => null }
        : require(name),
  process: { env: {} },
  FormData,
  File,
  Blob,
  URL,
  Error,
});
const service = moduleObject.exports;
const previa = () => ({
  status: "Validado",
  filename: "teste.csv",
  total_linhas: 3,
  total_recursos: 2,
  total_servicos: 2,
  total_vinculos: 3,
  amostra_recursos: [
    {
      nome: "Ana",
      email: "ana@example.com",
      telefone: null,
      ativo: true,
      servicos: ["Tradução", "Revisão"],
    },
  ],
});
const report = () => ({
  status: "Concluido",
  filename: "teste.csv",
  total_linhas: 3,
  total_recursos: 2,
  total_servicos: 2,
  recursos_criados: 2,
  recursos_reutilizados: 0,
  servicos_criados: 2,
  servicos_reutilizados: 0,
  vinculos_criados: 3,
  vinculos_existentes: 0,
});
const asset = (name) => ({
  name,
  uri: "blob:teste",
  file: new File(["csv"], name),
  size: 3,
});

test("multipart usa file e separador explícito sem definir Content-Type", () => {
  const form = service.montarFormularioCsv(asset("teste.csv"), ";");
  assert.equal(form.get("separador"), ";");
  assert.equal(form.get("file").name, "teste.csv");
  assert.equal(
    service.importApi.defaults.headers.common["Content-Type"],
    undefined,
  );
});
test("prévia nova válida e contrato antigo recusado", () => {
  assert.equal(service.conferirPrevia(previa()).total_vinculos, 3);
  assert.throws(() =>
    service.conferirPrevia({ validacao: "LOCAL", amostra_recursos: [] }),
  );
  assert.throws(() =>
    service.conferirPrevia({
      ...previa(),
      amostra_recursos: [{ ...previa().amostra_recursos[0], servicos: null }],
    }),
  );
});
test("relatório exige confirmação e contadores coerentes", () => {
  assert.equal(service.conferirRelatorio(report()).vinculos_criados, 3);
  assert.throws(() =>
    service.conferirRelatorio({ ...report(), status: "Received" }),
  );
  assert.throws(() =>
    service.conferirRelatorio({ ...report(), recursos_criados: 5 }),
  );
  assert.throws(() =>
    service.conferirRelatorio({ ...report(), vinculos_existentes: undefined }),
  );
});
test("409 é falha integral conhecida; 503 e rede não confirmam gravação", () => {
  const failure = (status) =>
    new axios.AxiosError("falha", undefined, undefined, undefined, {
      status,
      data: {
        detail: {
          codigo: "CONFLITO",
          mensagem: "Conflito no banco.",
          linha: 3,
        },
      },
    });
  const conflict = service.extrairFalhaImportacao(failure(409), true);
  assert.equal(conflict.resultado, "nao_gravado");
  assert.equal(conflict.linha, 3);
  assert.equal(conflict.mensagem, "Conflito no banco.");
  assert.equal(
    service.extrairFalhaImportacao(failure(503), true).resultado,
    "indeterminado",
  );
  assert.equal(
    service.extrairFalhaImportacao(new axios.AxiosError("rede"), true)
      .resultado,
    "indeterminado",
  );
  assert.equal(
    service.extrairFalhaImportacao(new Error("Resposta inválida"), true)
      .resultado,
    "indeterminado",
  );
});
test("extensão, vazio e limite exato de upload", () => {
  assert.equal(service.verificarArquivo(asset("TESTE.CSV"), 3), null);
  assert.match(service.verificarArquivo(asset("teste.txt"), 3), /extensão/);
  assert.match(service.verificarArquivo(asset("teste.csv"), 2), /limite/);
  assert.match(
    service.verificarArquivo({ name: "teste.csv", size: 0 }, 3),
    /vazio/,
  );
});
test("client chama as rotas novas e aguarda commit sem retry", async () => {
  const received = [];
  service.importApi.defaults.adapter = async (config) => {
    received.push(config);
    return {
      status: 200,
      statusText: "OK",
      headers: {},
      config,
      data: config.url.endsWith("/validar") ? previa() : report(),
    };
  };
  await service.validarRecursos(asset("teste.csv"), ",", () => {});
  await service.importarRecursos(asset("teste.csv"), ";");
  assert.deepEqual(
    received.map((r) => r.url),
    ["/upload-csv/validar", "/upload-csv"],
  );
  assert.equal(received[0].data.get("separador"), ",");
  assert.equal(received[1].data.get("separador"), ";");
  assert.equal(received[1].timeout, 0);
  assert.equal(received.length, 2);
});
