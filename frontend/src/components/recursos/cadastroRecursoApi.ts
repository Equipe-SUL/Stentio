import { isAxiosError } from "axios";
import { api, coreApi } from "../../lib/api";

export interface OpcaoCatalogoRecurso {
  id: string;
  nome: string;
  codigoIso?: string;
}

export type UnidadePrecoRecurso = "PALAVRA" | "HORA" | "PAGINA";

export interface PrecoCadastroRecurso {
  idiomaOrigemId: string;
  idiomaDestinoId: string;
  unidade: UnidadePrecoRecurso;
  valor: number;
}

export interface AutoCadastroRecursoRequest {
  nome: string;
  email: string;
  telefone: string;
  tiposServicoIds: string[];
  precos: PrecoCadastroRecurso[];
}

export interface PerfilRecursoCriado {
  id: string;
  nome: string;
  email: string;
  telefone: string;
  ativo: boolean;
  verificado: boolean;
  tiposServico: OpcaoCatalogoRecurso[];
  precos: {
    idiomaOrigem: OpcaoCatalogoRecurso;
    idiomaDestino: OpcaoCatalogoRecurso;
    unidade: UnidadePrecoRecurso;
    valor: number;
  }[];
}

export interface ErroCampoCadastro {
  campo: string;
  mensagem: string;
}

export function errosDeCadastro(error: unknown): {
  campos: Record<string, string>;
  geral: string;
} {
  if (!isAxiosError(error)) {
    return { campos: {}, geral: "Não foi possível concluir o cadastro. Tente novamente." };
  }

  const status = error.response?.status;
  const data = error.response?.data as
    | { message?: string | string[]; erros?: ErroCampoCadastro[] }
    | undefined;
  const campos: Record<string, string> = {};

  for (const item of data?.erros ?? []) {
    campos[item.campo] = item.mensagem;
  }

  if (status === 409) {
    campos.email = "Este e-mail já está cadastrado. Entre na sua conta para completar o perfil.";
    return { campos, geral: "" };
  }

  if (Array.isArray(data?.message)) {
    for (const mensagem of data.message) {
      const separador = mensagem.indexOf(":");
      if (separador > 0) {
        campos[mensagem.slice(0, separador)] = mensagem.slice(separador + 1).trim();
      }
    }
  }

  const geral =
    typeof data?.message === "string"
      ? data.message
      : status === 403
        ? "Os catálogos de competências estão indisponíveis para este perfil. Tente novamente mais tarde."
        : "Confira os campos destacados e tente novamente.";
  return { campos, geral };
}

export async function criarContaRecurso(dados: {
  nome: string;
  email: string;
  senha: string;
}): Promise<void> {
  await api.post("/api/v1/public/usuarios", dados);
}

async function listarCatalogo<T>(endpoint: string): Promise<T[]> {
  const { data } = await coreApi.get<{ content?: T[] } | T[]>(endpoint, {
    params: { ativo: true, size: 100, sort: "nome,asc" },
  });
  if (Array.isArray(data)) return data;
  return data.content ?? [];
}

export async function carregarCatalogosCadastroRecurso(): Promise<{
  tiposServico: OpcaoCatalogoRecurso[];
  idiomas: OpcaoCatalogoRecurso[];
}> {
  const [tiposServico, idiomas] = await Promise.all([
    listarCatalogo<OpcaoCatalogoRecurso>("/api/v1/tipos-servico"),
    listarCatalogo<OpcaoCatalogoRecurso>("/api/v1/idiomas"),
  ]);
  return { tiposServico, idiomas };
}

export async function enviarPerfilRecurso(
  dados: AutoCadastroRecursoRequest,
): Promise<PerfilRecursoCriado> {
  const { data } = await coreApi.post<PerfilRecursoCriado>(
    "/api/v1/recursos/auto-cadastro",
    dados,
  );
  return data;
}
