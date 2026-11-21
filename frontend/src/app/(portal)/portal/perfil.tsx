import React, { useCallback, useEffect, useMemo, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import { getApiErrorMessage } from "../../../lib/api";
import { buscarMeuPerfilRecurso, type PerfilRecurso } from "../../../components/recursos/portalRecursoApi";
import { listarMinhasCertificacoes, type CertificacaoResponse } from "../../../components/recursos/certificacoesApi";

const UNIDADE_LABEL: Record<string, string> = {
  PALAVRA: "Por palavra",
  HORA: "Por hora",
  PAGINA: "Por página",
};

interface PrecoItem {
  id?: string;
  unidade: string;
  valor: number;
}

interface ParComPrecos {
  chave: string;
  idiomaOrigem: { id: string; nome: string };
  idiomaDestino: { id: string; nome: string };
  precos: PrecoItem[];
  certificacao?: CertificacaoResponse;
}

export default function MeuPerfilRecursoScreen() {
  const router = useRouter();
  const [perfil, setPerfil] = useState<PerfilRecurso | null>(null);
  const [certificacoes, setCertificacoes] = useState<CertificacaoResponse[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro("");
    try {
      const [perfilData, certsData] = await Promise.all([
        buscarMeuPerfilRecurso(),
        listarMinhasCertificacoes(),
      ]);
      setPerfil(perfilData);
      setCertificacoes(certsData);
    } catch (error) {
      setErro(getApiErrorMessage(error));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    let ativo = true;
    Promise.all([buscarMeuPerfilRecurso(), listarMinhasCertificacoes()])
      .then(([perfilData, certsData]) => {
        if (ativo) {
          setPerfil(perfilData);
          setCertificacoes(certsData);
        }
      })
      .catch((error) => {
        if (ativo) setErro(getApiErrorMessage(error));
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });
    return () => {
      ativo = false;
    };
  }, []);

  // Agrupa os preços por par de idiomas único, evitando repetição visual do mesmo par
  const paresAgrupados = useMemo<ParComPrecos[]>(() => {
    const mapa = new Map<string, ParComPrecos>();
    for (const preco of perfil?.precos ?? []) {
      const chave = `${preco.idiomaOrigem.id}:::${preco.idiomaDestino.id}`;
      let item = mapa.get(chave);
      if (!item) {
        const cert = certificacoes.find(
          (c) =>
            c.idioma_origem.id === preco.idiomaOrigem.id &&
            c.idioma_destino.id === preco.idiomaDestino.id
        );
        item = {
          chave,
          idiomaOrigem: preco.idiomaOrigem,
          idiomaDestino: preco.idiomaDestino,
          precos: [],
          certificacao: cert,
        };
        mapa.set(chave, item);
      }
      item.precos.push({
        id: preco.id,
        unidade: preco.unidade,
        valor: preco.valor,
      });
    }
    return Array.from(mapa.values());
  }, [perfil?.precos, certificacoes]);

  return (
    <ScrollView className="flex-1" contentContainerClassName="items-center p-5 py-8 md:p-8">
      <View className="w-full max-w-5xl gap-6">
        <View className="gap-1">
          <Text className="text-2xl font-bold text-neutral-900">Meu perfil</Text>
          <Text className="text-sm text-neutral-600">Seus dados profissionais e competências cadastradas.</Text>
        </View>
        {carregando ? (
          <View className="items-center justify-center gap-3 rounded-2xl border border-neutral-200 bg-white p-12">
            <ActivityIndicator color="#8c5230" />
            <Text className="text-sm text-neutral-500">Carregando seu perfil...</Text>
          </View>
        ) : erro ? (
          <View className="items-center gap-3 rounded-2xl border border-red-200 bg-white p-8">
            <Text accessibilityRole="alert" className="text-center text-sm text-red-700">{erro}</Text>
            <Pressable onPress={() => void carregar()} className="rounded-lg border border-neutral-300 px-4 py-2">
              <Text className="font-medium text-neutral-700">Tentar novamente</Text>
            </Pressable>
          </View>
        ) : perfil ? (
          <>
            <View className="gap-5 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm md:p-7">
              <View className="flex-row flex-wrap items-start justify-between gap-3">
                <View className="flex-row items-center gap-4">
                  <View className="h-14 w-14 items-center justify-center rounded-full bg-[#f5ede7]">
                    <Ionicons name="person-outline" size={27} color="#8c5230" />
                  </View>
                  <View className="gap-1">
                    <Text className="text-lg font-bold text-neutral-900">{perfil.nome}</Text>
                    <Text className="text-sm text-neutral-600">{perfil.email}</Text>
                  </View>
                </View>
                <View className={`flex-row items-center gap-1.5 rounded-full px-3 py-1.5 ${perfil.verificado ? "bg-emerald-50" : "bg-amber-50"}`}>
                  <Ionicons name={perfil.verificado ? "checkmark-circle" : "time-outline"} size={15} color={perfil.verificado ? "#047857" : "#b45309"} />
                  <Text className={`text-xs font-semibold ${perfil.verificado ? "text-emerald-800" : "text-amber-800"}`}>
                    {perfil.verificado ? "Cadastro revisado" : "Pendente de revisão"}
                  </Text>
                </View>
              </View>
              <View className="flex-row flex-wrap gap-x-10 gap-y-4 border-t border-neutral-100 pt-4">
                <View className="min-w-[180px] flex-1 gap-1">
                  <Text className="text-xs text-neutral-500">Telefone</Text>
                  <Text className="text-sm font-medium text-neutral-800">{perfil.telefone || "Não informado"}</Text>
                </View>
                <View className="min-w-[140px] flex-1 gap-1">
                  <Text className="text-xs text-neutral-500">Situação da conta</Text>
                  <Text className="text-sm font-medium text-neutral-800">{perfil.ativo ? "Ativa" : "Inativa"}</Text>
                </View>
              </View>
            </View>

            <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm md:p-7">
              <View className="gap-1">
                <Text className="text-base font-bold text-neutral-900">Tipos de serviço</Text>
                <Text className="text-sm text-neutral-500">Serviços que você oferece.</Text>
              </View>
              {perfil.tiposServico.length ? (
                <View className="flex-row flex-wrap gap-2">
                  {perfil.tiposServico.map((tipo) => (
                    <View key={tipo.id} className="rounded-full bg-neutral-100 px-3 py-2">
                      <Text className="text-sm font-medium text-neutral-700">{tipo.nome}</Text>
                    </View>
                  ))}
                </View>
              ) : (
                <Text className="text-sm text-neutral-500">Nenhum serviço cadastrado.</Text>
              )}
            </View>

            <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm md:p-7">
              <View className="flex-row flex-wrap items-center justify-between gap-3">
                <View className="gap-1">
                  <Text className="text-base font-bold text-neutral-900">Pares, valores e certificações</Text>
                  <Text className="text-sm text-neutral-500">Idiomas atendidos e situação da comprovação por par.</Text>
                </View>
                <Pressable
                  accessibilityRole="button"
                  onPress={() => router.replace("/portal/certificacoes" as never)}
                  className="flex-row items-center gap-1.5 rounded-lg border border-neutral-300 bg-white px-3 py-1.5 hover:bg-neutral-50"
                >
                  <Ionicons name="ribbon-outline" size={16} color="#8c5230" />
                  <Text className="text-xs font-semibold text-neutral-700">Gerenciar certificações</Text>
                </Pressable>
              </View>

              {paresAgrupados.length ? (
                <View className="gap-3">
                  {paresAgrupados.map((par) => {
                    const cert = par.certificacao;
                    const isAprovada = cert?.status === "APROVADA";
                    const isPendente = cert?.status === "PENDENTE";
                    const isRejeitada = cert?.status === "REJEITADA";

                    return (
                      <View
                        key={par.chave}
                        className="gap-3 rounded-xl border border-neutral-200 p-4"
                      >
                        <View className="flex-row flex-wrap items-center justify-between gap-2">
                          <Text className="text-sm font-semibold text-neutral-800">
                            {par.idiomaOrigem.nome} → {par.idiomaDestino.nome}
                          </Text>
                          {/* Certification badge */}
                          <View
                            className={`rounded-full px-2.5 py-0.5 border ${
                              isAprovada
                                ? "border-emerald-200 bg-emerald-50 text-emerald-800"
                                : isPendente
                                ? "border-amber-200 bg-amber-50 text-amber-800"
                                : isRejeitada
                                ? "border-red-200 bg-red-50 text-red-800"
                                : "border-neutral-200 bg-neutral-100 text-neutral-600"
                            }`}
                          >
                            <Text
                              className={`text-[11px] font-semibold ${
                                isAprovada
                                  ? "text-emerald-800"
                                  : isPendente
                                  ? "text-amber-800"
                                  : isRejeitada
                                  ? "text-red-800"
                                  : "text-neutral-600"
                              }`}
                            >
                              {isAprovada
                                ? "Certificado"
                                : isPendente
                                ? "Certificação em análise"
                                : isRejeitada
                                ? "Certificação recusada"
                                : "Sem certificação"}
                            </Text>
                          </View>
                        </View>

                        <View className="flex-row flex-wrap gap-4 border-t border-neutral-100 pt-2.5">
                          {par.precos.map((p, pIdx) => (
                            <View key={`${par.chave}-${p.unidade}-${pIdx}`} className="gap-0.5 min-w-[120px]">
                              <Text className="text-xs text-neutral-500">
                                {UNIDADE_LABEL[p.unidade] ?? p.unidade}
                              </Text>
                              <Text className="text-sm font-bold text-neutral-900">
                                R$ {Number(p.valor).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 4 })}
                              </Text>
                            </View>
                          ))}
                        </View>
                      </View>
                    );
                  })}
                </View>
              ) : (
                <Text className="text-sm text-neutral-500">Nenhum par com valor cadastrado.</Text>
              )}
            </View>
          </>
        ) : null}
      </View>
    </ScrollView>
  );
}
