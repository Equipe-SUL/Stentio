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

// Uma opção de um campo "select". O formulário guarda o value e a tabela
// exibe o label, então referências externas (UUID) aparecem legíveis.
export interface OpcaoSelect {
  value: string;
  label: string;
}

export type TipoCampo = "text" | "boolean" | "iso" | "select" | "textarea";

// Descreve um campo editável de forma genérica, usado tanto para gerar
// as colunas da tabela quanto os campos do formulário de criação/edição.
export interface FieldDef<T> {
  key: keyof T;
  label: string;
  type: TipoCampo;
  placeholder?: string;
  // Por padrão todo campo é obrigatório. Use `false` para os que o
  // backend aceita vazios (ex.: telefone, opcional no cadastro de cliente).
  required?: boolean;
  // Obrigatório quando type = "select".
  options?: OpcaoSelect[];
  // Altura em linhas quando type = "textarea".
  linhas?: number;
}

// Filtros de status usados na listagem do Catálogo de Referenciais.
export type FiltroStatus = "todos" | "ativos" | "inativos";

export interface FiltroLista {
  nome?: string;
  ativo?: boolean;
}