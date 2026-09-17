// types.ts
export interface ColumnDef<T> {
  key: keyof T;
  header: string;
  render?: (item: T) => React.ReactNode;
  width?: number;         // ex: 160 → vira className "w-[160px]"
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
}