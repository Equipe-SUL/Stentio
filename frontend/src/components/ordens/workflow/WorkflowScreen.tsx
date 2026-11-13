import React, { useEffect, useMemo, useState } from "react";
import {
  ActivityIndicator,
  Pressable,
  ScrollView,
  Text,
  View,
} from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { CabecalhoTela } from "../../navegacao/CabecalhoTela";
import { OrdemServicoStatusBadge } from "../OrdemServicoStatusBadge";
import { buscarOrdemServicoPorId } from "../ordensApi";
import type { OrdemServicoDetalhe } from "../types";
import {
  aplicarModeloWorkflow,
  buscarWorkflowOrdem,
  listarModelosWorkflow,
  listarRecursosWorkflow,
  listarTiposServicoWorkflow,
  salvarNovoModeloWorkflow,
  salvarWorkflowManual,
  validarSequenciaEtapas,
} from "./workflowApi";
import { TimelineEtapas } from "./TimelineEtapas";
import { ModalAlocarRecurso } from "./ModalAlocarRecurso";
import { ModalCarregarModelo } from "./ModalCarregarModelo";
import { ModalSalvarModelo } from "./ModalSalvarModelo";
import type {
  EtapaWorkflow,
  ModeloWorkflow,
  RecursoOption,
  TipoServicoOption,
} from "./types";
import { getApiErrorMessage } from "../../../lib/api";

interface WorkflowScreenProps {
  ordemId: string;
}

export function WorkflowScreen({ ordemId }: WorkflowScreenProps) {
  const router = useRouter();

  // Estados principais
  const [ordem, setOrdem] = useState<OrdemServicoDetalhe | null>(null);
  const [etapas, setEtapas] = useState<EtapaWorkflow[]>([]);
  const [tiposServico, setTiposServico] = useState<TipoServicoOption[]>([]);
  const [recursos, setRecursos] = useState<RecursoOption[]>([]);
  const [modelos, setModelos] = useState<ModeloWorkflow[]>([]);

  // Estados de carregamento e feedback
  const [carregandoGeral, setCarregandoGeral] = useState(true);
  const [carregandoModelos, setCarregandoModelos] = useState(false);
  const [salvandoWorkflow, setSalvandoWorkflow] = useState(false);
  const [erro, setErro] = useState<string | null>(null);
  const [mensagemSucesso, setMensagemSucesso] = useState<string | null>(null);
  const [temAlteracoesPendentes, setTemAlteracoesPendentes] = useState(false);

  // Modais
  const [modalAlocacaoEtapa, setModalAlocacaoEtapa] = useState<EtapaWorkflow | null>(null);
  const [modalCarregarModeloAberto, setModalCarregarModeloAberto] = useState(false);
  const [modalSalvarModeloAberto, setModalSalvarModeloAberto] = useState(false);

  // Carregamento inicial de todos os dados necessários
  useEffect(() => {
    let ativo = true;

    async function carregar() {
      try {
        setCarregandoGeral(true);
        setErro(null);

        const [dadosOrdem, dadosWorkflow, listaTipos, listaRecursos] = await Promise.all([
          buscarOrdemServicoPorId(ordemId),
          buscarWorkflowOrdem(ordemId),
          listarTiposServicoWorkflow(),
          listarRecursosWorkflow(),
        ]);

        if (!ativo) return;
        setOrdem(dadosOrdem);
        setEtapas(dadosWorkflow.etapas);
        setTiposServico(listaTipos);
        setRecursos(listaRecursos);
        setTemAlteracoesPendentes(false);
      } catch (err: unknown) {
        if (!ativo) return;
        setErro(getApiErrorMessage(err));
      } finally {
        if (ativo) setCarregandoGeral(false);
      }
    }

    carregar();

    return () => {
      ativo = false;
    };
  }, [ordemId]);

  // Carrega modelos quando o modal correspondente for aberto
  const abrirModalCarregarModelo = async () => {
    setModalCarregarModeloAberto(true);
    try {
      setCarregandoModelos(true);
      const lista = await listarModelosWorkflow();
      setModelos(lista);
    } catch (err) {
      console.error("Falha ao listar modelos:", err);
    } finally {
      setCarregandoModelos(false);
    }
  };

  // RN.3: Se alguma etapa já possui recurso alocado, a sequência não pode mais ser alterada estruturalmente
  const bloqueadoPorRN3 = useMemo(() => {
    return etapas.some((e) => Boolean(e.recurso_id));
  }, [etapas]);

  // Limpa mensagens de sucesso automaticamente após alguns segundos
  useEffect(() => {
    if (mensagemSucesso) {
      const timer = setTimeout(() => setMensagemSucesso(null), 5000);
      return () => clearTimeout(timer);
    }
  }, [mensagemSucesso]);

  // Adicionar nova etapa (CA.3)
  const handleAdicionarEtapa = () => {
    if (bloqueadoPorRN3) return;

    const proximaOrdem = etapas.length + 1;
    const tipoPadrao = tiposServico[0]?.id || "";
    const tipoPadraoNome = tiposServico[0]?.nome || "";

    const novaEtapa: EtapaWorkflow = {
      ordem: proximaOrdem,
      nome: `Nova Etapa ${proximaOrdem}`,
      tipo_servico_id: tipoPadrao,
      tipo_servico_nome: tipoPadraoNome,
      prazo_estimado_horas: 12,
      // CA.4: Primeira é PENDENTE, seguintes AGUARDANDO_ETAPA_ANTERIOR
      status: proximaOrdem === 1 ? "PENDENTE" : "AGUARDANDO_ETAPA_ANTERIOR",
    };

    setEtapas((prev) => [...prev, novaEtapa]);
    setTemAlteracoesPendentes(true);
  };

  // Reordenar: subir etapa (CA.3 & RN.1)
  const handleSubirEtapa = (index: number) => {
    if (index <= 0 || bloqueadoPorRN3) return;
    setEtapas((prev) => {
      const copia = [...prev];
      const item = copia[index];
      copia[index] = copia[index - 1];
      copia[index - 1] = item;
      // Recalcula ordens estritamente sequenciais (RN.1)
      return copia.map((e, idx) => ({ ...e, ordem: idx + 1 }));
    });
    setTemAlteracoesPendentes(true);
  };

  // Reordenar: descer etapa (CA.3 & RN.1)
  const handleDescerEtapa = (index: number) => {
    if (index >= etapas.length - 1 || bloqueadoPorRN3) return;
    setEtapas((prev) => {
      const copia = [...prev];
      const item = copia[index];
      copia[index] = copia[index + 1];
      copia[index + 1] = item;
      // Recalcula ordens estritamente sequenciais (RN.1)
      return copia.map((e, idx) => ({ ...e, ordem: idx + 1 }));
    });
    setTemAlteracoesPendentes(true);
  };

  // Remover etapa (CA.3 & RN.1)
  const handleRemoverEtapa = (index: number) => {
    if (bloqueadoPorRN3) return;
    setEtapas((prev) => {
      const copia = prev.filter((_, idx) => idx !== index);
      // Recalcula ordens contínuas (RN.1)
      return copia.map((e, idx) => ({
        ...e,
        ordem: idx + 1,
        status: idx === 0 && e.status === "AGUARDANDO_ETAPA_ANTERIOR" ? "PENDENTE" : e.status,
      }));
    });
    setTemAlteracoesPendentes(true);
  };

  // Atualizar campo da etapa em edição inline
  const handleAtualizarCampoEtapa = (
    index: number,
    campo: keyof EtapaWorkflow,
    valor: any,
  ) => {
    setEtapas((prev) => {
      const copia = [...prev];
      const alvo = { ...copia[index], [campo]: valor };
      if (campo === "tipo_servico_id") {
        const achado = tiposServico.find((t) => t.id === valor);
        if (achado) alvo.tipo_servico_nome = achado.nome;
      }
      copia[index] = alvo;
      return copia;
    });
    setTemAlteracoesPendentes(true);
  };

  // Alternar conclusão da etapa
  const handleAlternarConclusao = (index: number) => {
    setEtapas((prev) => {
      const copia = [...prev];
      const atual = copia[index];
      const novoStatus = atual.status === "CONCLUIDA" ? "PENDENTE" : "CONCLUIDA";
      copia[index] = { ...atual, status: novoStatus };

      // Se concluiu a etapa atual, destrava a próxima que estava aguardando
      if (novoStatus === "CONCLUIDA" && index + 1 < copia.length) {
        if (copia[index + 1].status === "AGUARDANDO_ETAPA_ANTERIOR") {
          copia[index + 1] = { ...copia[index + 1], status: "PENDENTE" };
        }
      }
      return copia;
    });
    setTemAlteracoesPendentes(true);
  };

  // Confirmar alocação de recurso na etapa
  const handleConfirmarAlocacao = (recurso: RecursoOption) => {
    if (!modalAlocacaoEtapa) return;
    setEtapas((prev) =>
      prev.map((e) =>
        e.ordem === modalAlocacaoEtapa.ordem
          ? { ...e, recurso_id: recurso.id, recurso_nome: recurso.nome }
          : e,
      ),
    );
    setModalAlocacaoEtapa(null);
    setTemAlteracoesPendentes(true);
    setMensagemSucesso(`Recurso "${recurso.nome}" alocado na Etapa ${modalAlocacaoEtapa.ordem}.`);
  };

  // Remover alocação de recurso
  const handleRemoverAlocacao = (etapa: EtapaWorkflow) => {
    setEtapas((prev) =>
      prev.map((e) =>
        e.ordem === etapa.ordem ? { ...e, recurso_id: null, recurso_nome: null } : e,
      ),
    );
    setTemAlteracoesPendentes(true);
    setMensagemSucesso(`Alocação de recurso removida da Etapa ${etapa.ordem}.`);
  };

  // Salvar workflow na API (POST /api/v1/ordens-servico/{os_id}/etapas)
  const handleSalvarWorkflow = async () => {
    try {
      setSalvandoWorkflow(true);
      setErro(null);

      // Validação frontend prévia
      const validacao = validarSequenciaEtapas(etapas);
      if (!validacao.valido) {
        setErro(validacao.mensagem || "Erro na validação das etapas.");
        setSalvandoWorkflow(false);
        return;
      }

      await salvarWorkflowManual(ordemId, etapas);
      setTemAlteracoesPendentes(false);
      setMensagemSucesso("Workflow da Ordem de Serviço salvo com sucesso!");
      // Recarrega árvore para garantir integridade com o backend
      const dadosAtualizados = await buscarWorkflowOrdem(ordemId);
      setEtapas(dadosAtualizados.etapas);
    } catch (err: any) {
      setErro(getApiErrorMessage(err));
    } finally {
      setSalvandoWorkflow(false);
    }
  };

  // Aplicar modelo pré-definido (CA.2)
  const handleAplicarModelo = async (modeloId: string) => {
    try {
      setErro(null);
      await aplicarModeloWorkflow(ordemId, modeloId);
      setTemAlteracoesPendentes(false);
      setMensagemSucesso("Modelo de workflow aplicado com sucesso na OS!");
      // Recarrega etapas clonadas do modelo
      const dadosAtualizados = await buscarWorkflowOrdem(ordemId);
      setEtapas(dadosAtualizados.etapas);
    } catch (err: any) {
      setErro(getApiErrorMessage(err));
      throw err;
    }
  };

  // Salvar como novo modelo reutilizável
  const handleSalvarComoModelo = async (dados: { nome: string; descricao?: string }) => {
    try {
      setErro(null);
      await salvarNovoModeloWorkflow({
        nome: dados.nome,
        descricao: dados.descricao,
        etapas: etapas.map((e) => ({
          ordem: e.ordem,
          nome: e.nome,
          tipo_servico_id: e.tipo_servico_id,
          prazo_estimado_horas: e.prazo_estimado_horas,
        })),
      });
      setMensagemSucesso(`Modelo "${dados.nome}" salvo com sucesso para reuso!`);
    } catch (err: any) {
      setErro(getApiErrorMessage(err));
      throw err;
    }
  };

  if (carregandoGeral) {
    return (
      <View className="flex-1 items-center justify-center p-12 bg-[#fbfaf8]">
        <ActivityIndicator size="large" color="#6f4f28" />
        <Text className="mt-4 text-sm font-medium text-neutral-600">
          Carregando workflow da Ordem de Serviço...
        </Text>
      </View>
    );
  }

  const solicitacao = ordem?.solicitacao;
  const cliente = ordem?.cliente;

  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 60 }}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full max-w-5xl mx-auto px-4 py-8 md:px-8 gap-6">
        {/* Cabeçalho da Tela com Ações de Topo */}
        <CabecalhoTela
          contexto="Operacional"
          secao="Ordens de Serviço"
          titulo={`Workflow • ${ordem?.codigo || "OS"}`}
          descricao="Estruture e acompanhe as etapas para a conclusão do projeto em linha do tempo, defina prazos, aloque recursos e utilize modelos pré-definidos."
          rotaFallback={`/ordens/${ordemId}`}
          acoes={
            <View className="flex-row items-center gap-2 flex-wrap">
              {/* Botão Carregar Modelo (CA.1) */}
              <Pressable
                onPress={abrirModalCarregarModelo}
                disabled={bloqueadoPorRN3}
                className={`flex-row items-center gap-1.5 rounded-xl border border-neutral-300 bg-white px-3.5 py-2 active:bg-neutral-100 shadow-xs ${
                  bloqueadoPorRN3 ? "opacity-50" : ""
                }`}
                accessibilityRole="button"
                accessibilityLabel="Carregar modelo de workflow pré-definido"
              >
                <Ionicons name="copy-outline" size={16} color="#525252" />
                <Text className="text-xs font-semibold text-neutral-800">
                  Carregar Modelo
                </Text>
              </Pressable>

              {/* Botão Salvar como Modelo */}
              {etapas.length > 0 ? (
                <Pressable
                  onPress={() => setModalSalvarModeloAberto(true)}
                  className="flex-row items-center gap-1.5 rounded-xl border border-neutral-300 bg-white px-3.5 py-2 active:bg-neutral-100 shadow-xs"
                  accessibilityRole="button"
                  accessibilityLabel="Salvar sequência atual como modelo"
                >
                  <Ionicons name="bookmark-outline" size={16} color="#525252" />
                  <Text className="text-xs font-semibold text-neutral-800">
                    Salvar Modelo
                  </Text>
                </Pressable>
              ) : null}

              {/* Botão Salvar Workflow (CA.3) */}
              <Pressable
                onPress={handleSalvarWorkflow}
                disabled={salvandoWorkflow || etapas.length === 0}
                className={`flex-row items-center gap-2 rounded-xl px-4 py-2 shadow-xs transition-colors ${
                  salvandoWorkflow || etapas.length === 0
                    ? "bg-neutral-300 opacity-60"
                    : temAlteracoesPendentes
                    ? "bg-[#6f4f28] active:bg-[#5b4020]"
                    : "bg-[#8c5230]/80 active:bg-[#6f4f28]"
                }`}
                accessibilityRole="button"
                accessibilityLabel="Salvar alterações do workflow na OS"
              >
                {salvandoWorkflow ? (
                  <ActivityIndicator size="small" color="#ffffff" />
                ) : (
                  <Ionicons name="save" size={16} color="#ffffff" />
                )}
                <Text className="text-xs font-bold text-white">
                  {salvandoWorkflow ? "Gravando..." : "Salvar Workflow"}
                </Text>
              </Pressable>
            </View>
          }
        />

        {/* Notificação de Sucesso */}
        {mensagemSucesso ? (
          <View className="flex-row items-center gap-3 rounded-2xl bg-emerald-50 border border-emerald-200 p-4 shadow-xs">
            <Ionicons name="checkmark-circle" size={20} color="#059669" />
            <Text className="text-sm font-semibold text-emerald-900 flex-1">
              {mensagemSucesso}
            </Text>
            <Pressable onPress={() => setMensagemSucesso(null)}>
              <Ionicons name="close" size={18} color="#059669" />
            </Pressable>
          </View>
        ) : null}

        {/* Notificação de Erro */}
        {erro ? (
          <View className="flex-row items-center gap-3 rounded-2xl bg-red-50 border border-red-200 p-4 shadow-xs">
            <Ionicons name="alert-circle" size={20} color="#dc2626" />
            <Text className="text-sm font-semibold text-red-900 flex-1">{erro}</Text>
            <Pressable onPress={() => setErro(null)}>
              <Ionicons name="close" size={18} color="#dc2626" />
            </Pressable>
          </View>
        ) : null}

        {/* Card Resumo do Projeto da Ordem de Serviço */}
        {ordem ? (
          <View className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm gap-3">
            <View className="flex-col md:flex-row md:items-center justify-between gap-3 border-b border-neutral-100 pb-3">
              <View className="flex-row items-center gap-3 flex-wrap">
                <Text className="text-xl font-bold font-serif text-neutral-900">
                  {ordem.codigo}
                </Text>
                <OrdemServicoStatusBadge status={ordem.status} tamanho="pequeno" />
                {temAlteracoesPendentes ? (
                  <View className="rounded-full bg-amber-100 border border-amber-300 px-2.5 py-0.5">
                    <Text className="text-[11px] font-bold text-amber-800">
                      Alterações não salvas
                    </Text>
                  </View>
                ) : null}
              </View>

              <Pressable
                onPress={() => router.push(`/ordens/${ordemId}`)}
                className="flex-row items-center gap-1 self-start md:self-auto text-xs text-[#8c5230] hover:underline"
              >
                <Ionicons name="eye-outline" size={15} color="#8c5230" />
                <Text className="text-xs font-semibold text-[#8c5230]">
                  Ver detalhes completos da OS
                </Text>
              </Pressable>
            </View>

            <View className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-1">
              <View className="rounded-xl bg-neutral-50 p-3 border border-neutral-100">
                <Text className="text-[11px] font-medium text-neutral-500 uppercase">Cliente</Text>
                <Text className="text-sm font-bold text-neutral-800 truncate mt-0.5">
                  {cliente?.nome || "Cliente não informado"}
                </Text>
              </View>

              <View className="rounded-xl bg-neutral-50 p-3 border border-neutral-100">
                <Text className="text-[11px] font-medium text-neutral-500 uppercase">
                  Par de Idiomas
                </Text>
                <Text className="text-sm font-bold text-neutral-800 mt-0.5">
                  {solicitacao?.idioma_origem || "Origem"} → {solicitacao?.idioma_destino || "Destino"}
                </Text>
              </View>

              <View className="rounded-xl bg-neutral-50 p-3 border border-neutral-100">
                <Text className="text-[11px] font-medium text-neutral-500 uppercase">
                  Categoria
                </Text>
                <Text className="text-sm font-bold text-neutral-800 truncate mt-0.5">
                  {solicitacao?.categoria || "Geral"}
                </Text>
              </View>
            </View>
          </View>
        ) : null}

        {/* Timeline das Etapas em Containers */}
        <TimelineEtapas
          etapas={etapas}
          tiposServico={tiposServico}
          bloqueadoPorRN3={bloqueadoPorRN3}
          onAdicionarEtapa={handleAdicionarEtapa}
          onSubirEtapa={handleSubirEtapa}
          onDescerEtapa={handleDescerEtapa}
          onRemoverEtapa={handleRemoverEtapa}
          onAbrirAlocacao={(etapa) => setModalAlocacaoEtapa(etapa)}
          onRemoverAlocacao={handleRemoverAlocacao}
          onAlternarConclusao={handleAlternarConclusao}
          onAtualizarCampoEtapa={handleAtualizarCampoEtapa}
          onAbrirCarregarModelo={abrirModalCarregarModelo}
        />
      </View>

      {/* Modal para Alocar Recurso */}
      <ModalAlocarRecurso
        visible={Boolean(modalAlocacaoEtapa)}
        etapa={modalAlocacaoEtapa}
        recursos={recursos}
        tiposServico={tiposServico}
        onFechar={() => setModalAlocacaoEtapa(null)}
        onConfirmarAlocacao={handleConfirmarAlocacao}
      />

      {/* Modal para Carregar Modelo Pré-definido */}
      <ModalCarregarModelo
        visible={modalCarregarModeloAberto}
        modelos={modelos}
        carregando={carregandoModelos}
        onFechar={() => setModalCarregarModeloAberto(false)}
        onAplicarModelo={handleAplicarModelo}
      />

      {/* Modal para Salvar Sequência Atual como Modelo */}
      <ModalSalvarModelo
        visible={modalSalvarModeloAberto}
        etapas={etapas}
        tiposServico={tiposServico}
        onFechar={() => setModalSalvarModeloAberto(false)}
        onConfirmarSalvar={handleSalvarComoModelo}
      />
    </ScrollView>
  );
}
