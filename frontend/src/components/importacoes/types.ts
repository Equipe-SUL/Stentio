import type { DocumentPickerAsset } from "expo-document-picker";

export type ArquivoCsv = DocumentPickerAsset;
export type Separador = "auto" | "," | ";";
export type EtapaImportacao =
  "arquivo" | "validando" | "revisao" | "importando" | "resultado";

export interface ConfiguracaoImportacao {
  max_upload_bytes: number;
  max_recursos: number;
  max_linhas: number;
}

export interface PreviaImportacao {
  filename: string;
  total_linhas: number;
  total_recursos: number;
  validacao: "LOCAL";
  amostra_recursos: {
    linhas: number[];
    dados: {
      nome: string;
      email: string;
      telefone: string | null;
      tiposServicoIds: string[];
      precos: {
        idiomaOrigemId: string;
        idiomaDestinoId: string;
        unidade: "PALAVRA" | "HORA";
        valor: string;
      }[];
    };
  }[];
}

export type StatusRecurso =
  | "IMPORTADO"
  | "CONFLITO"
  | "REJEITADO"
  | "INDETERMINADO"
  | "NAO_PROCESSADO"
  | "FALHA_CONEXAO";
export interface ResultadoRecurso {
  email: string;
  linhas: number[];
  status: StatusRecurso;
  mensagem?: string;
  id?: string;
}

export interface RelatorioImportacao {
  filename: string;
  status:
    | "CONCLUIDA"
    | "CONCLUIDA_COM_CONFLITOS"
    | "PARCIAL"
    | "FALHOU"
    | "INDETERMINADA";
  total_linhas: number;
  total_recursos: number;
  importados: number;
  conflitos: number;
  rejeitados: number;
  indeterminados: number;
  nao_processados: number;
  falhas_conexao: number;
  resultados: ResultadoRecurso[];
}

export interface FalhaImportacao {
  mensagem: string;
  codigo?: string;
  linha?: number;
  campo?: string;
  relatorio?: RelatorioImportacao;
}
