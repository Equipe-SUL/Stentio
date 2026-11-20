import React, { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, Pressable, ScrollView, Text, View } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { getApiErrorMessage } from "../../../lib/api";
import { buscarMeuPerfilRecurso, type PerfilRecurso } from "../../../components/recursos/portalRecursoApi";

const UNIDADE_LABEL: Record<string, string> = {
  PALAVRA: "Por palavra",
  HORA: "Por hora",
  PAGINA: "Por página",
};

export default function MeuPerfilRecursoScreen() {
  const [perfil, setPerfil] = useState<PerfilRecurso | null>(null);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  const carregar = useCallback(async () => {
    setCarregando(true);
    setErro("");
    try {
      setPerfil(await buscarMeuPerfilRecurso());
    } catch (error) {
      setErro(getApiErrorMessage(error));
    } finally {
      setCarregando(false);
    }
  }, []);

  useEffect(() => {
    let ativo = true;
    buscarMeuPerfilRecurso()
      .then((dados) => { if (ativo) setPerfil(dados); })
      .catch((error) => { if (ativo) setErro(getApiErrorMessage(error)); })
      .finally(() => { if (ativo) setCarregando(false); });
    return () => { ativo = false; };
  }, []);

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
            <Pressable onPress={() => void carregar()} className="rounded-lg border border-neutral-300 px-4 py-2"><Text className="font-medium text-neutral-700">Tentar novamente</Text></Pressable>
          </View>
        ) : perfil ? (
          <>
            <View className="gap-5 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm md:p-7">
              <View className="flex-row flex-wrap items-start justify-between gap-3">
                <View className="flex-row items-center gap-4">
                  <View className="h-14 w-14 items-center justify-center rounded-full bg-[#f5ede7]"><Ionicons name="person-outline" size={27} color="#8c5230" /></View>
                  <View className="gap-1">
                    <Text className="text-lg font-bold text-neutral-900">{perfil.nome}</Text>
                    <Text className="text-sm text-neutral-600">{perfil.email}</Text>
                  </View>
                </View>
                <View className={`flex-row items-center gap-1.5 rounded-full px-3 py-1.5 ${perfil.verificado ? "bg-emerald-50" : "bg-amber-50"}`}>
                  <Ionicons name={perfil.verificado ? "checkmark-circle" : "time-outline"} size={15} color={perfil.verificado ? "#047857" : "#b45309"} />
                  <Text className={`text-xs font-semibold ${perfil.verificado ? "text-emerald-800" : "text-amber-800"}`}>{perfil.verificado ? "Verificado" : "Verificação pendente"}</Text>
                </View>
              </View>
              <View className="flex-row flex-wrap gap-x-10 gap-y-4 border-t border-neutral-100 pt-4">
                <View className="min-w-[180px] flex-1 gap-1"><Text className="text-xs text-neutral-500">Telefone</Text><Text className="text-sm font-medium text-neutral-800">{perfil.telefone || "Não informado"}</Text></View>
                <View className="min-w-[140px] flex-1 gap-1"><Text className="text-xs text-neutral-500">Situação da conta</Text><Text className="text-sm font-medium text-neutral-800">{perfil.ativo ? "Ativa" : "Inativa"}</Text></View>
              </View>
            </View>

            <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm md:p-7">
              <View className="gap-1"><Text className="text-base font-bold text-neutral-900">Tipos de serviço</Text><Text className="text-sm text-neutral-500">Serviços que você oferece.</Text></View>
              {perfil.tiposServico.length ? (
                <View className="flex-row flex-wrap gap-2">{perfil.tiposServico.map((tipo) => <View key={tipo.id} className="rounded-full bg-neutral-100 px-3 py-2"><Text className="text-sm font-medium text-neutral-700">{tipo.nome}</Text></View>)}</View>
              ) : <Text className="text-sm text-neutral-500">Nenhum serviço cadastrado.</Text>}
            </View>

            <View className="gap-4 rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm md:p-7">
              <View className="gap-1"><Text className="text-base font-bold text-neutral-900">Pares e valores</Text><Text className="text-sm text-neutral-500">Idiomas e valores informados no cadastro.</Text></View>
              {perfil.precos.length ? (
                <View className="gap-3">{perfil.precos.map((preco, index) => (
                  <View key={preco.id ?? `${preco.idiomaOrigem.id}-${preco.idiomaDestino.id}-${index}`} className="flex-row flex-wrap items-center justify-between gap-3 rounded-xl border border-neutral-200 p-4">
                    <View className="flex-1 gap-1"><Text className="text-sm font-semibold text-neutral-800">{preco.idiomaOrigem.nome} → {preco.idiomaDestino.nome}</Text><Text className="text-xs text-neutral-500">{UNIDADE_LABEL[preco.unidade] ?? preco.unidade}</Text></View>
                    <Text className="text-base font-bold text-neutral-900">R$ {Number(preco.valor).toLocaleString("pt-BR", { minimumFractionDigits: 2, maximumFractionDigits: 4 })}</Text>
                  </View>
                ))}</View>
              ) : <Text className="text-sm text-neutral-500">Nenhum par com valor cadastrado.</Text>}
            </View>
          </>
        ) : null}
      </View>
    </ScrollView>
  );
}
