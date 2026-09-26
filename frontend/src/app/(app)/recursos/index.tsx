import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, ScrollView, View, Text, Pressable, TextInput } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { DataTable } from "../../../components/usuarios/DataTable";
import { RowActions } from "../../../components/usuarios/RowActions";
import { StatusBadge } from "../../../components/usuarios/Badge";
import { RecursoFormModal, RecursoFormValues } from "../../../components/usuarios/RecursoFormModal";
import { useBuscaDebounce } from "../../../components/usuarios/useBuscaDebounce";
import { coreApi, getApiErrorMessage } from "../../../lib/api";
import type { ColumnDef, FiltroStatus } from "../../../components/usuarios/types";

type UnidadeCobranca = "POR_PALAVRA" | "POR_HORA" | "POR_PAGINA" | "POR_PROJETO";

interface TipoServicoResumo {
  id: string;
  nome: string;
}

interface IdiomaResumo {
  id: string;
  nome: string;
  codigoIso: string;
}

interface PrecoRecurso {
  id?: string;
  idiomaOrigem: IdiomaResumo;
  idiomaDestino: IdiomaResumo;
  unidade: UnidadeCobranca;
  valor: number;
}

interface Recurso {
  id: string;
  nome: string;
  email: string;
  telefone: string;
  ativo: boolean;
  tiposServico: TipoServicoResumo[];
  precos: PrecoRecurso[];
}

interface PrecoPayload {
  idiomaOrigemId: string;
  idiomaDestinoId: string;
  unidade: UnidadeCobranca;
  valor: number;
}

interface RecursoPayload {
  nome: string;
  email: string;
  telefone: string;
  tiposServicoIds: string[];
  precos: PrecoPayload[];
}

function ativoDeFiltro(status: FiltroStatus): boolean | undefined {
  if (status === "ativos") return true;
  if (status === "inativos") return false;
  return undefined;
}

// Ajuste os nomes de query param se RecursoFiltro.java tiver campos diferentes.
function montarQueryRecurso(nome: string, ativo?: boolean): string {
  const params = new URLSearchParams();
  if (nome.trim()) params.set("nome", nome.trim());
  if (ativo !== undefined) params.set("ativo", String(ativo));
  params.set("size", "100");
  return params.toString();
}

async function listarRecursos(nome: string, ativo?: boolean): Promise<Recurso[]> {
  const { data } = await coreApi.get<{ content: Recurso[] }>(
    `/api/v1/recursos?${montarQueryRecurso(nome, ativo)}`
  );
  return data.content ?? [];
}

// Endpoints reaproveitados da tela de Catálogo, só para popular os seletores do formulário
async function listarTiposServicoAtivos(): Promise<TipoServicoResumo[]> {
  const { data } = await coreApi.get<{ content: TipoServicoResumo[] }>(
    "/api/v1/tipos-servico?ativo=true&size=100"
  );
  return data.content ?? [];
}

async function listarIdiomasAtivos(): Promise<IdiomaResumo[]> {
  const { data } = await coreApi.get<{ content: IdiomaResumo[] }>("/api/v1/idiomas?ativo=true&size=100");
  return data.content ?? [];
}

function paraPayload(values: RecursoFormValues): RecursoPayload {
  return {
    nome: values.nome,
    email: values.email,
    telefone: values.telefone,
    tiposServicoIds: values.tiposServicoIds,
    precos: values.precos.map((preco) => ({
      idiomaOrigemId: preco.idiomaOrigemId,
      idiomaDestinoId: preco.idiomaDestinoId,
      unidade: preco.unidade,
      valor: Number(preco.valor.replace(",", ".")),
    })),
  };
}

function paraFormValues(recurso: Recurso): RecursoFormValues {
  return {
    nome: recurso.nome,
    email: recurso.email,
    telefone: recurso.telefone,
    tiposServicoIds: recurso.tiposServico.map((t) => t.id),
    precos: recurso.precos.map((preco) => ({
      idiomaOrigemId: preco.idiomaOrigem.id,
      idiomaDestinoId: preco.idiomaDestino.id,
      unidade: preco.unidade,
      valor: String(preco.valor),
    })),
  };
}

const VALORES_INICIAIS_VAZIOS: RecursoFormValues = {
  nome: "",
  email: "",
  telefone: "",
  tiposServicoIds: [],
  precos: [],
};

export default function RecursosPage() {
  const busca = useBuscaDebounce();
  const [status, setStatus] = useState<FiltroStatus>("todos");

  const [recursos, setRecursos] = useState<Recurso[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erroLista, setErroLista] = useState("");

  const [tiposServico, setTiposServico] = useState<TipoServicoResumo[]>([]);
  const [idiomas, setIdiomas] = useState<IdiomaResumo[]>([]);

  const [mostrarCriar, setMostrarCriar] = useState(false);
  const [recursoEmEdicao, setRecursoEmEdicao] = useState<Recurso | null>(null);
  const [erroCriacao, setErroCriacao] = useState("");
  const [erroEdicao, setErroEdicao] = useState("");
  const [erroAcao, setErroAcao] = useState("");

  const recarregar = useCallback(() => {
    setCarregando(true);
    setErroLista("");
    return listarRecursos(busca.debounced, ativoDeFiltro(status))
      .then(setRecursos)
      .catch((error) => setErroLista(getApiErrorMessage(error)))
      .finally(() => setCarregando(false));
  }, [busca.debounced, status]);

  useEffect(() => {
    recarregar();
  }, [recarregar]);

  useEffect(() => {
    Promise.all([listarTiposServicoAtivos(), listarIdiomasAtivos()])
      .then(([tipos, idiomasCarregados]) => {
        setTiposServico(tipos);
        setIdiomas(idiomasCarregados);
      })
      .catch((error) => setErroAcao(getApiErrorMessage(error)));
  }, []);

  async function handleCreate(values: RecursoFormValues): Promise<boolean> {
    setErroCriacao("");
    try {
      // criar() no backend devolve { mensagem, recurso } — desembrulha aqui.
      await coreApi.post<{ mensagem: string; recurso: Recurso }>(
        "/api/v1/recursos",
        paraPayload(values)
      );
      setMostrarCriar(false);
      await recarregar();
      return true;
    } catch (error) {
      setErroCriacao(getApiErrorMessage(error));
      return false;
    }
  }

  async function handleUpdate(values: RecursoFormValues): Promise<boolean> {
    if (!recursoEmEdicao) return false;

    setErroEdicao("");
    try {
      await coreApi.put(`/api/v1/recursos/${recursoEmEdicao.id}`, paraPayload(values));
      setRecursoEmEdicao(null);
      await recarregar();
      return true;
    } catch (error) {
      setErroEdicao(getApiErrorMessage(error));
      return false;
    }
  }

  // PATCH de status aqui é em /{id}, sem sufixo /status — diferente de TipoServico/Idioma.
  async function alterarStatus(id: string, ativo: boolean) {
    await coreApi.patch(`/api/v1/recursos/${id}`, { ativo });
  }

  function handleToggleStatus(recurso: Recurso) {
    setErroAcao("");
    alterarStatus(recurso.id, !recurso.ativo)
      .then(recarregar)
      .catch((error) => setErroAcao(getApiErrorMessage(error)));
  }

  function handleToggleStatusNoForm() {
    if (!recursoEmEdicao) return;
    setErroEdicao("");
    alterarStatus(recursoEmEdicao.id, !recursoEmEdicao.ativo)
      .then(() => {
        setRecursoEmEdicao(null);
        return recarregar();
      })
      .catch((error) => setErroEdicao(getApiErrorMessage(error)));
  }

  const columns: ColumnDef<Recurso>[] = [
    { key: "nome", header: "Nome" },
    { key: "email", header: "Email", hideOnMobile: true },
    { key: "telefone", header: "Telefone", hideOnMobile: true },
    {
      key: "ativo",
      header: "Status",
      render: (recurso) => <StatusBadge status={recurso.ativo ? "Ativo" : "Inativo"} />,
    },
    {
      key: "tiposServico",
      header: "Tipos de serviço",
      render: (recurso) => (
        <Text className="text-sm text-neutral-700" numberOfLines={1}>
          {recurso.tiposServico.map((t) => t.nome).join(", ") || "—"}
        </Text>
      ),
    },
    {
      key: "precos",
      header: "Preços",
      width: 110,
      render: (recurso) => (
        <Text className="text-sm text-neutral-700">
          {recurso.precos.length} {recurso.precos.length === 1 ? "preço" : "preços"}
        </Text>
      ),
    },
    {
      key: "id",
      header: "",
      width: 92,
      align: "right",
      render: (recurso) => (
        <RowActions
          onEdit={() => {
            setErroEdicao("");
            setRecursoEmEdicao(recurso);
          }}
          onToggle={() => handleToggleStatus(recurso)}
          toggleLabel={recurso.ativo ? "Desativar" : "Ativar"}
        />
      ),
    },
  ];

  return (
    <ScrollView
      className="flex-1 bg-neutral-50"
      contentContainerClassName="gap-6 p-6 web:px-24 web:py-12 web:items-center"
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full web:max-w-6xl gap-6">
        <View className="flex-row flex-wrap items-center justify-between gap-3">
          <View className="shrink">
            <Text className="text-2xl font-bold text-neutral-900">Recursos</Text>
            <Text className="text-sm text-neutral-500">
              Profissionais freelancers cadastrados, seus serviços e preços
            </Text>
          </View>

          <Pressable
            onPress={() => {
              setErroCriacao("");
              setMostrarCriar(true);
            }}
            className="flex-row items-center gap-2 rounded-lg bg-[#6f4f28] px-5 py-3"
          >
            <Ionicons name="add" size={18} color="#ffffff" />
            <Text className="font-medium text-white">Novo recurso</Text>
          </Pressable>
        </View>

        <View className="gap-2">
          <View className="flex-row items-center gap-2 rounded-lg border border-neutral-300 bg-white px-3 py-2">
            <Ionicons name="search" size={16} color="#a3a3a3" />
            <TextInput
              value={busca.valor}
              onChangeText={busca.setValor}
              placeholder="Buscar por nome..."
              placeholderTextColor="#a3a3a3"
              className="flex-1 py-0 text-sm text-neutral-800"
              clearButtonMode="while-editing"
            />
          </View>

          <View className="flex-row gap-1 self-start rounded-lg bg-neutral-100 p-1">
            {(["todos", "ativos", "inativos"] as FiltroStatus[]).map((opcao) => {
              const selecionado = status === opcao;
              return (
                <Pressable
                  key={opcao}
                  onPress={() => setStatus(opcao)}
                  className={`items-center rounded-md px-3 py-1.5 ${selecionado ? "bg-white shadow-sm" : ""}`}
                >
                  <Text className={`text-xs font-medium ${selecionado ? "text-neutral-900" : "text-neutral-500"}`}>
                    {opcao === "todos" ? "Todos" : opcao === "ativos" ? "Ativos" : "Inativos"}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {erroAcao ? (
          <View className="rounded-lg border border-red-200 bg-red-50 p-3">
            <Text className="text-sm font-medium text-red-600">{erroAcao}</Text>
          </View>
        ) : null}

        <View className="shadow-lg">
          {carregando ? (
            <View className="items-center justify-center gap-3 rounded-2xl bg-white p-10">
              <ActivityIndicator color="#6f4f28" />
              <Text className="text-sm text-neutral-500">Carregando recursos...</Text>
            </View>
          ) : erroLista ? (
            <View className="items-center justify-center gap-3 rounded-2xl bg-white p-10">
              <Text className="text-center text-sm text-red-600">{erroLista}</Text>
              <Pressable onPress={recarregar} className="rounded-lg border border-neutral-300 px-5 py-3">
                <Text className="font-medium text-neutral-700">Tentar de novo</Text>
              </Pressable>
            </View>
          ) : (
            <DataTable
              data={recursos}
              columns={columns}
              keyExtractor={(recurso) => recurso.id}
              emptyMessage="Nenhum recurso cadastrado"
              scrollEnabled={false}
              mobileBreakpoint={480}
            />
          )}
        </View>
      </View>

      <RecursoFormModal
        visible={mostrarCriar}
        title="Novo recurso"
        initialValues={VALORES_INICIAIS_VAZIOS}
        tiposServicoDisponiveis={tiposServico}
        idiomasDisponiveis={idiomas}
        onSubmit={handleCreate}
        onCancel={() => setMostrarCriar(false)}
        submitLabel="Cadastrar"
        erro={erroCriacao}
      />

      <RecursoFormModal
        visible={recursoEmEdicao !== null}
        title="Editar recurso"
        initialValues={recursoEmEdicao ? paraFormValues(recursoEmEdicao) : VALORES_INICIAIS_VAZIOS}
        tiposServicoDisponiveis={tiposServico}
        idiomasDisponiveis={idiomas}
        onSubmit={handleUpdate}
        onCancel={() => setRecursoEmEdicao(null)}
        onToggleStatus={handleToggleStatusNoForm}
        toggleStatusLabel={recursoEmEdicao?.ativo ? "Desativar" : "Ativar"}
        submitLabel="Salvar"
        erro={erroEdicao}
      />
    </ScrollView>
  );
}