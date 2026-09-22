// types.ts
export interface ColumnDef<T> {
  key: keyof T;
  header: string;
  render?: (item: T) => React.ReactNode;
  width?: number;
  align?: "left" | "right";
  hideOnMobile?: boolean;
}

export interface DataTableProps<T> {
  data: T[];
  columns: ColumnDef<T>[];
  keyExtractor: (item: T) => string;
  onRowPress?: (item: T) => void;
  emptyMessage?: string;
  scrollEnabled?: boolean;
  mobileBreakpoint?: number; // novo — cada tabela pode ter seu próprio limiar
}

// Descreve um campo editável de forma genérica, usado tanto para gerar
// as colunas da tabela quanto os campos do formulário de criação/edição.
export interface FieldDef<T> {
  key: keyof T;
  label: string;
  type: "text" | "boolean" | "iso";
  placeholder?: string;
}

// Filtros de status usados na listagem do Catálogo de Referenciais.
export type FiltroStatus = "todos" | "ativos" | "inativos";

export interface FiltroLista {
  nome?: string;
  ativo?: boolean;
}