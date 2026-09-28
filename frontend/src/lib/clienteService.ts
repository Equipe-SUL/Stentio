import { coreApi } from "./api";
import type { FieldDef } from "../components/usuarios/types";

// Espelha o contrato de `ClienteResponse` no Core, mas só com os campos que
// a tela usa. `dataCadastro` e `dataModificacao` chegam na resposta e são
// ignorados aqui de propósito: não são editáveis, então não devem virar
// coluna nem entrar no `defaultValues` do formulário.
export interface Cliente {
  id: string;
  nomeEmpresa: string;
  nomeRepresentante: string;
  emailRepresentante: string;
  telefone: string | null;
  cpfCnpj: string;
}

// Telefone é o único campo opcional: o Core aceita vazio e normaliza para
// null na entidade, então ele volta vazio na listagem.
export const camposCliente: FieldDef<Cliente>[] = [
  {
    key: "nomeEmpresa",
    label: "Empresa",
    type: "text",
    placeholder: "Razão social ou nome do cliente",
  },
  {
    key: "nomeRepresentante",
    label: "Representante",
    type: "text",
    placeholder: "Quem responde pela solicitação",
  },
  {
    key: "emailRepresentante",
    label: "E-mail",
    type: "text",
    placeholder: "nome@empresa.com",
  },
  {
    key: "cpfCnpj",
    label: "CPF ou CNPJ",
    type: "text",
    placeholder: "Somente números",
  },
  {
    key: "telefone",
    label: "Telefone",
    type: "text",
    required: false,
    placeholder: "(11) 99999-9999",
  },
];

// `termo` no Core busca em `nome_empresa` e `nome_representante` de uma vez.
function montarQuery(termo?: string): string {
  const params = new URLSearchParams();
  if (termo?.trim()) params.set("termo", termo.trim());
  // Busca única: sem paginação para listar todos os clientes de uma chamada.
  params.set("size", "100");
  return params.toString();
}

export async function listarClientes(termo?: string): Promise<Cliente[]> {
  const { data } = await coreApi.get<{ content: Cliente[] }>(
    `/api/v1/clientes?${montarQuery(termo)}`,
  );
  return data.content ?? data;
}

export async function criarCliente(valores: Omit<Cliente, "id">): Promise<Cliente> {
  const { data } = await coreApi.post<Cliente>("/api/v1/clientes", {
    nomeEmpresa: valores.nomeEmpresa,
    nomeRepresentante: valores.nomeRepresentante,
    emailRepresentante: valores.emailRepresentante,
    cpfCnpj: valores.cpfCnpj,
    telefone: valores.telefone,
  });
  return data;
}

export async function atualizarCliente(id: string, valores: Omit<Cliente, "id">) {
  await coreApi.put(`/api/v1/clientes/${id}`, {
    nomeEmpresa: valores.nomeEmpresa,
    nomeRepresentante: valores.nomeRepresentante,
    emailRepresentante: valores.emailRepresentante,
    cpfCnpj: valores.cpfCnpj,
    telefone: valores.telefone,
  });
}

// Exclusão física: o Core não faz soft delete em clientes, e o DELETE exige
// role ADMIN. Ainda não há vínculo com solicitações, então não há o que reter.
export async function excluirCliente(id: string) {
  await coreApi.delete(`/api/v1/clientes/${id}`);
}
