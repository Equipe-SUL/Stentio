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
  form.append("separador", separador);
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
    "/upload-csv/validar",
    montarFormularioCsv(arquivo, separador),
    {
      onUploadProgress: ({ loaded, total }) => {
        if (total) progresso(Math.min(100, Math.round((loaded / total) * 100)));
      },
    },
  );
  return conferirPrevia(data);
}

export async function importarRecursos(
  arquivo: ArquivoCsv,
  separador: Separador,
): Promise<RelatorioImportacao> {
  const { data } = await importApi.post<RelatorioImportacao>(
    "/upload-csv",
    montarFormularioCsv(arquivo, separador),
    // Aguarda a confirmação da transação inteira. Não repete gravações automaticamente.
    { timeout: 0 },
  );
  return conferirRelatorio(data);
}

const AVISO_INDETERMINADO =
  " O resultado da gravação não pôde ser confirmado. Confira os cadastros antes de reenviar.";

export function extrairFalhaImportacao(
  error: unknown,
  gravando = false,
): FalhaImportacao {
  if (isAxiosError(error)) {
    const detail = error.response?.data?.detail;
    const status = error.response?.status;
    let mensagem =
      typeof detail === "string"
        ? detail
        : typeof detail?.mensagem === "string"
          ? detail.mensagem
          : "";
    if (!mensagem) {
      mensagem =
        status === 401
          ? "Sua sessão expirou. Entre novamente no Stentio."
          : status === 403
            ? "Você não tem permissão para importar recursos."
            : status === 413
              ? "O arquivo ultrapassa o limite de upload do servidor."
              : status === 409
                ? "O arquivo está em conflito com os cadastros existentes."
                : status === 503
                  ? "O serviço não conseguiu confirmar a importação no banco."
                  : !error.response
                    ? "Não foi possível obter uma resposta do serviço de importação."
                    : `Não foi possível concluir a solicitação (HTTP ${status}).`;
    }
    const resultado = !gravando
      ? undefined
      : [400, 401, 403, 409, 413, 422].includes(status ?? 0)
        ? "nao_gravado"
        : "indeterminado";
    if (resultado === "indeterminado") mensagem += AVISO_INDETERMINADO;
    return {
      mensagem,
      resultado,
      codigo: typeof detail?.codigo === "string" ? detail.codigo : undefined,
      linha: typeof detail?.linha === "number" ? detail.linha : undefined,
      campo: typeof detail?.campo === "string" ? detail.campo : undefined,
    };
  }
  return {
    mensagem:
      (error instanceof Error
        ? error.message
        : "Não foi possível concluir a solicitação.") +
      (gravando ? AVISO_INDETERMINADO : ""),
    resultado: gravando ? "indeterminado" : undefined,
  };
}

function numero(value: unknown): value is number {
  return Number.isInteger(value) && (value as number) >= 0;
}

export function conferirPrevia(data: PreviaImportacao): PreviaImportacao {
  if (
    data?.status !== "Validado" ||
    typeof data.filename !== "string" ||
    ![
      data.total_linhas,
      data.total_recursos,
      data.total_servicos,
      data.total_vinculos,
    ].every(numero) ||
    data.total_recursos < 1 ||
    !Array.isArray(data.amostra_recursos) ||
    !data.amostra_recursos.every(
      (item) =>
        item &&
        typeof item.nome === "string" &&
        typeof item.email === "string" &&
        typeof item.ativo === "boolean" &&
        (item.telefone === null || typeof item.telefone === "string") &&
        Array.isArray(item.servicos) &&
        item.servicos.every((s) => typeof s === "string"),
    )
  ) {
    throw new Error(
      "A resposta de validação não corresponde ao contrato de recursos e serviços. Confira a versão do Import Service.",
    );
  }
  return data;
}

export function conferirRelatorio(
  data: RelatorioImportacao,
): RelatorioImportacao {
  if (
    data?.status !== "Concluido" ||
    typeof data.filename !== "string" ||
    ![
      data.total_linhas,
      data.total_recursos,
      data.total_servicos,
      data.recursos_criados,
      data.recursos_reutilizados,
      data.servicos_criados,
      data.servicos_reutilizados,
      data.vinculos_criados,
      data.vinculos_existentes,
    ].every(numero) ||
    data.total_recursos < 1 ||
    data.recursos_criados + data.recursos_reutilizados !==
      data.total_recursos ||
    data.servicos_criados + data.servicos_reutilizados !== data.total_servicos
  ) {
    throw new Error(
      "O serviço respondeu fora do contrato esperado para a confirmação da importação.",
    );
  }
  return data;
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
