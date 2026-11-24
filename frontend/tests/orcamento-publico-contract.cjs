const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");

function carregarBaseUrl(env) {
  const source = readFileSync(
    resolve(__dirname, "../src/lib/orcamentoPublicoService.ts"),
    "utf8",
  );
  const js = ts.transpileModule(source, {
    compilerOptions: {
      module: ts.ModuleKind.CommonJS,
      target: ts.ScriptTarget.ES2022,
    },
  }).outputText;
  const moduleObject = { exports: {} };

  vm.runInNewContext(js, {
    exports: moduleObject.exports,
    module: moduleObject,
    process: { env },
    URL,
  });

  return moduleObject.exports.baseUrl;
}

test("orçamento público usa o proxy same-origin em produção", () => {
  const baseUrl = carregarBaseUrl({
    NODE_ENV: "production",
    EXPO_PUBLIC_CORE_API_URL: "http://10.0.2.2:8082",
  });

  assert.equal(baseUrl(), "");
});

test("orçamento público respeita a URL do Core no Expo local", () => {
  const baseUrl = carregarBaseUrl({
    NODE_ENV: "development",
    EXPO_PUBLIC_CORE_API_URL: "http://10.0.2.2:8082/",
  });

  assert.equal(baseUrl(), "http://10.0.2.2:8082");
});
