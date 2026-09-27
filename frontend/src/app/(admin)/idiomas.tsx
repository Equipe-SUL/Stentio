import { useCallback, useState } from "react";
import { ScrollView, View, Text } from "react-native";
import { CrudSection } from "../../components/usuarios/CrudSection";
import { useBuscaDebounce } from "../../components/usuarios/useBuscaDebounce";
import { coreApi } from "../../lib/api";
import type { FieldDef, FiltroLista, FiltroStatus } from "../../components/usuarios/types";

function ativoDeFiltro(status: FiltroStatus): boolean | undefined {
  if (status === "ativos") return true;
  if (status === "inativos") return false;
  return undefined;
}

function montarQuery(filtro: FiltroLista): string {
  const params = new URLSearchParams();
  if (filtro.nome?.trim()) params.set("nome", filtro.nome.trim());
  if (filtro.ativo !== undefined) params.set("ativo", String(filtro.ativo));
  params.set("size", "100");
  return params.toString();
}

interface Idioma {
  id: string;
  nome: string;
  codigoIso: string;
  ativo: boolean;
}

const camposIdioma: FieldDef<Idioma>[] = [
  { key: "nome", label: "Nome", type: "text" },
  { key: "codigoIso", label: "Código (ISO)", type: "iso", placeholder: "ex: pt, pt-BR" },
  { key: "ativo", label: "Status", type: "boolean" },
];

async function listarIdiomas(filtro: FiltroLista = {}): Promise<Idioma[]> {
  const { data } = await coreApi.get<{ content: Idioma[] }>(`/api/v1/idiomas?${montarQuery(filtro)}`);
  return data.content ?? data;
}

async function criarIdioma(valores: Omit<Idioma, "id">): Promise<Idioma> {
  const { data } = await coreApi.post<Idioma>("/api/v1/idiomas", { nome: valores.nome, codigoIso: valores.codigoIso });
  return data;
}

async function atualizarIdioma(id: string, valores: Omit<Idioma, "id">) {
  await coreApi.put(`/api/v1/idiomas/${id}`, { nome: valores.nome, codigoIso: valores.codigoIso });
}

async function atualizarStatusIdioma(id: string, ativo: boolean) {
  await coreApi.patch(`/api/v1/idiomas/${id}/status`, { ativo });
}

async function excluirIdioma(id: string) {
  await atualizarStatusIdioma(id, false);
}

export default function IdiomasScreen() {
  const buscaIdiomas = useBuscaDebounce();
  const [statusIdiomas, setStatusIdiomas] = useState<FiltroStatus>("todos");

  const fetchIdiomas = useCallback(
    () => listarIdiomas({ nome: buscaIdiomas.debounced, ativo: ativoDeFiltro(statusIdiomas) }),
    [buscaIdiomas.debounced, statusIdiomas],
  );

  return (
    <ScrollView className="flex-1 bg-[#f4f4f5]" contentContainerClassName="p-8" showsVerticalScrollIndicator={false}>
      <View className="mb-8">
        <Text className="text-3xl font-bold text-zinc-900 mb-1">Idiomas</Text>
        <Text className="text-zinc-500 text-sm">Cadastro de idiomas com abreviações ISO</Text>
      </View>

      <View className="bg-white rounded-2xl shadow-sm border border-zinc-100 p-6">
        <CrudSection
          title=""
          createTitle="Novo idioma"
          editTitle="Editar idioma"
          fields={camposIdioma}
          defaultValues={{ nome: "", codigoIso: "", ativo: true }}
          fetchList={fetchIdiomas}
          createItem={criarIdioma}
          updateItem={atualizarIdioma}
          deleteItem={excluirIdioma}
          updateStatus={atualizarStatusIdioma}
          deleteMessage="Este registro será desativado e ficará oculto da listagem. Você pode reativá-lo depois."
          emptyMessage="Nenhum idioma cadastrado"
          filtros={{
            busca: buscaIdiomas.valor,
            onBusca: buscaIdiomas.setValor,
            comStatus: true,
            status: statusIdiomas,
            onStatus: setStatusIdiomas,
          }}
        />
      </View>
    </ScrollView>
  );
}