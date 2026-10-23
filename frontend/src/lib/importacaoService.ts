import { create, isAxiosError } from "axios";
import { Platform } from "react-native";
import { getToken } from "./auth";
import type {
  ArquivoCsv,
  ConfiguracaoImportacao,
  FalhaImportacao,
  PreviaImportacao,
  RelatorioImportacao,
  Separador,
} from "../components/importacoes/types";

function resolverUrl(): string {
  const configurada =
    process.env.EXPO_PUBLIC_IMPORT_API_URL ??
    (Platform.OS === "android"
      ? "http://10.0.2.2:8000"
      : "http://localhost:8000");
  // Segue os clients de Auth/Core: o cookie HttpOnly pertence ao host da página.
  if (Platform.OS === "web" && typeof window !== "undefined") {
    try {
      const url = new URL(configurada, window.location.origin);
      url.hostname = window.location.hostname;
      return url.toString().replace(/\/$/, "");
    } catch {
      /* O client apresentará o erro de conexão. */
    }
  }
  return configurada;
}

export const importApi = create({
  baseURL: resolverUrl(),
  withCredentials: true,
  timeout: 60000,
  headers: { "X-Stentio-Import": "1" },
});

importApi.interceptors.request.use(async (config) => {
  const token = await getToken();
  if (token) config.headers.Authorization = `Bearer ${token}`;
  return config;
});

export function montarFormularioCsv(
  arquivo: ArquivoCsv,
  separador: Separador,
): FormData {
  const form = new FormData();
  if (Platform.OS === "web") {
    if (!arquivo.file)
      throw new Error("Selecione o arquivo novamente para continuar.");
    form.append("file", arquivo.file, arquivo.name);
  } else {
    form.append("file", {
      uri: arquivo.uri,
      name: arquivo.name,
      type: arquivo.mimeType ?? "text/csv",
    } as unknown as Blob);
  }
  if (separador !== "auto") form.append("separador", separador);
  return form;
}

export async function carregarConfiguracao(): Promise<ConfiguracaoImportacao> {
  const { data } = await importApi.get<ConfiguracaoImportacao>(
    "/api/v1/importacoes/configuracao",
  );
  if (
    ![data.max_upload_bytes, data.max_recursos, data.max_linhas].every(
      (n) => Number.isInteger(n) && n > 0,
    )
  ) {
    throw new Error(
      "O serviço retornou limites inválidos. Tente carregar novamente.",
    );
  }
  return data;
}

export async function validarRecursos(
  arquivo: ArquivoCsv,
  separador: Separador,
  progresso: (valor: number) => void,
): Promise<PreviaImportacao> {
  const { data } = await importApi.post<PreviaImportacao>(
    "/api/v1/importacoes/recursos/validar",
    montarFormularioCsv(arquivo, separador),
    {
      onUploadProgress: ({ loaded, total }) => {
        if (total) progresso(Math.min(100, Math.round((loaded / total) * 100)));
      },
    },
  );
  return data;
}

export async function importarRecursos(
  arquivo: ArquivoCsv,
  separador: Separador,
): Promise<RelatorioImportacao> {
  const { data } = await importApi.post<RelatorioImportacao>(
    "/api/v1/importacoes/recursos",
    montarFormularioCsv(arquivo, separador),
    // O Core processa sequencialmente. Um timeout curto perde o relatório de um
    // lote ainda em execução. Não há retry automático de chamadas de gravação.
    { timeout: 0 },
  );
  return data;
}

export function extrairFalhaImportacao(
  error: unknown,
  gravando = false,
): FalhaImportacao {
  if (isAxiosError(error)) {
    const body = error.response?.data;
    const detail = body?.detail;
    const falha: FalhaImportacao = {
      mensagem:
        typeof detail === "string"
          ? detail
          : typeof detail?.mensagem === "string"
            ? detail.mensagem
            : "",
      codigo: typeof detail?.codigo === "string" ? detail.codigo : undefined,
      linha: typeof detail?.linha === "number" ? detail.linha : undefined,
      campo: typeof detail?.campo === "string" ? detail.campo : undefined,
      relatorio: body?.relatorio,
    };
    if (!falha.mensagem) {
      const status = error.response?.status;
      falha.mensagem =
        status === 401
          ? "Sua sessão expirou. Entre novamente no Stentio."
          : status === 403
            ? "Você não tem permissão para importar recursos."
            : status === 413
              ? "O arquivo ultrapassa o limite de upload do servidor."
              : !error.response
                ? "Não foi possível obter uma resposta do serviço de importação."
                : `Não foi possível concluir a solicitação (HTTP ${status}).`;
    }
    if (
      gravando &&
      !falha.relatorio &&
      ![400, 401, 403, 413, 422].includes(error.response?.status ?? 0)
    ) {
      falha.mensagem +=
        " O resultado da gravação é indeterminado. Confira os recursos no sistema antes de reenviar.";
    }
    return falha;
  }
  return {
    mensagem:
      error instanceof Error
        ? error.message
        : "Não foi possível concluir a solicitação.",
  };
}

export function verificarArquivo(
  arquivo: ArquivoCsv,
  limite: number,
): string | null {
  if (!arquivo.name.toLowerCase().endsWith(".csv"))
    return "Selecione um arquivo com extensão .csv.";
  const tamanho = arquivo.file?.size ?? arquivo.size;
  if (tamanho === 0)
    return "O arquivo está vazio. Selecione um CSV com registros.";
  if (tamanho !== undefined && tamanho > limite)
    return `O arquivo ultrapassa o limite de ${formatarBytes(limite)}.`;
  return null;
}

export function formatarBytes(bytes?: number): string {
  if (bytes === undefined) return "Tamanho informado na validação";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 ** 2) return `${(bytes / 1024).toFixed(1)} KiB`;
  return `${(bytes / 1024 ** 2).toFixed(1)} MiB`;
}
