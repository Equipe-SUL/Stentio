import { useCallback } from "react";
import { ScrollView, View, Text } from "react-native";
import { CrudSection } from "../../components/usuarios/CrudSection";
import { useBuscaDebounce } from "../../components/usuarios/useBuscaDebounce";
import { coreApi } from "../../lib/api";
import type { FieldDef, FiltroLista } from "../../components/usuarios/types";

function montarQuery(filtro: FiltroLista): string {
  const params = new URLSearchParams();
  if (filtro.nome?.trim()) params.set("nome", filtro.nome.trim());
  params.set("size", "100");
  return params.toString();
}

interface Categoria {
  id: string;
  nome: string;
}

const camposCategoria: FieldDef<Categoria>[] = [{ key: "nome", label: "Nome", type: "text" }];

async function listarCategorias(filtro: FiltroLista = {}): Promise<Categoria[]> {
  const { data } = await coreApi.get<{ content: Categoria[] }>(`/api/v1/categorias-projeto?${montarQuery(filtro)}`);
  return data.content ?? data;
}

async function criarCategoria(valores: Omit<Categoria, "id">): Promise<Categoria> {
  const { data } = await coreApi.post<Categoria>("/api/v1/categorias-projeto", { nome: valores.nome });
  return data;
}

async function atualizarCategoria(id: string, valores: Omit<Categoria, "id">) {
  await coreApi.put(`/api/v1/categorias-projeto/${id}`, { nome: valores.nome });
}

async function excluirCategoria(id: string) {
  await coreApi.delete(`/api/v1/categorias-projeto/${id}`);
}

export default function CategoriasScreen() {
  const buscaCategorias = useBuscaDebounce();

  const fetchCategorias = useCallback(
    () => listarCategorias({ nome: buscaCategorias.debounced }),
    [buscaCategorias.debounced],
  );

  return (
    <ScrollView className="flex-1 bg-[#f4f4f5]" contentContainerClassName="p-8" showsVerticalScrollIndicator={false}>
      <View className="mb-8">
        <Text className="text-3xl font-bold text-zinc-900 mb-1">Categorias de Projeto</Text>
        <Text className="text-zinc-500 text-sm">Organize os projetos por tipo de conteúdo</Text>
      </View>

      <View className="bg-white rounded-2xl shadow-sm border border-zinc-100 p-6">
        <CrudSection
          title=""
          createTitle="Nova categoria"
          editTitle="Editar categoria"
          fields={camposCategoria}
          defaultValues={{ nome: "" }}
          fetchList={fetchCategorias}
          createItem={criarCategoria}
          updateItem={atualizarCategoria}
          deleteItem={excluirCategoria}
          emptyMessage="Nenhuma categoria cadastrada"
          filtros={{
            busca: buscaCategorias.valor,
            onBusca: buscaCategorias.setValor,
          }}
        />
      </View>
    </ScrollView>
  );
}