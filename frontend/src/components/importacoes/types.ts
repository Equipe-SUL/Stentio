import type { DocumentPickerAsset } from "expo-document-picker";

export type ArquivoCsv = DocumentPickerAsset;
export type Separador = "," | ";";
export type EtapaImportacao =
  "arquivo" | "validando" | "revisao" | "importando" | "resultado";

export interface ConfiguracaoImportacao {
  max_upload_bytes: number;
  max_recursos: number;
  max_linhas: number;
}

export interface RecursoPrevia {
  nome: string;
  email: string;
  telefone: string | null;
  ativo: boolean;
  servicos: string[];
}

export interface PreviaImportacao {
  status: "Validado";
  filename: string;
  total_linhas: number;
  total_recursos: number;
  total_servicos: number;
  total_vinculos: number;
  amostra_recursos: RecursoPrevia[];
}

export interface RelatorioImportacao {
  status: "Concluido";
  filename: string;
  total_linhas: number;
  total_recursos: number;
  total_servicos: number;
  recursos_criados: number;
  recursos_reutilizados: number;
  servicos_criados: number;
  servicos_reutilizados: number;
  vinculos_criados: number;
  vinculos_existentes: number;
}

export interface FalhaImportacao {
  mensagem: string;
  codigo?: string;
  linha?: number;
  campo?: string;
  resultado?: "nao_gravado" | "indeterminado";
}
