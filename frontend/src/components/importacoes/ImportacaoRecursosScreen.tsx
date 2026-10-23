import { useState } from "react";
import {
  ActivityIndicator,
  Modal,
  Pressable,
  ScrollView,
  Text,
  View,
} from "react-native";
import { useRouter } from "expo-router";
import { Ionicons } from "@expo/vector-icons";
import { CabecalhoTela } from "../navegacao/CabecalhoTela";
import { ArquivoCsvField } from "./ArquivoCsvField";
import { OrientacoesCsv } from "./OrientacoesCsv";
import { PreviaRecursos } from "./PreviaRecursos";
import { RelatorioRecursos } from "./RelatorioRecursos";
import { useImportacaoRecursos } from "./useImportacaoRecursos";
import { formatarBytes } from "../../lib/importacaoService";

export function ImportacaoRecursosScreen() {
  const fluxo = useImportacaoRecursos();
  const router = useRouter();
  const [confirmar, setConfirmar] = useState(false);
  const [largura, setLargura] = useState(0);
  const amplo = largura >= 850;
  const passo =
    fluxo.etapa === "arquivo" || fluxo.etapa === "validando"
      ? 0
      : fluxo.etapa === "revisao"
        ? 1
        : 2;
  const processando =
    fluxo.etapa === "validando" || fluxo.etapa === "importando";
  return (
    <ScrollView
      className="flex-1 bg-[#fbfaf8]"
      contentContainerStyle={{ flexGrow: 1, paddingBottom: 40 }}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full max-w-6xl mx-auto px-4 py-8 md:px-8">
        <CabecalhoTela
          contexto="Sistema"
          secao="Importações"
          titulo="Importar recursos"
          descricao="Traga os profissionais do seu sistema anterior. Valide o arquivo, revise os dados e acompanhe o resultado da importação."
          rotaFallback="/admin"
        />
        <View className="flex-row gap-2 mb-6">
          {["Arquivo", "Revisão", "Resultado"].map((label, index) => (
            <View
              key={label}
              className={`flex-1 flex-row flex-wrap items-center gap-2 border-b-2 pb-3 ${index <= passo ? "border-[#8c5230]" : "border-zinc-200"}`}
            >
              <View
                className={`h-6 w-6 rounded-full items-center justify-center ${index <= passo ? "bg-[#8c5230]" : "bg-zinc-200"}`}
              >
                <Text
                  className={`text-xs font-bold ${index <= passo ? "text-white" : "text-zinc-500"}`}
                >
                  {index + 1}
                </Text>
              </View>
              <Text
                className={`text-xs font-semibold ${index <= passo ? "text-[#8c5230]" : "text-zinc-500"}`}
              >
                {label}
              </Text>
            </View>
          ))}
        </View>
        <View
          onLayout={(event) => setLargura(event.nativeEvent.layout.width)}
          style={{
            flexDirection: amplo ? "row" : "column",
            gap: 24,
            alignItems: "flex-start",
          }}
        >
          <View
            style={{
              flex: amplo ? 1 : undefined,
              width: amplo ? undefined : "100%",
              minWidth: 0,
            }}
            className="gap-5"
          >
            <View className="rounded-2xl border border-zinc-200 bg-white p-5 md:p-6 gap-5">
              <View className="flex-row gap-3 items-center">
                <View className="h-10 w-10 rounded-xl bg-[#8c5230]/10 items-center justify-center">
                  <Ionicons name="people-outline" size={21} color="#8c5230" />
                </View>
                <View className="flex-1">
                  <Text className="text-base font-semibold text-zinc-900">
                    Importação de profissionais
                  </Text>
                  <Text className="text-xs text-zinc-500 mt-1">
                    Recursos, serviços e preços
                  </Text>
                </View>
              </View>
              {!fluxo.configuracao ? (
                <View className="gap-3 py-6 items-center">
                  {fluxo.erroConfiguracao ? (
                    <>
                      <Text
                        accessibilityRole="alert"
                        className="text-sm text-center text-red-700"
                      >
                        {fluxo.erroConfiguracao}
                      </Text>
                      <Pressable
                        onPress={fluxo.recarregarConfiguracao}
                        accessibilityRole="button"
                        className="rounded-lg border border-zinc-200 px-4 py-3"
                      >
                        <Text className="text-sm text-[#8c5230]">
                          Tentar novamente
                        </Text>
                      </Pressable>
                    </>
                  ) : (
                    <>
                      <ActivityIndicator color="#8c5230" />
                      <Text className="text-sm text-zinc-500">
                        Carregando limites do serviço...
                      </Text>
                    </>
                  )}
                </View>
              ) : fluxo.etapa === "importando" ||
                fluxo.etapa === "resultado" ? (
                <View className="flex-row items-center gap-3 rounded-xl border border-zinc-200 bg-zinc-50 p-4">
                  <Ionicons
                    name="document-text-outline"
                    size={24}
                    color="#8c5230"
                  />
                  <View className="flex-1 min-w-0 gap-1">
                    <Text
                      className="text-sm font-semibold text-zinc-900"
                      selectable
                    >
                      {fluxo.arquivo?.name}
                    </Text>
                    <Text className="text-xs text-zinc-500">
                      {formatarBytes(
                        fluxo.arquivo?.file?.size ?? fluxo.arquivo?.size,
                      )}
                    </Text>
                  </View>
                </View>
              ) : (
                <ArquivoCsvField
                  arquivo={fluxo.arquivo}
                  configuracao={fluxo.configuracao}
                  separador={fluxo.separador}
                  disabled={fluxo.bloqueado}
                  selecionando={fluxo.selecionando}
                  onSelecionar={fluxo.selecionarArquivo}
                  onRemover={fluxo.removerArquivo}
                  onSeparador={fluxo.alterarSeparador}
                />
              )}
              {fluxo.erro ? (
                <View
                  accessibilityRole="alert"
                  accessibilityLiveRegion="assertive"
                  className="rounded-xl border border-red-200 bg-red-50 p-4 gap-1"
                >
                  <Text className="text-sm font-semibold leading-5 text-red-700">
                    {fluxo.erro.mensagem}
                  </Text>
                  {fluxo.erro.linha !== undefined ? (
                    <Text className="text-xs text-red-700">
                      Linha do CSV: {fluxo.erro.linha}
                    </Text>
                  ) : null}
                  {fluxo.erro.campo ? (
                    <Text className="text-xs text-red-700">
                      Campo: {fluxo.erro.campo}
                    </Text>
                  ) : null}
                </View>
              ) : null}
              {processando ? (
                <View
                  accessibilityLiveRegion="polite"
                  className="flex-row items-center gap-3 rounded-xl bg-zinc-50 p-4"
                >
                  <ActivityIndicator color="#8c5230" />
                  <View className="flex-1 gap-1">
                    <Text className="text-sm font-semibold text-zinc-800">
                      {fluxo.etapa === "importando"
                        ? "Importando recursos..."
                        : fluxo.progresso < 100
                          ? `Enviando arquivo · ${fluxo.progresso}%`
                          : "Validando o arquivo..."}
                    </Text>
                    <Text className="text-xs leading-5 text-zinc-500">
                      {fluxo.etapa === "importando"
                        ? "Aguarde o relatório sem sair desta tela. Cada profissional é salvo separadamente."
                        : "Todas as linhas serão verificadas. Nenhum cadastro será gravado nesta etapa."}
                    </Text>
                  </View>
                </View>
              ) : null}
              {fluxo.etapa === "arquivo" || fluxo.etapa === "validando" ? (
                <Pressable
                  onPress={fluxo.validar}
                  accessibilityLabel="Validar arquivo"
                  disabled={
                    !fluxo.arquivo || fluxo.bloqueado || !fluxo.configuracao
                  }
                  accessibilityRole="button"
                  accessibilityState={{
                    disabled:
                      !fluxo.arquivo || fluxo.bloqueado || !fluxo.configuracao,
                  }}
                  className={`flex-row items-center justify-center gap-2 rounded-lg bg-[#8c5230] px-5 py-3 ${!fluxo.arquivo || fluxo.bloqueado ? "opacity-50" : "active:bg-[#6f4f28]"}`}
                >
                  <Ionicons
                    name="checkmark-circle-outline"
                    size={18}
                    color="#fff"
                  />
                  <Text className="text-sm font-semibold text-white">
                    Validar arquivo
                  </Text>
                </Pressable>
              ) : null}
            </View>

            {fluxo.previa && fluxo.etapa === "revisao" ? (
              <View className="gap-5">
                <PreviaRecursos previa={fluxo.previa} />
                <View className="rounded-xl bg-amber-50 border border-amber-200 p-4">
                  <Text className="text-sm leading-5 text-amber-900">
                    A validação ainda não gravou cadastros. Ao confirmar, o
                    sistema poderá importar parte do arquivo e recusar outros
                    recursos. Confira o relatório ao final.
                  </Text>
                </View>
                <Pressable
                  onPress={() => setConfirmar(true)}
                  accessibilityLabel="Continuar para importação"
                  accessibilityRole="button"
                  className="flex-row items-center justify-center gap-2 rounded-lg bg-[#8c5230] px-5 py-3 active:bg-[#6f4f28]"
                >
                  <Ionicons
                    name="cloud-upload-outline"
                    size={19}
                    color="#fff"
                  />
                  <Text className="text-sm font-semibold text-white">
                    Continuar para importação
                  </Text>
                </Pressable>
              </View>
            ) : null}

            {fluxo.etapa === "resultado" ? (
              <View className="gap-5">
                {fluxo.relatorio ? (
                  <RelatorioRecursos relatorio={fluxo.relatorio} />
                ) : (
                  <View className="rounded-xl border border-amber-200 bg-amber-50 p-4">
                    <Text className="text-sm leading-5 text-amber-900">
                      O serviço não retornou um relatório. Confira a mensagem
                      acima antes de iniciar outra importação.
                    </Text>
                  </View>
                )}
                <View className="flex-row flex-wrap gap-3">
                  <Pressable
                    onPress={fluxo.novaImportacao}
                    accessibilityRole="button"
                    className="rounded-lg bg-[#8c5230] px-5 py-3"
                  >
                    <Text className="text-sm font-semibold text-white">
                      Nova importação
                    </Text>
                  </Pressable>
                  <Pressable
                    onPress={() => router.push("/recursos")}
                    accessibilityRole="button"
                    className="rounded-lg border border-zinc-200 bg-white px-5 py-3"
                  >
                    <Text className="text-sm font-semibold text-zinc-700">
                      Ver recursos
                    </Text>
                  </Pressable>
                </View>
              </View>
            ) : null}
          </View>
          <View style={{ width: amplo ? 290 : "100%", flexShrink: 0 }}>
            <OrientacoesCsv configuracao={fluxo.configuracao} />
          </View>
        </View>
      </View>
      <Modal
        visible={confirmar}
        transparent
        animationType="fade"
        onRequestClose={() => setConfirmar(false)}
      >
        <View className="flex-1 items-center justify-center bg-black/40 px-6">
          <View
            className="w-full max-w-md rounded-2xl bg-white p-6 gap-4"
            accessibilityViewIsModal
          >
            <Text
              accessibilityRole="header"
              className="text-lg font-bold text-zinc-900"
            >
              Confirmar importação
            </Text>
            <Text className="text-sm leading-6 text-zinc-600">
              Você está prestes a enviar {fluxo.previa?.total_recursos} recursos
              para cadastro. Recursos em conflito não serão alterados. Uma falha
              poderá ocorrer após outros cadastros já terem sido salvos.
            </Text>
            <View className="flex-row flex-wrap justify-end gap-3 pt-2">
              <Pressable
                onPress={() => setConfirmar(false)}
                accessibilityRole="button"
                className="rounded-lg px-4 py-3"
              >
                <Text className="text-sm font-semibold text-zinc-600">
                  Voltar
                </Text>
              </Pressable>
              <Pressable
                onPress={() => {
                  setConfirmar(false);
                  void fluxo.importar();
                }}
                accessibilityRole="button"
                className="rounded-lg bg-[#8c5230] px-4 py-3"
              >
                <Text className="text-sm font-semibold text-white">
                  Confirmar importação
                </Text>
              </Pressable>
            </View>
          </View>
        </View>
      </Modal>
    </ScrollView>
  );
}
