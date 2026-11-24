import { useMemo, useState } from "react";
import {
  ActivityIndicator,
  Modal,
  Pressable,
  ScrollView,
  Text,
  View,
} from "react-native";
import { Picker } from "@react-native-picker/picker";
import * as DocumentPicker from "expo-document-picker";
import { Ionicons } from "@expo/vector-icons";
import { getApiErrorMessage } from "../../lib/api";
import {
  enviarCertificacao,
  type ArquivoCertificacao,
  type CertificacaoResponse,
  type StatusCertificacao,
} from "./certificacoesApi";

export const TAMANHO_MAXIMO_CERTIFICACAO_MB = 10;
const TAMANHO_MAXIMO_BYTES = TAMANHO_MAXIMO_CERTIFICACAO_MB * 1024 * 1024;

function formatarTamanho(bytes?: number): string {
  if (bytes === undefined) return "";
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

export interface ParIdiomaOpcao {
  idiomaOrigem: { id: string; nome: string };
  idiomaDestino: { id: string; nome: string };
  statusCertificacao?: StatusCertificacao;
}

interface ModalEnviarCertificacaoProps {
  visible: boolean;
  onClose: () => void;
  onSuccess: (cert: CertificacaoResponse) => void;
  pares: ParIdiomaOpcao[];
  parInicial?: { idiomaOrigemId: string; idiomaDestinoId: string } | null;
}

export function ModalEnviarCertificacao({
  visible,
  onClose,
  onSuccess,
  pares,
  parInicial,
}: ModalEnviarCertificacaoProps) {
  const [parSelecionadoChave, setParSelecionadoChave] = useState<string | null>(
    null,
  );
  const [arquivo, setArquivo] = useState<ArquivoCertificacao | null>(null);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");
  const [selecionandoArquivo, setSelecionandoArquivo] = useState(false);

  // Garante que cada par de idiomas apareça estritamente uma única vez nas opções
  const paresUnicos = useMemo(() => {
    const mapa = new Map<string, ParIdiomaOpcao>();
    for (const p of pares) {
      const chave = `${p.idiomaOrigem.id}:::${p.idiomaDestino.id}`;
      if (!mapa.has(chave)) {
        mapa.set(chave, p);
      }
    }
    return Array.from(mapa.values());
  }, [pares]);

  const primeiroNaoAprovado = paresUnicos.find(
    (par) => par.statusCertificacao !== "APROVADA",
  );
  const chaveParInicial = parInicial
    ? `${parInicial.idiomaOrigemId}:::${parInicial.idiomaDestinoId}`
    : null;
  const chaveParSelecionado =
    parSelecionadoChave ??
    chaveParInicial ??
    (primeiroNaoAprovado
      ? `${primeiroNaoAprovado.idiomaOrigem.id}:::${primeiroNaoAprovado.idiomaDestino.id}`
      : paresUnicos[0]
        ? `${paresUnicos[0].idiomaOrigem.id}:::${paresUnicos[0].idiomaDestino.id}`
        : "");

  function fechar() {
    setErro("");
    setArquivo(null);
    setParSelecionadoChave(null);
    onClose();
  }

  async function handleSelecionarArquivo() {
    if (selecionandoArquivo || enviando) return;
    setErro("");
    setSelecionandoArquivo(true);
    try {
      const resultado = await DocumentPicker.getDocumentAsync({
        multiple: false,
        copyToCacheDirectory: true,
        type: "*/*",
      });

      if (resultado.canceled || !resultado.assets || resultado.assets.length === 0) return;

      const asset = resultado.assets[0];
      if (asset.size !== undefined && asset.size > TAMANHO_MAXIMO_BYTES) {
        setErro(`O arquivo selecionado excede o limite de ${TAMANHO_MAXIMO_CERTIFICACAO_MB} MB.`);
        return;
      }

      setArquivo({
        uri: asset.uri,
        name: asset.name,
        type: asset.mimeType ?? "application/octet-stream",
        size: asset.size,
        file: asset.file,
      });
    } catch {
      setErro("Falha ao selecionar o arquivo. Tente novamente.");
    } finally {
      setSelecionandoArquivo(false);
    }
  }

  async function handleEnviar() {
    if (!chaveParSelecionado) {
      setErro("Selecione o par de idiomas a certificar.");
      return;
    }
    if (!arquivo) {
      setErro("Selecione o arquivo da certificação para envio.");
      return;
    }

    const [idiomaOrigemId, idiomaDestinoId] = chaveParSelecionado.split(":::");
    if (!idiomaOrigemId || !idiomaDestinoId) {
      setErro("Par de idiomas inválido.");
      return;
    }

    setEnviando(true);
    setErro("");
    try {
      const resultado = await enviarCertificacao(idiomaOrigemId, idiomaDestinoId, arquivo);
      onSuccess(resultado);
      fechar();
    } catch (e) {
      setErro(getApiErrorMessage(e));
    } finally {
      setEnviando(false);
    }
  }

  const parAtual = paresUnicos.find(
    (p) => `${p.idiomaOrigem.id}:::${p.idiomaDestino.id}` === chaveParSelecionado
  );
  const estaAprovado = parAtual?.statusCertificacao === "APROVADA";

  return (
    <Modal visible={visible} transparent animationType="fade" onRequestClose={fechar}>
      <View className="flex-1 items-center justify-center bg-black/40 px-4 py-8">
        <View className="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
          <View className="flex-row items-center justify-between pb-3 border-b border-neutral-100">
            <View className="flex-row items-center gap-2.5">
              <View className="h-9 w-9 items-center justify-center rounded-xl bg-[#f5ede7]">
                <Ionicons name="ribbon-outline" size={20} color="#8c5230" />
              </View>
              <View>
                <Text className="text-lg font-bold text-neutral-900">Enviar certificação</Text>
                <Text className="text-xs text-neutral-500">Documento comprobatório por par de idiomas</Text>
              </View>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Fechar modal"
              onPress={fechar}
              className="h-8 w-8 items-center justify-center rounded-lg border border-neutral-200"
            >
              <Ionicons name="close" size={18} color="#71717a" />
            </Pressable>
          </View>

          {erro ? (
            <View className="mt-4 flex-row gap-2 rounded-xl border border-red-200 bg-red-50 p-3">
              <Ionicons name="alert-circle-outline" size={18} color="#dc2626" />
              <Text className="flex-1 text-xs text-red-700 leading-4">{erro}</Text>
            </View>
          ) : null}

          <ScrollView className="mt-4 max-h-115" showsVerticalScrollIndicator={false}>
            <View className="gap-4">
              <View className="gap-1.5">
                <Text className="text-xs font-semibold text-neutral-700 uppercase tracking-wider">
                  Par de idiomas atendido *
                </Text>
                {paresUnicos.length === 0 ? (
                  <Text className="text-sm text-neutral-500 italic">
                    Nenhum par de idiomas cadastrado no seu perfil.
                  </Text>
                ) : (
                  <View className="overflow-hidden rounded-xl border border-neutral-300 bg-white">
                    <Picker
                      selectedValue={chaveParSelecionado}
                      onValueChange={(val) => setParSelecionadoChave(String(val))}
                    >
                      {paresUnicos.map((p) => {
                        const chave = `${p.idiomaOrigem.id}:::${p.idiomaDestino.id}`;
                        const labelAprovado = p.statusCertificacao === "APROVADA" ? " (Já certificado)" : "";
                        return (
                          <Picker.Item
                            key={chave}
                            label={`${p.idiomaOrigem.nome} → ${p.idiomaDestino.nome}${labelAprovado}`}
                            value={chave}
                          />
                        );
                      })}
                    </Picker>
                  </View>
                )}
                {estaAprovado ? (
                  <Text className="text-xs text-amber-700">
                    A certificação para este par já foi aprovada e não precisa ser reenviada.
                  </Text>
                ) : null}
              </View>

              <View className="gap-1.5">
                <Text className="text-xs font-semibold text-neutral-700 uppercase tracking-wider">
                  Documento comprobatório *
                </Text>
                <Text className="text-xs text-neutral-500">
                  Envie certificado, diploma ou comprovação de proficiência (máx. {TAMANHO_MAXIMO_CERTIFICACAO_MB} MB).
                </Text>

                {arquivo ? (
                  <View className="flex-row items-center justify-between gap-3 rounded-xl border border-neutral-200 bg-neutral-50 p-3.5">
                    <View className="flex-1 flex-row items-center gap-2.5">
                      <Ionicons name="document-text-outline" size={24} color="#8c5230" />
                      <View className="flex-1">
                        <Text className="text-sm font-semibold text-neutral-900" numberOfLines={1}>
                          {arquivo.name}
                        </Text>
                        <Text className="text-xs text-neutral-500">
                          {formatarTamanho(arquivo.size)}
                        </Text>
                      </View>
                    </View>
                    <Pressable
                      accessibilityRole="button"
                      accessibilityLabel="Remover arquivo selecionado"
                      onPress={() => setArquivo(null)}
                      className="h-8 w-8 items-center justify-center rounded-lg hover:bg-neutral-200"
                    >
                      <Ionicons name="trash-outline" size={18} color="#dc2626" />
                    </Pressable>
                  </View>
                ) : (
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel="Selecionar documento de certificação"
                    onPress={() => void handleSelecionarArquivo()}
                    disabled={selecionandoArquivo || enviando}
                    className="flex-row items-center justify-center gap-2 rounded-xl border-2 border-dashed border-neutral-300 bg-neutral-50/50 p-6"
                  >
                    {selecionandoArquivo ? (
                      <ActivityIndicator color="#8c5230" />
                    ) : (
                      <>
                        <Ionicons name="cloud-upload-outline" size={22} color="#8c5230" />
                        <Text className="text-sm font-medium text-neutral-700">
                          Clique para selecionar o arquivo
                        </Text>
                      </>
                    )}
                  </Pressable>
                )}
              </View>

              <View className="rounded-xl bg-amber-50 p-3 flex-row gap-2.5">
                <Ionicons name="information-circle-outline" size={18} color="#b45309" />
                <Text className="flex-1 text-xs text-amber-800 leading-4">
                  O documento será analisado pelo gestor de projetos. Quando aprovado, você estará apto a receber demandas desse par de idiomas.
                </Text>
              </View>
            </View>
          </ScrollView>

          <View className="mt-6 flex-row justify-end gap-3 pt-4 border-t border-neutral-100">
            <Pressable
              accessibilityRole="button"
              onPress={fechar}
              disabled={enviando}
              className="rounded-xl border border-neutral-300 px-4 py-2.5"
            >
              <Text className="text-sm font-medium text-neutral-700">Cancelar</Text>
            </Pressable>
            <Pressable
              accessibilityRole="button"
              onPress={() => void handleEnviar()}
              disabled={enviando || !arquivo || !chaveParSelecionado || estaAprovado}
              className={`flex-row items-center justify-center gap-2 rounded-xl px-5 py-2.5 ${
                enviando || !arquivo || !chaveParSelecionado || estaAprovado
                  ? "bg-neutral-300"
                  : "bg-[#8c5230]"
              }`}
            >
              {enviando ? (
                <ActivityIndicator color="#ffffff" size="small" />
              ) : (
                <Ionicons name="checkmark" size={18} color="#ffffff" />
              )}
              <Text className="text-sm font-semibold text-white">Enviar comprovação</Text>
            </Pressable>
          </View>
        </View>
      </View>
    </Modal>
  );
}
