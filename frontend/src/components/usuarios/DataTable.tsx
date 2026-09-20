import { FlatList, Pressable, Text, useWindowDimensions, View } from "react-native";
import type { ColumnDef, DataTableProps } from "./types";

const MOBILE_BREAKPOINT = 768;

function cellStyle(width?: number) {
  if (width) {
    return {
      width,
      flexGrow: 0,
      flexShrink: 0,
    };
  }

  return {
    flex: 1,
    minWidth: 0,
  };
}

function renderCellValue<T>(item: T, column: ColumnDef<T>) {
  if (column.render) {
    return column.render(item);
  }

  return (
    <Text className="text-sm text-neutral-800" numberOfLines={1}>
      {String(item[column.key])}
    </Text>
  );
}

function getAlignmentClass(align?: ColumnDef<unknown>["align"]) {
  return align === "right" ? "items-end" : "items-start";
}

export function DataTable<T>({
  data,
  columns,
  keyExtractor,
  onRowPress,
  emptyMessage = "Nenhum registro encontrado",
  scrollEnabled = true,
}: DataTableProps<T>) {
  const { width } = useWindowDimensions();
  const isMobile = width < MOBILE_BREAKPOINT;

  if (data.length === 0) {
    return (
      <View className="items-center justify-center rounded-2xl border border-neutral-200 bg-white px-6 py-16">
        <Text className="text-sm text-neutral-400">{emptyMessage}</Text>
      </View>
    );
  }

  return (
    <View className="overflow-hidden rounded-2xl border border-neutral-200 bg-white">
      {!isMobile && (
        <View className="flex-row items-center border-b border-neutral-200 bg-neutral-50 px-6 py-4">
          {columns.map((column) => (
            <View
              key={String(column.key)}
              style={cellStyle(column.width)}
              className={getAlignmentClass(column.align)}
            >
              <Text className="text-xs font-semibold uppercase tracking-wide text-neutral-500">
                {column.header}
              </Text>
            </View>
          ))}
        </View>
      )}

      <FlatList
        data={data}
        keyExtractor={keyExtractor}
        scrollEnabled={scrollEnabled}
        initialNumToRender={data.length}
        renderItem={({ item }) =>
          isMobile ? (
            <CardRow item={item} columns={columns} onPress={onRowPress} />
          ) : (
            <TableRow item={item} columns={columns} onPress={onRowPress} />
          )
        }
        ItemSeparatorComponent={() => <View className="h-px bg-neutral-200" />}
        showsVerticalScrollIndicator={false}
      />
    </View>
  );
}

function TableRow<T>({
  item,
  columns,
  onPress,
}: {
  item: T;
  columns: ColumnDef<T>[];
  onPress?: (item: T) => void;
}) {
  return (
    <Pressable
      onPress={() => onPress?.(item)}
      className="flex-row items-center px-6 py-4 active:bg-neutral-50"
    >
      {columns.map((column) => (
        <View
          key={String(column.key)}
          style={cellStyle(column.width)}
          className={getAlignmentClass(column.align)}
        >
          {renderCellValue(item, column)}
        </View>
      ))}
    </Pressable>
  );
}

function CardRow<T>({
  item,
  columns,
  onPress,
}: {
  item: T;
  columns: ColumnDef<T>[];
  onPress?: (item: T) => void;
}) {
  const visibleColumns = columns.filter((column) => !column.hideOnMobile);

  return (
    <Pressable
      onPress={() => onPress?.(item)}
      className="gap-3 px-5 py-4 active:bg-neutral-50"
    >
      {visibleColumns.map((column) => (
        <View
          key={String(column.key)}
          className="flex-row items-center justify-between"
        >
          <Text className="text-xs font-medium uppercase tracking-wide text-neutral-500">
            {column.header}
          </Text>

          <View className="ml-4 shrink items-end">
            {renderCellValue(item, column)}
          </View>
        </View>
      ))}
    </Pressable>
  );
}
