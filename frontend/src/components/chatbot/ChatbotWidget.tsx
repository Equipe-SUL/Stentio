import { useEffect, useRef, useState } from "react";
import {
  ActivityIndicator,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
  useWindowDimensions,
} from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { useRouter } from "expo-router";
import type { Role } from "../../lib/session";
import { podeAcessarRota } from "../../lib/routePermissions";
import { buscarHistoricoChatbot, enviarMensagemChatbot } from "./chatbotApi";
import type { ChatbotAtalho, ChatbotMensagem } from "./types";

function rotaDoAtalho(atalho: ChatbotAtalho): string {
  switch (atalho.tipo) {
    case "OS":
      return `/ordens/${atalho.id}`;
    case "ORCAMENTO":
      return `/orcamentos/${atalho.id}`;
    case "CLIENTE":
      return "/clientes";
  }
}

function idLocal(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;
}

export function ChatbotWidget({ role }: { role: Role }) {
  const router = useRouter();
  const { width, height } = useWindowDimensions();
  const mobile = width < 768;
  const [aberto, setAberto] = useState(false);
  const [mensagem, setMensagem] = useState("");
  const [mensagens, setMensagens] = useState<ChatbotMensagem[]>([]);
  const [conversaId, setConversaId] = useState<string>();
  const [carregandoHistorico, setCarregandoHistorico] = useState(false);
  const [enviando, setEnviando] = useState(false);
  const [erro, setErro] = useState("");
  const listaRef = useRef<ScrollView>(null);
  const historicoCarregado = useRef(false);

  useEffect(() => {
    if (!aberto || historicoCarregado.current) return;
    historicoCarregado.current = true;
    setCarregandoHistorico(true);
    buscarHistoricoChatbot()
      .then((historico) => {
        setConversaId(historico.conversaId);
        setMensagens(historico.mensagens ?? []);
      })
      .catch(() => {
        setErro("Não foi possível carregar a conversa anterior.");
      })
      .finally(() => setCarregandoHistorico(false));
  }, [aberto]);

  async function enviar() {
    const texto = mensagem.trim();
    if (!texto || enviando) return;
    const localId = idLocal();
    setMensagem("");
    setErro("");
    setMensagens((atuais) => [
      ...atuais,
      { id: localId, autor: "USUARIO", conteudo: texto },
    ]);
    setEnviando(true);
    try {
      const resposta = await enviarMensagemChatbot({
        mensagem: texto,
        conversaId,
      });
      if (resposta.conversaId) setConversaId(resposta.conversaId);
      setMensagens((atuais) => [
        ...atuais,
        {
          id: resposta.mensagemId ?? idLocal(),
          autor: "ASSISTENTE",
          conteudo: resposta.resposta,
          atalhos: resposta.atalhos ?? [],
        },
      ]);
    } catch {
      setErro("Não consegui enviar sua mensagem. Tente novamente.");
    } finally {
      setEnviando(false);
    }
  }

  return (
    <View pointerEvents="box-none" style={styles.anchor}>
      {aberto && (
        <KeyboardAvoidingView
          behavior={Platform.OS === "ios" ? "padding" : undefined}
          keyboardVerticalOffset={mobile ? 20 : 0}
          style={[
            styles.panel,
            mobile
              ? { width: Math.max(0, width - 24), height: Math.min(height - 130, 620), bottom: 82, right: 12 }
              : { width: 390, height: Math.min(height - 80, 620), bottom: 86, right: 24 },
          ]}
        >
          <View style={styles.header}>
            <View style={styles.headerIcon}>
              <Ionicons name="sparkles" size={19} color="#fff" />
            </View>
            <View style={styles.headerCopy}>
              <Text style={styles.title}>Assistente Stentio</Text>
              <Text style={styles.subtitle}>Dúvidas e consultas do sistema</Text>
            </View>
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Fechar chatbot"
              hitSlop={10}
              onPress={() => setAberto(false)}
              style={styles.closeButton}
            >
              <Ionicons name="close" size={23} color="#fff" />
            </Pressable>
          </View>

          <ScrollView
            ref={listaRef}
            style={styles.transcript}
            contentContainerStyle={styles.transcriptContent}
            keyboardShouldPersistTaps="handled"
            onContentSizeChange={() => listaRef.current?.scrollToEnd({ animated: true })}
          >
            {carregandoHistorico && (
              <ActivityIndicator color="#8c5230" style={styles.loading} />
            )}
            {!carregandoHistorico && mensagens.length === 0 && (
              <View style={styles.welcome}>
                <Text style={styles.welcomeTitle}>Olá! Como posso ajudar?</Text>
                <Text style={styles.welcomeText}>
                  Pergunte sobre o uso do sistema ou consulte uma OS ou orçamento.
                </Text>
              </View>
            )}
            {mensagens.map((item) => (
              <View
                key={item.id}
                style={[
                  styles.messageRow,
                  item.autor === "USUARIO" && styles.userRow,
                ]}
              >
                <View
                  style={[
                    styles.bubble,
                    item.autor === "USUARIO" ? styles.userBubble : styles.assistantBubble,
                  ]}
                >
                  <Text style={item.autor === "USUARIO" ? styles.userText : styles.assistantText}>
                    {item.conteudo}
                  </Text>
                  {!!item.atalhos?.length && (
                    <View style={styles.shortcuts}>
                      {item.atalhos
                        .filter((atalho) => podeAcessarRota(rotaDoAtalho(atalho), role))
                        .map((atalho) => (
                          <Pressable
                            key={`${atalho.tipo}-${atalho.id}`}
                            accessibilityRole="link"
                            onPress={() => {
                              setAberto(false);
                              router.push(rotaDoAtalho(atalho) as never);
                            }}
                            style={styles.shortcut}
                          >
                            <Ionicons name="open-outline" size={14} color="#6f3e24" />
                            <Text style={styles.shortcutText}>{atalho.titulo}</Text>
                          </Pressable>
                        ))}
                    </View>
                  )}
                </View>
              </View>
            ))}
            {enviando && (
              <View style={styles.typing}>
                <ActivityIndicator size="small" color="#8c5230" />
                <Text style={styles.typingText}>Preparando resposta...</Text>
              </View>
            )}
            {!!erro && <Text accessibilityRole="alert" style={styles.error}>{erro}</Text>}
          </ScrollView>

          <View style={styles.composer}>
            <TextInput
              accessibilityLabel="Mensagem para o chatbot"
              value={mensagem}
              onChangeText={setMensagem}
              placeholder="Escreva sua dúvida..."
              placeholderTextColor="#a1a1aa"
              multiline
              maxLength={2000}
              editable={!enviando}
              onSubmitEditing={() => void enviar()}
              style={styles.input}
            />
            <Pressable
              accessibilityRole="button"
              accessibilityLabel="Enviar mensagem"
              disabled={!mensagem.trim() || enviando}
              onPress={() => void enviar()}
              style={[styles.sendButton, (!mensagem.trim() || enviando) && styles.sendDisabled]}
            >
              <Ionicons name="arrow-up" size={21} color="#fff" />
            </Pressable>
          </View>
        </KeyboardAvoidingView>
      )}
      <Pressable
        accessibilityRole="button"
        accessibilityLabel={aberto ? "Fechar assistente" : "Abrir assistente Stentio"}
        accessibilityState={{ expanded: aberto }}
        onPress={() => setAberto((atual) => !atual)}
        style={[styles.launcher, mobile && styles.launcherMobile]}
      >
        <Ionicons name={aberto ? "close" : "chatbubbles-outline"} size={25} color="#fff" />
      </Pressable>
    </View>
  );
}

const styles = StyleSheet.create({
  anchor: { ...StyleSheet.absoluteFillObject, zIndex: 100, pointerEvents: "box-none" },
  launcher: {
    position: "absolute", right: 24, bottom: 24, width: 58, height: 58,
    borderRadius: 29, alignItems: "center", justifyContent: "center",
    backgroundColor: "#8c5230", elevation: 8, shadowColor: "#2D1A11",
    shadowOffset: { width: 0, height: 4 }, shadowOpacity: 0.25, shadowRadius: 8,
  },
  launcherMobile: { right: 18, bottom: 92 },
  panel: {
    position: "absolute", overflow: "hidden", borderRadius: 20,
    backgroundColor: "#fff", elevation: 18, shadowColor: "#18181b",
    shadowOffset: { width: 0, height: 8 }, shadowOpacity: 0.2, shadowRadius: 20,
  },
  header: { minHeight: 70, backgroundColor: "#2D1A11", flexDirection: "row", alignItems: "center", paddingHorizontal: 16 },
  headerIcon: { width: 36, height: 36, borderRadius: 18, backgroundColor: "#8c5230", alignItems: "center", justifyContent: "center", marginRight: 11 },
  headerCopy: { flex: 1 },
  title: { color: "#fff", fontSize: 15, fontWeight: "700" },
  subtitle: { color: "#e7d8cf", fontSize: 11, marginTop: 3 },
  closeButton: { width: 36, height: 36, alignItems: "center", justifyContent: "center" },
  transcript: { flex: 1, backgroundColor: "#faf9f8" },
  transcriptContent: { padding: 14, flexGrow: 1 },
  loading: { marginTop: 18 },
  welcome: { marginTop: 26, alignItems: "center", paddingHorizontal: 16 },
  welcomeTitle: { color: "#2D1A11", fontWeight: "700", fontSize: 17 },
  welcomeText: { color: "#71717a", textAlign: "center", fontSize: 13, lineHeight: 19, marginTop: 8 },
  messageRow: { alignItems: "flex-start", marginBottom: 10 },
  userRow: { alignItems: "flex-end" },
  bubble: { maxWidth: "88%", borderRadius: 16, paddingHorizontal: 13, paddingVertical: 10 },
  userBubble: { backgroundColor: "#8c5230", borderBottomRightRadius: 5 },
  assistantBubble: { backgroundColor: "#fff", borderColor: "#e4e4e7", borderWidth: 1, borderBottomLeftRadius: 5 },
  userText: { color: "#fff", fontSize: 14, lineHeight: 20 },
  assistantText: { color: "#27272a", fontSize: 14, lineHeight: 20 },
  shortcuts: { borderTopWidth: 1, borderTopColor: "#eee7e2", marginTop: 9, paddingTop: 7, gap: 5 },
  shortcut: { flexDirection: "row", alignItems: "center", gap: 6, paddingVertical: 4 },
  shortcutText: { color: "#6f3e24", fontSize: 12, fontWeight: "600", textDecorationLine: "underline" },
  typing: { flexDirection: "row", alignItems: "center", gap: 8, marginVertical: 8, paddingHorizontal: 8 },
  typingText: { color: "#71717a", fontSize: 12 },
  error: { color: "#b91c1c", fontSize: 12, margin: 8 },
  composer: { minHeight: 64, flexDirection: "row", alignItems: "flex-end", padding: 10, borderTopColor: "#e4e4e7", borderTopWidth: 1, gap: 8, backgroundColor: "#fff" },
  input: { flex: 1, maxHeight: 100, minHeight: 42, borderRadius: 14, backgroundColor: "#f4f4f5", paddingHorizontal: 13, paddingVertical: 10, color: "#27272a", fontSize: 14 },
  sendButton: { width: 42, height: 42, borderRadius: 21, alignItems: "center", justifyContent: "center", backgroundColor: "#8c5230" },
  sendDisabled: { backgroundColor: "#c4b4aa" },
});
