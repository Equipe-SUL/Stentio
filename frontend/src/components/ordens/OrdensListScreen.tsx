import React, { useEffect, useMemo, useState } from "react";
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
import { Picker } from "@react-native-picker/picker";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { DataTable } from "../usuarios/DataTable";
import type { ColumnDef } from "../usuarios/types";
import { getApiErrorMessage } from "../../lib/api";
import { listarClientesOpcoes, listarOrdensServico } from "./ordensApi";
import { OrdemServicoStatusBadge } from "./OrdemServicoStatusBadge";
import { OrdemServicoDetalhesModal } from "./OrdemServicoDetalhesModal";
import type {
  ClienteOpcao,
  OrdemServicoPagina,
  OrdemServicoResumo,
} from "./types";

type FiltroStatus =
  | "TODOS"
  | "PENDENTE_ALOCACAO"
  | "EM_PLANEJAMENTO"
  | "EM_EXECUCAO"
  | "AGUARDANDO_VERIFICACAO";

const STATUS_OPCOES: { valor: FiltroStatus; rotulo: string }[] = [
  { valor: "TODOS", rotulo: "Todas" },
  { valor: "PENDENTE_ALOCACAO", rotulo: "Pendente de Alocação" },
  { valor: "EM_PLANEJAMENTO", rotulo: "Em Planejamento" },
  { valor: "EM_EXECUCAO", rotulo: "Em Execução" },
  { valor: "AGUARDANDO_VERIFICACAO", rotulo: "Aguardando Verificação" },
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

export function OrdensListScreen() {
  const router = useRouter();
  const [paginaDados, setPaginaDados] = useState<OrdemServicoPagina>({
    pagina: 1,
    tamanho_pagina: 10,
    total_elementos: 0,
    conteudo: [],
  });

  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [chaveAtualizacao, setChaveAtualizacao] = useState(0);
  const [busca, setBusca] = useState("");
  const [filtroStatus, setFiltroStatus] = useState<FiltroStatus>("TODOS");
  const [clienteSelecionadoId, setClienteSelecionadoId] = useState("");
  const [clientes, setClientes] = useState<ClienteOpcao[]>([]);

  const [paginaAtual, setPaginaAtual] = useState(1);
  const [tamanhoPagina] = useState(10);

  const [ordemSelecionadaId, setOrdemSelecionadaId] = useState<string | null>(null);

  useEffect(() => {
    listarClientesOpcoes().then(setClientes);
  }, []);

  useEffect(() => {
    let ativo = true;
    const statusParam = filtroStatus === "TODOS" ? undefined : filtroStatus;

    listarOrdensServico({
      status: statusParam,
      clienteId: clienteSelecionadoId || undefined,
      pagina: paginaAtual,
      tamanhoPagina,
    })
      .then((dados) => {
        if (ativo) {
          setPaginaDados(dados);
          setErro("");
        }
      })
      .catch((err: unknown) => {
        if (ativo) setErro(getApiErrorMessage(err));
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });

    return () => {
      ativo = false;
    };
  }, [filtroStatus, clienteSelecionadoId, paginaAtual, tamanhoPagina, chaveAtualizacao]);

  const ordensFiltradas = useMemo(() => {
    const termo = busca.trim().toLowerCase();
    if (!termo) return paginaDados.conteudo;

    return paginaDados.conteudo.filter((item) => {
      const codigo = item.codigo_visual?.toLowerCase() ?? "";
      const clienteNome = item.cliente_nome?.toLowerCase() ?? "";
      return codigo.includes(termo) || clienteNome.includes(termo);
    });
  }, [paginaDados.conteudo, busca]);

  const totalPaginas = Math.max(
    1,
    Math.ceil(paginaDados.total_elementos / tamanhoPagina),
  );

  const handleMudarStatus = (novoStatus: FiltroStatus) => {
    if (novoStatus === filtroStatus) return;
    setCarregando(true);
    setFiltroStatus(novoStatus);
    setPaginaAtual(1);
  };

  const handleMudarCliente = (novoClienteId: string) => {
    if (novoClienteId === clienteSelecionadoId) return;
    setCarregando(true);
    setClienteSelecionadoId(novoClienteId);
    setPaginaAtual(1);
  };

  const handleMudarPagina = (novaPagina: number) => {
    setCarregando(true);
    setPaginaAtual(novaPagina);
  };

  const handleTentarNovamente = () => {
    setCarregando(true);
    setChaveAtualizacao((valor) => valor + 1);
  };

  const columns: ColumnDef<OrdemServicoResumo>[] = [
    {
      key: "codigo_visual",
      header: "Código da OS",
      width: 150,
      render: (item) => (
        <View className="py-1">
          <Text className="font-mono text-sm font-bold text-neutral-900">
            {item.codigo_visual}
          </Text>
        </View>
      ),
    },
    {
      key: "cliente_nome",
      header: "Cliente",
      render: (item) => (
        <View className="py-1">
          <Text className="text-sm font-semibold text-neutral-900" numberOfLines={1}>
            {item.cliente_nome}
          </Text>
        </View>
      ),
    },
    {
      key: "data_criacao",
      header: "Data de Criação",
      hideOnMobile: true,
      width: 160,
      render: (item) => (
        <Text className="text-sm text-neutral-600">
          {formatarData(item.data_criacao)}
        </Text>
      ),
    },
    {
      key: "status",
      header: "Status",
      width: 210,
      render: (item) => <OrdemServicoStatusBadge status={item.status} />,
    },
    {
      key: "id",
      header: "Ações",
      width: 220,
      align: "right",
      render: (item) => (
        <View className="flex-row items-center justify-end gap-2">
          {item.status === "AGUARDANDO_VERIFICACAO" ? (
            <Pressable
              onPress={() => router.push(`/ordens/${item.id}/verificacao` as never)}
              className="flex-row items-center gap-1.5 rounded-lg border border-purple-300 bg-purple-50 px-2.5 py-1.5 hover:bg-purple-100 active:bg-purple-200"
              accessibilityRole="button"
              accessibilityLabel={`Revisar pacote final da OS ${item.codigo_visual}`}
            >
              <Ionicons name="checkmark-done-outline" size={15} color="#6b21a8" />
              <Text className="text-xs font-bold text-purple-800">Revisar</Text>
            </Pressable>
          ) : null}

          <Pressable
            onPress={() => router.push(`/ordens/${item.id}/workflow`)}
            className="flex-row items-center gap-1.5 rounded-lg border border-[#8c5230]/40 bg-[#fbf8f5] px-2.5 py-1.5 hover:bg-[#f6efe9] active:bg-[#ede2d6]"
            accessibilityRole="button"
            accessibilityLabel={`Abrir workflow da OS ${item.codigo_visual}`}
          >
            <Ionicons name="git-network-outline" size={15} color="#8c5230" />
            <Text className="text-xs font-bold text-[#8c5230]">Workflow</Text>
          </Pressable>

          <Pressable
            onPress={() => setOrdemSelecionadaId(item.id)}
            className="rounded-lg p-1.5 border border-neutral-300 bg-white hover:bg-neutral-50 active:bg-neutral-100"
            accessibilityRole="button"
            accessibilityLabel={`Ver detalhes da OS ${item.codigo_visual}`}
          >
            <Ionicons name="eye-outline" size={17} color="#525252" />
          </Pressable>
        </View>
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
          secao="Ordens de Serviço"
          titulo="Ordens de Serviço"
          descricao="Consulte as ordens de serviço ativas no sistema, acompanhe seu status e visualize os detalhes completos do projeto."
          rotaFallback="/admin"
        />

        {/* Barra de Filtros e Busca (CA1) */}
        <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm">
          <View className="flex-row items-center gap-2 rounded-lg border border-neutral-300 bg-neutral-50 px-3 py-2.5">
            <Ionicons name="search" size={18} color="#a3a3a3" />
            <TextInput
              value={busca}
              onChangeText={setBusca}
              placeholder="Buscar por código visual (ex: OS-2026-...) ou nome do cliente..."
              placeholderTextColor="#a3a3a3"
              className="flex-1 py-0 text-sm text-neutral-800"
              clearButtonMode="while-editing"
            />
            {busca ? (
              <Pressable onPress={() => setBusca("")} hitSlop={8}>
                <Ionicons name="close-circle" size={16} color="#a3a3a3" />
              </Pressable>
            ) : null}
          </View>

          <View className="flex-col md:flex-row md:items-center justify-between gap-3">
            <View className="flex-row flex-wrap gap-1.5 self-start rounded-lg bg-neutral-100 p-1">
              {STATUS_OPCOES.map((opcao) => {
                const selecionado = filtroStatus === opcao.valor;
                return (
                  <Pressable
                    key={opcao.valor}
                    onPress={() => handleMudarStatus(opcao.valor)}
                    className={`items-center rounded-md px-3 py-1.5 transition-colors ${
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

            <View className="w-full md:w-72">
              <View className="overflow-hidden rounded-lg border border-neutral-300 bg-white">
                <Picker
                  selectedValue={clienteSelecionadoId}
                  onValueChange={handleMudarCliente}
                  dropdownIconColor="#737373"
                  style={{ color: "#171717" }}
                  accessibilityLabel="Filtrar por cliente"
                >
                  <Picker.Item label="Todos os clientes" value="" />
                  {clientes.map((c) => (
                    <Picker.Item key={c.id} label={c.nomeEmpresa} value={c.id} />
                  ))}
                </Picker>
              </View>
            </View>
          </View>
        </View>

        {/* Tabela de Ordens de Serviço */}
        <View className="shadow-lg">
          {carregando ? (
            <View className="items-center justify-center gap-3 rounded-2xl bg-white p-12 border border-neutral-200">
              <ActivityIndicator color="#6f4f28" size="large" />
              <Text className="text-sm text-neutral-500 font-medium">
                Carregando ordens de serviço...
              </Text>
            </View>
          ) : erro ? (
            <View className="items-center justify-center gap-3 rounded-2xl bg-white p-10 border border-neutral-200">
              <Ionicons name="alert-circle-outline" size={32} color="#dc2626" />
              <Text className="text-center text-sm text-red-600 font-medium">
                {erro}
              </Text>
              <Pressable
                onPress={handleTentarNovamente}
                className="rounded-lg border border-neutral-300 px-5 py-2.5 active:bg-neutral-50"
              >
                <Text className="font-medium text-neutral-700">Tentar de novo</Text>
              </Pressable>
            </View>
          ) : (
            <View className="gap-4">
              <DataTable
                data={ordensFiltradas}
                columns={columns}
                keyExtractor={(item) => item.id}
                onRowPress={(item) => setOrdemSelecionadaId(item.id)}
                emptyMessage={
                  busca.trim() || filtroStatus !== "TODOS" || clienteSelecionadoId
                    ? "Nenhuma ordem de serviço encontrada para os filtros aplicados."
                    : "Nenhuma ordem de serviço registrada no momento."
                }
                scrollEnabled={false}
              />

              {paginaDados.total_elementos > 0 ? (
                <View className="flex-col sm:flex-row items-center justify-between gap-4 rounded-xl border border-neutral-200 bg-white px-4 py-3 shadow-sm">
                  <View className="flex-row items-center gap-2">
                    <Text className="text-xs text-neutral-500">
                      Total:{" "}
                      <Text className="font-bold text-neutral-800">
                        {paginaDados.total_elementos}
                      </Text>{" "}
                      {paginaDados.total_elementos === 1 ? "ordem" : "ordens"}
                    </Text>
                    <Text className="text-neutral-300">•</Text>
                    <Text className="text-xs text-neutral-500">
                      Página {paginaAtual} de {totalPaginas}
                    </Text>
                  </View>

                  <View className="flex-row items-center gap-2">
                    <Pressable
                      onPress={() => handleMudarPagina(Math.max(1, paginaAtual - 1))}
                      disabled={paginaAtual <= 1}
                      className={`flex-row items-center gap-1 rounded-lg border px-3 py-1.5 ${
                        paginaAtual <= 1
                          ? "border-neutral-200 bg-neutral-100 opacity-50"
                          : "border-neutral-300 bg-white active:bg-neutral-100"
                      }`}
                      accessibilityRole="button"
                      accessibilityLabel="Página anterior"
                    >
                      <Ionicons
                        name="chevron-back"
                        size={16}
                        color={paginaAtual <= 1 ? "#a3a3a3" : "#374151"}
                      />
                      <Text
                        className={`text-xs font-medium ${
                          paginaAtual <= 1 ? "text-neutral-400" : "text-neutral-700"
                        }`}
                      >
                        Anterior
                      </Text>
                    </Pressable>

                    <Pressable
                      onPress={() => handleMudarPagina(Math.min(totalPaginas, paginaAtual + 1))}
                      disabled={paginaAtual >= totalPaginas}
                      className={`flex-row items-center gap-1 rounded-lg border px-3 py-1.5 ${
                        paginaAtual >= totalPaginas
                          ? "border-neutral-200 bg-neutral-100 opacity-50"
                          : "border-neutral-300 bg-white active:bg-neutral-100"
                      }`}
                      accessibilityRole="button"
                      accessibilityLabel="Próxima página"
                    >
                      <Text
                        className={`text-xs font-medium ${
                          paginaAtual >= totalPaginas
                            ? "text-neutral-400"
                            : "text-neutral-700"
                        }`}
                      >
                        Próxima
                      </Text>
                      <Ionicons
                        name="chevron-forward"
                        size={16}
                        color={paginaAtual >= totalPaginas ? "#a3a3a3" : "#374151"}
                      />
                    </Pressable>
                  </View>
                </View>
              ) : null}
            </View>
          )}
        </View>
      </View>

      <OrdemServicoDetalhesModal
        ordemId={ordemSelecionadaId}
        onClose={() => setOrdemSelecionadaId(null)}
      />
    </ScrollView>
  );
}