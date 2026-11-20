import axios from "axios";
import { Platform } from "react-native";
import { coreApi, getApiErrorMessage } from "./api";
import type { ArquivoSelecionado } from "../components/solicitacoes/types";

export interface CategoriaPublicaOpcao {
  id: string;
  nome: string;
}

export interface IdiomaPublicoOpcao {
  id: string;
  nome: string;
  codigoIso: string;
}

export interface SolicitacaoPublicaOpcoes {
  categoriasProjeto: CategoriaPublicaOpcao[];
  idiomas: IdiomaPublicoOpcao[];
}

export interface SolicitacaoPublicaFormValues {
  nomeEmpresa: string;
  nomeRepresentante: string;
  emailRepresentante: string;
  telefone?: string;
  cpfCnpj: string;
  categoriaProjetoId: string;
  idiomaOrigemId: string;
  idiomaDestinoId: string;
  descricao?: string;
}

export interface SolicitacaoPublicaCriadaResponse {
  id: string;
  status: string;
  cliente?: { id: string; nomeEmpresa: string };
  categoriaProjeto?: { id: string; nome: string };
  idiomaOrigem?: { id: string; nome: string; codigoIso: string };
  idiomaDestino?: { id: string; nome: string; codigoIso: string };
  descricao?: string;
  dataRecebimento?: string;
}

export interface ErroCampoPublico {
  campo: string;
  mensagem: string;
}

const ENDPOINT_OPCOES = "/api/v1/public/solicitacoes/opcoes";
const ENDPOINT_SOLICITACOES = "/api/v1/public/solicitacoes";

export function somenteDigitos(valor?: string | null): string {
  if (!valor) return "";
  return valor.replace(/\D/g, "");
}

export function ehCpfValido(cpf: string): boolean {
  const digitos = somenteDigitos(cpf);
  if (digitos.length !== 11) return false;
  if (/^(\d)\1{10}$/.test(digitos)) return false;

  let soma = 0;
  for (let i = 0; i < 9; i++) {
    soma += parseInt(digitos.charAt(i), 10) * (10 - i);
  }
  let resto = 11 - (soma % 11);
  const digito1 = resto >= 10 ? 0 : resto;
  if (digito1 !== parseInt(digitos.charAt(9), 10)) return false;

  soma = 0;
  for (let i = 0; i < 10; i++) {
    soma += parseInt(digitos.charAt(i), 10) * (11 - i);
  }
  resto = 11 - (soma % 11);
  const digito2 = resto >= 10 ? 0 : resto;
  return digito2 === parseInt(digitos.charAt(10), 10);
}

export function ehCnpjValido(cnpj: string): boolean {
  const digitos = somenteDigitos(cnpj);
  if (digitos.length !== 14) return false;
  if (/^(\d)\1{13}$/.test(digitos)) return false;

  const pesos1 = [5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];
  const pesos2 = [6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2];

  let soma = 0;
  for (let i = 0; i < 12; i++) {
    soma += parseInt(digitos.charAt(i), 10) * pesos1[i];
  }
  let resto = soma % 11;
  const digito1 = resto < 2 ? 0 : 11 - resto;
  if (digito1 !== parseInt(digitos.charAt(12), 10)) return false;

  soma = 0;
  for (let i = 0; i < 13; i++) {
    soma += parseInt(digitos.charAt(i), 10) * pesos2[i];
  }
  resto = soma % 11;
  const digito2 = resto < 2 ? 0 : 11 - resto;
  return digito2 === parseInt(digitos.charAt(13), 10);
}

export function validarCpfOuCnpj(valor: string): boolean {
  const digitos = somenteDigitos(valor);
  if (digitos.length === 11) return ehCpfValido(digitos);
  if (digitos.length === 14) return ehCnpjValido(digitos);
  return false;
}

export function formatarCpfCnpjInput(valor: string): string {
  const digitos = somenteDigitos(valor).slice(0, 14);
  if (digitos.length <= 11) {
    return digitos
      .replace(/^(\d{3})(\d)/, "$1.$2")
      .replace(/^(\d{3})\.(\d{3})(\d)/, "$1.$2.$3")
      .replace(/^(\d{3})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3-$4");
  }
  return digitos
    .replace(/^(\d{2})(\d)/, "$1.$2")
    .replace(/^(\d{2})\.(\d{3})(\d)/, "$1.$2.$3")
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})(\d)/, "$1.$2.$3/$4")
    .replace(/^(\d{2})\.(\d{3})\.(\d{3})\/(\d{4})(\d)/, "$1.$2.$3/$4-$5");
}

export function formatarTelefoneInput(valor: string): string {
  const digitos = somenteDigitos(valor).slice(0, 11);
  if (digitos.length === 0) return "";
  if (digitos.length <= 2) return `(${digitos}`;
  if (digitos.length <= 6) return `(${digitos.slice(0, 2)}) ${digitos.slice(2)}`;
  if (digitos.length <= 10) {
    return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 6)}-${digitos.slice(6)}`;
  }
  return `(${digitos.slice(0, 2)}) ${digitos.slice(2, 7)}-${digitos.slice(7)}`;
}

export async function carregarOpcoesPublicas(): Promise<SolicitacaoPublicaOpcoes> {
  const { data } = await coreApi.get<SolicitacaoPublicaOpcoes>(ENDPOINT_OPCOES);
  return {
    categoriasProjeto: (data?.categoriasProjeto ?? []).sort((a, b) =>
      a.nome.localeCompare(b.nome, "pt-BR")
    ),
    idiomas: (data?.idiomas ?? []).sort((a, b) =>
      a.nome.localeCompare(b.nome, "pt-BR")
    ),
  };
}

export function montarFormDataPublico(
  values: SolicitacaoPublicaFormValues,
  arquivos: ArquivoSelecionado[]
): FormData {
  const form = new FormData();
  form.append("nomeEmpresa", values.nomeEmpresa.trim());
  form.append("nomeRepresentante", values.nomeRepresentante.trim());
  form.append("emailRepresentante", values.emailRepresentante.trim());
  
  if (values.telefone && values.telefone.trim()) {
    form.append("telefone", values.telefone.trim());
  }

  form.append("cpfCnpj", values.cpfCnpj.trim());
  form.append("categoriaProjetoId", values.categoriaProjetoId);
  form.append("idiomaOrigemId", values.idiomaOrigemId);
  form.append("idiomaDestinoId", values.idiomaDestinoId);

  if (values.descricao && values.descricao.trim()) {
    form.append("descricao", values.descricao.trim());
  }

  for (const arquivo of arquivos) {
    if (Platform.OS === "web" && arquivo.file) {
      form.append("arquivos", arquivo.file, arquivo.nome);
    } else {
      form.append("arquivos", {
        uri: arquivo.uri,
        name: arquivo.nome,
        type: arquivo.tipo,
      } as unknown as Blob);
    }
  }

  return form;
}

export async function enviarSolicitacaoPublica(
  values: SolicitacaoPublicaFormValues,
  arquivos: ArquivoSelecionado[]
): Promise<SolicitacaoPublicaCriadaResponse> {
  const body = montarFormDataPublico(values, arquivos);
  const { data } = await coreApi.post<SolicitacaoPublicaCriadaResponse>(
    ENDPOINT_SOLICITACOES,
    body
  );
  return data;
}

export function extrairErrosPublicos(error: unknown): ErroCampoPublico[] {
  if (!axios.isAxiosError(error)) return [];
  const erros: unknown = error.response?.data?.erros;
  if (!Array.isArray(erros)) return [];

  return erros.filter(
    (erro): erro is ErroCampoPublico =>
      typeof erro?.campo === "string" && typeof erro?.mensagem === "string"
  );
}

export { getApiErrorMessage };
