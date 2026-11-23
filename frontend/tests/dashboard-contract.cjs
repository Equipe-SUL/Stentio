const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const Module = require('node:module');
const { test } = require('node:test');
const ts = require('typescript');

const arquivo = path.resolve(__dirname, '../src/components/dashboard/dashboardUtils.ts');
const source = fs.readFileSync(arquivo, 'utf8');
const js = ts.transpileModule(source, {
  fileName: arquivo,
  compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
}).outputText;
const modulo = new Module(arquivo, module);
modulo.filename = arquivo;
modulo.paths = Module._nodeModulePaths(path.dirname(arquivo));
modulo._compile(js, arquivo);
const { ordenarPendencias, filtrarPendencias, ehAtrasada, rotaDeAcao } = modulo.exports;

const pendencias = [
  { id: 'b', tipo_entidade: 'ORCAMENTO', status: 'EM_ELABORACAO', data_limite: null },
  { id: 'c', tipo_entidade: 'ETAPA_OS', status: 'ATRASADO', data_limite: '2026-10-21T09:00:00Z' },
  { id: 'a', tipo_entidade: 'ORCAMENTO', status: 'AGUARDANDO_APROVACAO', data_limite: '2026-10-22T17:00:00Z' },
];

test('pendências ordenadas por prazo com null no final, sem mutação', () => {
  const saida = ordenarPendencias(pendencias);
  assert.deepEqual(saida.map(i => i.id), ['c', 'a', 'b']);
  assert.deepEqual(pendencias.map(i => i.id), ['b', 'c', 'a']);
});

test('filtros respeitam status e tipo retornados pelo backend', () => {
  assert.equal(filtrarPendencias(pendencias, 'ATRASADAS').length, 1);
  assert.equal(filtrarPendencias(pendencias, 'ORCAMENTO').length, 2);
  assert.equal(filtrarPendencias(pendencias, 'TODAS').length, 3);
});

test('RN.1: o frontend não infere atraso apenas da data', () => {
  assert.equal(ehAtrasada(pendencias[1]), true);
  assert.equal(ehAtrasada({ ...pendencias[1], status: 'EM_ANDAMENTO' }), false);
});

test('links permitidos usam rota fornecida pelo Core e rejeitam rotas externas', () => {
  assert.equal(rotaDeAcao('/ordens/uuid-da-os'), '/ordens/uuid-da-os');
  assert.equal(rotaDeAcao('/orcamentos/uuid-do-orcamento'), '/orcamentos/uuid-do-orcamento');
  assert.equal(rotaDeAcao('https://exemplo.com'), null);
  assert.equal(rotaDeAcao('//exemplo.com/ordens/123'), null);
  assert.equal(rotaDeAcao('/admin'), null);
});
