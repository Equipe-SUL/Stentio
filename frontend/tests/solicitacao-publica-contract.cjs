const { test } = require("node:test");
const assert = require("node:assert/strict");
const { readFileSync } = require("node:fs");
const { resolve } = require("node:path");
const vm = require("node:vm");
const ts = require("typescript");

function carregarModuloVm(caminhoRelativo) {
  const arquivo = resolve(__dirname, caminhoRelativo);
  const source = readFileSync(arquivo, "utf8");
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
    require: (name) => {
      if (name === "react-native") {
        return { Platform: { OS: "web" } };
      }
      if (name === "./api" || name.endsWith("/api")) {
        return {
          coreApi: { get: async () => ({ data: {} }), post: async () => ({ data: {} }) },
          getApiErrorMessage: (err) => err?.message || "Erro",
        };
      }
      if (name.includes("solicitacaoPublicaService")) {
        return service;
      }
      return require(name);
    },
    process: { env: {} },
    FormData,
    File,
    Blob,
    URL,
    Error,
  });

  return moduleObject.exports;
}

const service = carregarModuloVm("../src/lib/solicitacaoPublicaService.ts");
const {
  somenteDigitos,
  ehCpfValido,
  ehCnpjValido,
  validarCpfOuCnpj,
  formatarCpfCnpjInput,
  formatarTelefoneInput,
  montarFormDataPublico,
} = service;

const schemaExports = carregarModuloVm("../src/lib/solicitacaoPublicaSchema.ts");
const { solicitacaoPublicaSchema, MENSAGEM_IDIOMAS_IGUAIS } = schemaExports;

test("somenteDigitos remove caracteres especiais", () => {
  assert.equal(somenteDigitos("123.456.789-00"), "12345678900");
  assert.equal(somenteDigitos("(11) 98765-4321"), "11987654321");
  assert.equal(somenteDigitos("texto123"), "123");
  assert.equal(somenteDigitos(null), "");
});

test("RN.3: validação de CPF (dígitos verificadores e sequências repetidas)", () => {
  // CPF válido gerado de acordo com algoritmo oficial
  assert.equal(ehCpfValido("52998224725"), true);
  assert.equal(ehCpfValido("529.982.247-25"), true);

  // Sequências com todos dígitos iguais são inválidas
  assert.equal(ehCpfValido("11111111111"), false);
  assert.equal(ehCpfValido("00000000000"), false);

  // Dígito verificador incorreto
  assert.equal(ehCpfValido("52998224726"), false);

  // Tamanho incorreto
  assert.equal(ehCpfValido("12345"), false);
});

test("RN.3: validação de CNPJ (dígitos verificadores e sequências repetidas)", () => {
  // CNPJ válido (exemplo Petrobras / Banco do Brasil / formato oficial)
  assert.equal(ehCnpjValido("00000000000191"), true);
  assert.equal(ehCnpjValido("00.000.000/0001-91"), true);

  // Sequências com dígitos iguais
  assert.equal(ehCnpjValido("00000000000000"), false);
  assert.equal(ehCnpjValido("11111111111111"), false);

  // Dígito incorreto
  assert.equal(ehCnpjValido("00000000000192"), false);

  // Tamanho incorreto
  assert.equal(ehCnpjValido("1234567890123"), false);
});

test("validarCpfOuCnpj aceita tanto CPF quanto CNPJ válidos", () => {
  assert.equal(validarCpfOuCnpj("529.982.247-25"), true);
  assert.equal(validarCpfOuCnpj("00.000.000/0001-91"), true);
  assert.equal(validarCpfOuCnpj("12345678900"), false);
});

test("formatação visual de CPF e CNPJ", () => {
  assert.equal(formatarCpfCnpjInput("52998224725"), "529.982.247-25");
  assert.equal(formatarCpfCnpjInput("00000000000191"), "00.000.000/0001-91");
});

test("formatação visual de Telefone com DDD", () => {
  assert.equal(formatarTelefoneInput("11987654321"), "(11) 98765-4321");
  assert.equal(formatarTelefoneInput("1133334444"), "(11) 3333-4444");
});

test("Cenário 3 (BDD) e RN.1: seleção de idiomas iguais é rejeitada pelo schema", () => {
  const mesmoIdiomaId = "550e8400-e29b-41d4-a716-446655440000";
  const resultado = solicitacaoPublicaSchema.safeParse({
    nomeEmpresa: "Empresa Teste",
    nomeRepresentante: "Representante Teste",
    emailRepresentante: "teste@empresa.com",
    cpfCnpj: "52998224725",
    categoriaProjetoId: "550e8400-e29b-41d4-a716-446655440001",
    idiomaOrigemId: mesmoIdiomaId,
    idiomaDestinoId: mesmoIdiomaId,
    descricao: "Texto para tradução",
  });

  assert.equal(resultado.success, false);
  const erroIdioma = resultado.error.issues.find(
    (i) => i.path.includes("idiomaDestinoId")
  );
  assert.ok(erroIdioma, "Deveria ter erro no idiomaDestinoId");
  assert.equal(erroIdioma.message, MENSAGEM_IDIOMAS_IGUAIS);
});

test("Cenário 4 (BDD): campos obrigatórios ausentes são rejeitados", () => {
  const resultado = solicitacaoPublicaSchema.safeParse({
    nomeEmpresa: "",
    nomeRepresentante: "",
    emailRepresentante: "",
    cpfCnpj: "",
    categoriaProjetoId: "",
    idiomaOrigemId: "",
    idiomaDestinoId: "",
  });

  assert.equal(resultado.success, false);
  const camposComErro = resultado.error.issues.map((i) => i.path[0]);
  assert.ok(camposComErro.includes("nomeEmpresa"));
  assert.ok(camposComErro.includes("nomeRepresentante"));
  assert.ok(camposComErro.includes("emailRepresentante"));
  assert.ok(camposComErro.includes("cpfCnpj"));
  assert.ok(camposComErro.includes("categoriaProjetoId"));
  assert.ok(camposComErro.includes("idiomaOrigemId"));
  assert.ok(camposComErro.includes("idiomaDestinoId"));
});

test("RN.4: descrição com mais de 2000 caracteres é rejeitada", () => {
  const descricaoLonga = "a".repeat(2001);
  const resultado = solicitacaoPublicaSchema.safeParse({
    nomeEmpresa: "Empresa Teste",
    nomeRepresentante: "Representante Teste",
    emailRepresentante: "teste@empresa.com",
    cpfCnpj: "52998224725",
    categoriaProjetoId: "550e8400-e29b-41d4-a716-446655440001",
    idiomaOrigemId: "550e8400-e29b-41d4-a716-446655440002",
    idiomaDestinoId: "550e8400-e29b-41d4-a716-446655440003",
    descricao: descricaoLonga,
  });

  assert.equal(resultado.success, false);
  const erroDescricao = resultado.error.issues.find((i) => i.path.includes("descricao"));
  assert.ok(erroDescricao, "Deveria conter erro de tamanho na descrição");
});

test("Cenários 1 e 2 (BDD): preenchimento válido é aceito pelo schema", () => {
  const resultado = solicitacaoPublicaSchema.safeParse({
    nomeEmpresa: "Empresa Inovadora Ltda",
    nomeRepresentante: "Carlos Silva",
    emailRepresentante: "carlos@empresa.com",
    telefone: "(11) 98765-4321",
    cpfCnpj: "529.982.247-25",
    categoriaProjetoId: "550e8400-e29b-41d4-a716-446655440001",
    idiomaOrigemId: "550e8400-e29b-41d4-a716-446655440002",
    idiomaDestinoId: "550e8400-e29b-41d4-a716-446655440003",
    descricao: "Tradução de contrato comercial internacional.",
  });

  assert.equal(resultado.success, true);
});

test("CA.2: montagem de multipart/form-data com anexos", () => {
  const valores = {
    nomeEmpresa: "Empresa Soluções",
    nomeRepresentante: "Mariana Souza",
    emailRepresentante: "mariana@empresa.com",
    telefone: "11988887777",
    cpfCnpj: "52998224725",
    categoriaProjetoId: "cat-1",
    idiomaOrigemId: "id-pt",
    idiomaDestinoId: "id-en",
    descricao: "Contrato em PDF para tradução",
  };
  const arquivoFake = {
    id: "arq-1",
    nome: "contrato.pdf",
    uri: "blob:contrato",
    tipo: "application/pdf",
    tamanho: 1024,
    file: new File(["teste"], "contrato.pdf", { type: "application/pdf" }),
  };

  const form = montarFormDataPublico(valores, [arquivoFake]);
  assert.equal(form.get("nomeEmpresa"), "Empresa Soluções");
  assert.equal(form.get("cpfCnpj"), "52998224725");
  assert.equal(form.get("categoriaProjetoId"), "cat-1");
  assert.equal(form.get("idiomaOrigemId"), "id-pt");
  assert.equal(form.get("idiomaDestinoId"), "id-en");
  assert.equal(form.get("descricao"), "Contrato em PDF para tradução");
  const arquivoNoForm = form.get("arquivos");
  assert.ok(arquivoNoForm);
  assert.equal(arquivoNoForm.name, "contrato.pdf");
});
