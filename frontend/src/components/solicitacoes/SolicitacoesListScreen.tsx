import { useCallback, useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  TextInput,
  View,
} from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { DataTable } from "../usuarios/DataTable";
import type { ColumnDef } from "../usuarios/types";
import { getApiErrorMessage } from "../../lib/api";
import { listarSolicitacoes } from "./solicitacoesApi";
import type { SolicitacaoResponse } from "./types";
import { SolicitacaoStatusBadge } from "./SolicitacaoStatusBadge";
import { SolicitacaoDetalhesModal } from "./SolicitacaoDetalhesModal";

type FiltroStatusSolicitacao =
  | "TODOS"
  | "PENDENTE_ORCAMENTO"
  | "EM_ANDAMENTO"
  | "FINALIZADA"
  | "CANCELADA";

const OPCOES_FILTRO: { valor: FiltroStatusSolicitacao; rotulo: string }[] = [
  { valor: "TODOS", rotulo: "Todas" },
  { valor: "PENDENTE_ORCAMENTO", rotulo: "Pendente Orçamento" },
  { valor: "EM_ANDAMENTO", rotulo: "Em Andamento" },
  { valor: "FINALIZADA", rotulo: "Finalizada" },
  { valor: "CANCELADA", rotulo: "Cancelada" },
];

function formatarData(isoString?: string): string {
  if (!isoString) return "—";
  try {
    const data = new Date(isoString);
    return new Intl.DateTimeFormat("pt-BR", {
      dateStyle: "short",
      timeStyle: "short",
    }).format(data);
  } catch {
    return isoString;
  }
}

export function SolicitacoesListScreen() {
  const router = useRouter();

  const [solicitacoes, setSolicitacoes] = useState<SolicitacaoResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [busca, setBusca] = useState("");
  const [filtroStatus, setFiltroStatus] = useState<FiltroStatusSolicitacao>("TODOS");

  const [solicitacaoSelecionada, setSolicitacaoSelecionada] =
    useState<SolicitacaoResponse | null>(null);

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro("");
    try {
      const dados = await listarSolicitacoes();
      setSolicitacoes(dados);
    } catch (err) {
      setErro(getApiErrorMessage(err));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    carregar();
  }, [carregar]);

  // Filtragem no cliente por termo e status
  const solicitacoesFiltradas = useMemo(() => {
    const termo = busca.trim().toLowerCase();

    return solicitacoes.filter((item) => {
      // Filtro de status
      if (filtroStatus !== "TODOS" && item.status !== filtroStatus) {
        return false;
      }

      // Filtro de texto
      if (!termo) return true;

      const empresa = item.cliente?.nomeEmpresa?.toLowerCase() ?? "";
      const categoria = item.categoriaProjeto?.nome?.toLowerCase() ?? "";
      const idiomaOrigem = item.idiomaOrigem?.nome?.toLowerCase() ?? "";
      const idiomaDestino = item.idiomaDestino?.nome?.toLowerCase() ?? "";
      const descricao = item.descricao?.toLowerCase() ?? "";

      return (
        empresa.includes(termo) ||
        categoria.includes(termo) ||
        idiomaOrigem.includes(termo) ||
        idiomaDestino.includes(termo) ||
        descricao.includes(termo)
      );
    });
  }, [solicitacoes, busca, filtroStatus]);

  const columns: ColumnDef<SolicitacaoResponse>[] = [
    {
      key: "cliente",
      header: "Cliente",
      render: (item) => (
        <View className="py-1">
          <Text className="text-sm font-semibold text-neutral-900" numberOfLines={1}>
            {item.cliente.nomeEmpresa}
          </Text>
        </View>
      ),
    },
    {
      key: "categoriaProjeto",
      header: "Categoria",
      render: (item) => (
        <Text className="text-sm text-neutral-700" numberOfLines={1}>
          {item.categoriaProjeto.nome}
        </Text>
      ),
    },
    {
      key: "idiomaOrigem",
      header: "Idiomas",
      render: (item) => (
        <View className="flex-row items-center gap-1.5">
          <Text className="text-sm font-medium text-neutral-800">
            {item.idiomaOrigem.codigoIso.toUpperCase()}
          </Text>
          <Ionicons name="arrow-forward" size={14} color="#737373" />
          <Text className="text-sm font-medium text-neutral-800">
            {item.idiomaDestino.codigoIso.toUpperCase()}
          </Text>
        </View>
      ),
    },
    {
      key: "dataRecebimento",
      header: "Data",
      hideOnMobile: true,
      render: (item) => (
        <Text className="text-sm text-neutral-600">
          {formatarData(item.dataRecebimento)}
        </Text>
      ),
    },
    {
      key: "arquivos",
      header: "Arquivos",
      width: 110,
      render: (item) => {
        const total = item.arquivos?.length ?? 0;
        return (
          <View className="flex-row items-center gap-1">
            <Ionicons name="attach-outline" size={16} color="#737373" />
            <Text className="text-sm text-neutral-600">
              {total} {total === 1 ? "anexo" : "anexos"}
            </Text>
          </View>
        );
      },
    },
    {
      key: "status",
      header: "Status",
      render: (item) => <SolicitacaoStatusBadge status={item.status} />,
    },
    {
      key: "id",
      header: "",
      width: 60,
      align: "right",
      render: (item) => (
        <Pressable
          onPress={() => setSolicitacaoSelecionada(item)}
          className="rounded-lg p-2 active:bg-neutral-200"
          accessibilityRole="button"
          accessibilityLabel="Ver detalhes da solicitação"
        >
          <Ionicons name="eye-outline" size={20} color="#6f4f28" />
        </Pressable>
      ),
    },
  ];

  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full max-w-5xl mx-auto px-4 py-8 md:px-8 gap-6">
        <CabecalhoTela
          contexto="Operacional"
          secao="Solicitações"
          titulo="Solicitações"
          descricao="Gerencie as solicitações recebidas dos clientes e acompanhe seus orçamentos."
          rotaFallback="/admin"
          acoes={
            <Pressable
              onPress={() => router.push("/solicitacoes/nova")}
              className="flex-row items-center gap-2 rounded-lg bg-[#6f4f28] px-5 py-3 active:bg-[#5b4020]"
              accessibilityRole="button"
            >
              <Ionicons name="add" size={18} color="#ffffff" />
              <Text className="font-medium text-white">Nova solicitação</Text>
            </Pressable>
          }
        />

        {/* Barra de busca e filtros de status */}
        <View className="gap-3">
          <View className="flex-row items-center gap-2 rounded-lg border border-neutral-300 bg-white px-3 py-2">
            <Ionicons name="search" size={16} color="#a3a3a3" />
            <TextInput
              value={busca}
              onChangeText={setBusca}
              placeholder="Buscar por cliente, categoria, idioma ou descrição..."
              placeholderTextColor="#a3a3a3"
              className="flex-1 py-0 text-sm text-neutral-800"
              clearButtonMode="while-editing"
            />
          </View>

          <View className="flex-row flex-wrap gap-1.5 self-start rounded-lg bg-neutral-100 p-1">
            {OPCOES_FILTRO.map((opcao) => {
              const selecionado = filtroStatus === opcao.valor;
              return (
                <Pressable
                  key={opcao.valor}
                  onPress={() => setFiltroStatus(opcao.valor)}
                  className={`items-center rounded-md px-3 py-1.5 ${
                    selecionado ? "bg-white shadow-sm" : ""
                  }`}
                >
                  <Text
                    className={`text-xs font-medium ${
                      selecionado ? "text-neutral-900" : "text-neutral-500"
                    }`}
                  >
                    {opcao.rotulo}
                  </Text>
                </Pressable>
              );
            })}
          </View>
        </View>

        {/* Tabela de solicitações */}
        <View className="shadow-lg">
          {carregando ? (
            <View className="items-center justify-center gap-3 rounded-2xl bg-white p-10 border border-neutral-200">
              <ActivityIndicator color="#6f4f28" />
              <Text className="text-sm text-neutral-500">Carregando solicitações...</Text>
            </View>
          ) : erro ? (
            <View className="items-center justify-center gap-3 rounded-2xl bg-white p-10 border border-neutral-200">
              <Text className="text-center text-sm text-red-600">{erro}</Text>
              <Pressable
                onPress={carregar}
                className="rounded-lg border border-neutral-300 px-5 py-2.5 active:bg-neutral-50"
              >
                <Text className="font-medium text-neutral-700">Tentar de novo</Text>
              </Pressable>
            </View>
          ) : (
            <DataTable
              data={solicitacoesFiltradas}
              columns={columns}
              keyExtractor={(item) => item.id}
              onRowPress={(item) => setSolicitacaoSelecionada(item)}
              emptyMessage={
                busca.trim() || filtroStatus !== "TODOS"
                  ? "Nenhuma solicitação encontrada para o filtro atual"
                  : "Nenhuma solicitação cadastrada"
              }
              scrollEnabled={false}
            />
          )}
        </View>
      </View>

      {/* Modal de detalhes */}
      <SolicitacaoDetalhesModal
        solicitacao={solicitacaoSelecionada}
        onClose={() => setSolicitacaoSelecionada(null)}
        onCriarOrcamento={(solicitacao) => { setSolicitacaoSelecionada(null); router.push({ pathname: "/orcamentos/novo", params: { solicitacaoId: solicitacao.id } }); }}
      />
    </ScrollView>
  );
}
