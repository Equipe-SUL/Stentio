// CrudSection.tsx
import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, View, Text, Pressable } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { DataTable } from "./DataTable";
import { RowActions } from "./RowActions";
import { EntityFormModal } from "./EntityFormModal";
import { ConfirmDialog } from "./ConfirmDialog";
import { StatusBadge } from "./Badge";
import type { ColumnDef, FieldDef } from "./types";

interface EntityBase {
  id: string;
}

interface CrudSectionProps<T extends EntityBase> {
  title: string;
  createTitle: string;
  editTitle: string;
  fields: FieldDef<T>[];
  defaultValues: Omit<T, "id">;
  fetchList: () => Promise<T[]>;
  createItem: (values: Omit<T, "id">) => Promise<T>;
  updateItem: (id: string, values: Omit<T, "id">) => Promise<void>;
  deleteItem: (id: string) => Promise<void>;
  updateStatus?: (id: string, ativo: boolean) => Promise<void>;
  deleteMessage?: string;
  emptyMessage?: string;
}

function getApiErrorMessage(error: unknown): string {
  if (error instanceof Error) return error.message;
  return "Ocorreu um erro inesperado. Tente novamente.";
}

function renderFieldValue<T>(item: T, field: FieldDef<T>) {
  const valor = item[field.key];

  if (field.type === "boolean") {
    return <StatusBadge status={valor ? "Ativo" : "Inativo"} />;
  }

  if (field.type === "iso") {
    return (
      <Text className="font-mono text-xs uppercase text-neutral-600" numberOfLines={1}>
        {String(valor)}
      </Text>
    );
  }

  return (
    <Text className="text-sm text-neutral-800" numberOfLines={1}>
      {String(valor)}
    </Text>
  );
}

export function CrudSection<T extends EntityBase>({
  title,
  createTitle,
  editTitle,
  fields,
  defaultValues,
  fetchList,
  createItem,
  updateItem,
  deleteItem,
  updateStatus,
  deleteMessage = "Tem certeza que deseja excluir este registro? Essa ação não pode ser desfeita.",
  emptyMessage = "Nenhum registro cadastrado",
}: CrudSectionProps<T>) {
  const [itens, setItens] = useState<T[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erroLista, setErroLista] = useState("");

  const [mostrarCriar, setMostrarCriar] = useState(false);
  const [itemEmEdicao, setItemEmEdicao] = useState<T | null>(null);
  const [itemParaExcluir, setItemParaExcluir] = useState<T | null>(null);

  const [erroCriacao, setErroCriacao] = useState("");
  const [erroEdicao, setErroEdicao] = useState("");
  const [erroAcao, setErroAcao] = useState("");

  const recarregar = useCallback(() => {
    setCarregando(true);
    setErroLista("");
    return fetchList()
      .then(setItens)
      .catch((error) => setErroLista(getApiErrorMessage(error)))
      .finally(() => setCarregando(false));
  }, [fetchList]);

  useEffect(() => {
    let ativo = true;
    fetchList()
      .then((lista) => {
        if (ativo) setItens(lista);
      })
      .catch((error) => {
        if (ativo) setErroLista(getApiErrorMessage(error));
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, [fetchList]);

  async function handleCreate(values: T): Promise<boolean> {
    setErroCriacao("");
    try {
      const { id: _ignorado, ...dados } = values;
      const criado = await createItem(dados as Omit<T, "id">);
      if (updateStatus && "ativo" in values && values.ativo === false) {
        await updateStatus(criado.id, false);
      }
      setMostrarCriar(false);
      await recarregar();
      return true;
    } catch (error) {
      setErroCriacao(getApiErrorMessage(error));
      return false;
    }
  }

  async function handleUpdate(values: T): Promise<boolean> {
    if (!itemEmEdicao) return false;

    setErroEdicao("");
    try {
      const { id: _ignorado, ...dados } = values;
      await updateItem(itemEmEdicao.id, dados as Omit<T, "id">);
      if (
        updateStatus &&
        "ativo" in values &&
        "ativo" in itemEmEdicao &&
        Boolean(itemEmEdicao.ativo) !== Boolean(values.ativo)
      ) {
        await updateStatus(itemEmEdicao.id, Boolean(values.ativo));
      }
      setItemEmEdicao(null);
      await recarregar();
      return true;
    } catch (error) {
      setErroEdicao(getApiErrorMessage(error));
      return false;
    }
  }

  function handleToggleStatus(item: T) {
    if (!updateStatus) return;

    setErroAcao("");
    updateStatus(item.id, !Boolean((item as unknown as { ativo?: boolean }).ativo))
      .then(recarregar)
      .catch((error) => setErroAcao(getApiErrorMessage(error)));
  }

  function handleSolicitarExclusao() {
    if (!itemEmEdicao) return;
    setItemParaExcluir(itemEmEdicao);
    setItemEmEdicao(null);
  }

  async function handleConfirmarExclusao() {
    if (!itemParaExcluir) return;

    setErroAcao("");
    try {
      await deleteItem(itemParaExcluir.id);
      setItemParaExcluir(null);
      await recarregar();
    } catch (error) {
      setItemParaExcluir(null);
      setErroAcao(getApiErrorMessage(error));
    }
  }

  const columns: ColumnDef<T>[] = [
    ...fields.map((field) => ({
      key: field.key,
      header: field.label,
      render: (item: T) => renderFieldValue(item, field),
    })),
    {
      key: "id" as keyof T,
      header: "",
      width: 92,
      align: "right",
      render: (item: T) => (
        <RowActions
          onEdit={() => {
            setErroEdicao("");
            setItemEmEdicao(item);
          }}
          onToggle={
            updateStatus
              ? () => handleToggleStatus(item)
              : undefined
          }
          toggleLabel={
            updateStatus
              ? Boolean((item as unknown as { ativo?: boolean }).ativo)
                ? "Desativar"
                : "Ativar"
              : undefined
          }
        />
      ),
    },
  ];

  return (
    <View className="flex-1 min-w-[260px] gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm">
      <View className="flex-row items-center justify-between gap-2">
        <Text className="text-lg font-bold text-neutral-900">{title}</Text>

        <Pressable
          onPress={() => {
            setErroCriacao("");
            setMostrarCriar(true);
          }}
          className="flex-row items-center gap-1 rounded-lg bg-[#6f4f28] px-3 py-2"
        >
          <Ionicons name="add" size={16} color="#ffffff" />
          <Text className="text-xs font-medium text-white">Novo</Text>
        </Pressable>
      </View>

      {erroAcao ? (
        <View className="rounded-lg border border-red-200 bg-red-50 p-3">
          <Text className="text-sm font-medium text-red-600">{erroAcao}</Text>
        </View>
      ) : null}

      {carregando ? (
        <View className="items-center justify-center gap-3 py-10">
          <ActivityIndicator color="#6f4f28" />
          <Text className="text-sm text-neutral-500">Carregando...</Text>
        </View>
      ) : erroLista ? (
        <View className="items-center justify-center gap-3 py-10">
          <Text className="text-center text-sm text-red-600">{erroLista}</Text>
          <Pressable onPress={recarregar} className="rounded-lg border border-neutral-300 px-4 py-2">
            <Text className="text-sm font-medium text-neutral-700">Tentar de novo</Text>
          </Pressable>
        </View>
      ) : (
        <DataTable
          data={itens}
          columns={columns}
          keyExtractor={(item) => item.id}
          emptyMessage={emptyMessage}
          scrollEnabled={false}
          mobileBreakpoint={360}
        />
      )}

      <EntityFormModal
        visible={mostrarCriar}
        title={createTitle}
        fields={fields}
        initialValues={{ id: "", ...defaultValues } as T}
        onSubmit={handleCreate}
        onCancel={() => setMostrarCriar(false)}
        submitLabel="Cadastrar"
        erro={erroCriacao}
      />

      <EntityFormModal
        visible={itemEmEdicao !== null}
        title={editTitle}
        fields={fields}
        initialValues={itemEmEdicao ?? ({ id: "", ...defaultValues } as T)}
        onSubmit={handleUpdate}
        onCancel={() => setItemEmEdicao(null)}
        onDelete={handleSolicitarExclusao}
        submitLabel="Salvar"
        erro={erroEdicao}
      />

      <ConfirmDialog
        visible={itemParaExcluir !== null}
        title="Excluir registro"
        message={deleteMessage}
        confirmLabel="Excluir"
        onConfirm={handleConfirmarExclusao}
        onCancel={() => setItemParaExcluir(null)}
      />
    </View>
  );
}