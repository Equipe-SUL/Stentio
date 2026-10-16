import axios from "axios";
import { Platform } from "react-native";
import { coreApi } from "../../lib/api";
import type { SolicitacaoFormValues } from "./solicitacaoSchema";
import type {
  ArquivoSelecionado,
  CategoriaProjetoOpcao,
  ClienteOpcao,
  ErroCampoApi,
  IdiomaOpcao,
  OpcoesSolicitacao,
  SolicitacaoResponse,
} from "./types";

// Rotas do microsserviço Core (porta 8082)
const ENDPOINTS = {
  clientes: "/api/v1/clientes",
  categorias: "/api/v1/categorias-projeto",
  idiomas: "/api/v1/idiomas",
  solicitacoes: "/api/v1/solicitacoes",
} as const;

// Alinhado ao spring.data.web.pageable.max-page-size do Core (100 itens).
const TAMANHO_PAGINA_OPCOES = 100;

// Os GETs de apoio podem devolver um array puro ou uma página { content }.
function extrairLista<T>(data: unknown): T[] {
  if (Array.isArray(data)) return data as T[];
  const content = (data as { content?: unknown } | null)?.content;
  return Array.isArray(content) ? (content as T[]) : [];
}

async function buscarOpcoes<T extends { ativo?: boolean }>(url: string): Promise<T[]> {
  const { data } = await coreApi.get(url, { params: { size: TAMANHO_PAGINA_OPCOES } });
  return extrairLista<T>(data).filter((item) => item.ativo !== false);
}

export async function carregarOpcoesSolicitacao(): Promise<OpcoesSolicitacao> {
  const [clientes, categorias, idiomas] = await Promise.all([
    buscarOpcoes<ClienteOpcao>(ENDPOINTS.clientes),
    buscarOpcoes<CategoriaProjetoOpcao>(ENDPOINTS.categorias),
    buscarOpcoes<IdiomaOpcao>(ENDPOINTS.idiomas),
  ]);

  return {
    clientes: [...clientes].sort((a, b) => a.nomeEmpresa.localeCompare(b.nomeEmpresa, "pt-BR")),
    categorias: [...categorias].sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR")),
    idiomas: [...idiomas].sort((a, b) => a.nome.localeCompare(b.nome, "pt-BR")),
  };
}

// CA.5: o contrato é multipart/form-data. Campos de texto com o nome exato do
// SolicitacaoRequest e anexos todos na parte "arquivos" (repetida, uma por arquivo).
function montarFormData(values: SolicitacaoFormValues, arquivos: ArquivoSelecionado[]): FormData {
  const form = new FormData();
  form.append("clienteId", values.clienteId);
  form.append("categoriaProjetoId", values.categoriaProjetoId);
  form.append("idiomaOrigemId", values.idiomaOrigemId);
  form.append("idiomaDestinoId", values.idiomaDestinoId);

  const descricao = values.descricao.trim();
  if (descricao) form.append("descricao", descricao);

  for (const arquivo of arquivos) {
    if (Platform.OS === "web" && arquivo.file) {
      // Na web o FormData espera um File/Blob de verdade.
      form.append("arquivos", arquivo.file, arquivo.nome);
    } else {
      // No React Native o FormData aceita este objeto { uri, name, type }.
      form.append("arquivos", {
        uri: arquivo.uri,
        name: arquivo.nome,
        type: arquivo.tipo,
      } as unknown as Blob);
    }
  }

  return form;
}

export async function criarSolicitacao(
  values: SolicitacaoFormValues,
  arquivos: ArquivoSelecionado[],
): Promise<SolicitacaoResponse> {
  // Não definimos Content-Type: o axios/RN montam o boundary do multipart sozinhos.
  const { data } = await coreApi.post<SolicitacaoResponse>(
    ENDPOINTS.solicitacoes,
    montarFormData(values, arquivos),
  );
  return data;
}

// Lê `erros[].campo` / `erros[].mensagem` da resposta 400 do backend.
export function extrairErrosDeCampo(error: unknown): ErroCampoApi[] {
  if (!axios.isAxiosError(error)) return [];
  const erros: unknown = error.response?.data?.erros;
  if (!Array.isArray(erros)) return [];

  return erros.filter(
    (erro): erro is ErroCampoApi =>
      typeof erro?.campo === "string" && typeof erro?.mensagem === "string",
  );
}

export async function listarSolicitacoes(): Promise<SolicitacaoResponse[]> {
  const { data } = await coreApi.get<{ content: SolicitacaoResponse[] }>(
    ENDPOINTS.solicitacoes,
    {
      params: { size: TAMANHO_PAGINA_OPCOES, sort: "dataRecebimento,desc" },
    },
  );
  return extrairLista<SolicitacaoResponse>(data);
}

export async function baixarArquivoSolicitacao(url: string, nomeArquivo: string): Promise<void> {
  if (Platform.OS === "web" && typeof window !== "undefined") {
    const { data } = await coreApi.get(url, { responseType: "blob" });
    const blobUrl = window.URL.createObjectURL(new Blob([data]));
    const link = document.createElement("a");
    link.href = blobUrl;
    link.setAttribute("download", nomeArquivo);
    document.body.appendChild(link);
    link.click();
    link.remove();
    window.URL.revokeObjectURL(blobUrl);
  }
}