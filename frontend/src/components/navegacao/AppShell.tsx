import React, { useEffect, useState, type ReactNode } from "react";
import {
  View,
  Text,
  TouchableOpacity,
  ScrollView,
  Platform,
  Pressable,
  StyleSheet,
  useWindowDimensions,
  BackHandler,
  Keyboard,
} from "react-native";
import { useRouter, usePathname, type Href } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import type { UsuarioLogado } from "../../lib/session";

export type IconName = React.ComponentProps<typeof Ionicons>["name"];

export interface MenuItem {
  name: string;
  icon: IconName;
  route?: Href;
}

export interface MenuGroup {
  title: string;
  icon: IconName;
  items: MenuItem[];
}

interface GrupoAberto {
  titulo: string;
  rota: string;
  mobile: boolean;
}

interface AppShellProps {
  groups: MenuGroup[];
  usuario: UsuarioLogado;
  homeRoute: Href;
  onSair: () => void | Promise<void>;
  children: ReactNode;
}

function iniciaisDoNome(nome: string): string {
  const partes = nome.trim().split(/\s+/).filter(Boolean);
  if (partes.length === 0) return "";
  if (partes.length === 1) return partes[0].slice(0, 2).toUpperCase();
  return (partes[0][0] + partes[partes.length - 1][0]).toUpperCase();
}

export function AppShell({
  groups,
  usuario,
  homeRoute,
  onSair,
  children,
}: AppShellProps) {
  const router = useRouter();
  const pathname = usePathname();
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const isMobile = width < 768;
  const [keyboardVisible, setKeyboardVisible] = useState(false);
  const [grupoAberto, setGrupoAberto] = useState<GrupoAberto | null>(null);
  const iniciais = iniciaisDoNome(usuario.nome);

  const isRouteActive = (route?: Href) => {
    if (!route) return false;
    return pathname === route || pathname.startsWith(`${route}/`);
  };

  const activeMobileGroup =
    grupoAberto &&
    grupoAberto.rota === pathname &&
    grupoAberto.mobile === isMobile
      ? grupoAberto.titulo
      : null;

  useEffect(() => {
    const show = Keyboard.addListener(
      Platform.OS === "ios" ? "keyboardWillShow" : "keyboardDidShow",
      () => {
        setKeyboardVisible(true);
        setGrupoAberto(null);
      },
    );
    const hide = Keyboard.addListener(
      Platform.OS === "ios" ? "keyboardWillHide" : "keyboardDidHide",
      () => setKeyboardVisible(false),
    );
    return () => {
      show.remove();
      hide.remove();
    };
  }, []);

  useEffect(() => {
    if (!isMobile || !activeMobileGroup) return;
    const subscription = BackHandler.addEventListener(
      "hardwareBackPress",
      () => {
        setGrupoAberto(null);
        return true;
      },
    );
    return () => subscription.remove();
  }, [activeMobileGroup, isMobile]);

  const handleNavigate = (route?: Href) => {
    if (!route) return;
    router.navigate(route);
    setGrupoAberto(null);
  };

  return (
    <View className="flex-1 bg-[#f4f4f5]">
      <View
        style={{
          flex: 1,
          flexDirection: isMobile ? "column" : "row",
          paddingLeft: insets.left,
          paddingRight: insets.right,
        }}
      >
        {/* ========================================== */}
        {/* SIDEBAR DESKTOP (Escondida no telemóvel)   */}
        {/* ========================================== */}
        {!isMobile && (
          <View className="w-64 bg-[#2D1A11] h-full flex-col justify-between">
            <View className="flex-1">
              <View className="p-6 flex-row items-center justify-between">
                <TouchableOpacity
                  onPress={() => handleNavigate(homeRoute)}
                  className="flex-row items-center"
                >
                  <View className="w-8 h-8 bg-[#8c5230] rounded items-center justify-center mr-3">
                    <Text className="text-white font-bold text-lg">S</Text>
                  </View>
                  <Text className="text-white font-bold text-xl tracking-wide">
                    Stentio
                  </Text>
                </TouchableOpacity>
              </View>
              <ScrollView
                className="flex-1 px-4"
                showsVerticalScrollIndicator={false}
              >
                {groups.map((group, index) => (
                  <View key={index} className="mb-6">
                    <Text className="text-xs font-bold text-[#a1a1aa] mb-2 tracking-wider">
                      {group.title}
                    </Text>
                    {group.items.map((item) => {
                      const disponivel = item.route !== undefined;
                      const isActive = isRouteActive(item.route);
                      return (
                        <TouchableOpacity
                          key={item.name}
                          accessibilityRole="button"
                          accessibilityState={{
                            selected: isActive,
                            disabled: !disponivel,
                          }}
                          disabled={!disponivel}
                          onPress={() => handleNavigate(item.route)}
                          className={`flex-row items-center px-3 py-2.5 rounded-lg mb-1 transition-colors ${isActive ? "bg-[#8c5230]" : "hover:bg-white/5"}`}
                        >
                          <Ionicons
                            name={item.icon}
                            size={20}
                            color={isActive ? "#ffffff" : "#d4d4d8"}
                            style={{ marginRight: 12 }}
                          />
                          <Text
                            className={`text-sm ${isActive ? "text-white font-semibold" : disponivel ? "text-zinc-300" : "text-zinc-500"}`}
                          >
                            {item.name}
                          </Text>
                        </TouchableOpacity>
                      );
                    })}
                  </View>
                ))}
              </ScrollView>
            </View>
            <View className="p-4 border-t border-white/10 flex-row items-center justify-between hover:bg-white/5 transition-colors">
              <View className="flex-row items-center">
                <View className="w-10 h-10 bg-[#8c5230] rounded-full items-center justify-center mr-3">
                  <Text className="text-white font-bold">{iniciais}</Text>
                </View>
                <View className="flex-1">
                  <Text
                    className="text-white font-bold text-sm"
                    numberOfLines={1}
                  >
                    {usuario.nome}
                  </Text>
                  <View className="bg-[#8c5230] px-1.5 py-0.5 rounded mt-1 self-start">
                    <Text className="text-[10px] text-white font-bold tracking-wider">
                      {usuario.role}
                    </Text>
                  </View>
                </View>
              </View>
              <TouchableOpacity onPress={onSair} className="p-2">
                <Ionicons name="log-out-outline" size={22} color="#a1a1aa" />
              </TouchableOpacity>
            </View>
          </View>
        )}
        {/* ========================================== */}
        {/* ÁREA CENTRAL & MOBILE                        */}
        {/* ========================================== */}
        <View
          className="flex-1 flex-col bg-[#f4f4f5]"
          style={{ minWidth: 0, minHeight: 0 }}
        >
          {/* O cabeçalho e o dock ficam fora do scroll das páginas. */}
          {isMobile && (
            <View style={[styles.header, { paddingTop: insets.top + 8 }]}>
              <TouchableOpacity
                accessibilityRole="button"
                accessibilityLabel="Ir para o início"
                onPress={() => handleNavigate(homeRoute)}
                style={styles.homeButton}
              >
                <Ionicons name="grid-outline" size={32} color="#8c5230" />
              </TouchableOpacity>
              <TouchableOpacity
                accessibilityRole="button"
                accessibilityLabel="Sair da conta"
                onPress={onSair}
                style={styles.avatar}
              >
                <Text style={styles.avatarText}>{iniciais}</Text>
              </TouchableOpacity>
            </View>
          )}

          <View style={styles.content}>
            <View
              style={styles.content}
              accessibilityElementsHidden={isMobile && !!activeMobileGroup}
              importantForAccessibility={
                isMobile && activeMobileGroup ? "no-hide-descendants" : "auto"
              }
            >
              {children}
            </View>
            {isMobile && activeMobileGroup && (
              <View style={styles.overlay}>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Fechar menu"
                  style={styles.backdrop}
                  onPress={() => setGrupoAberto(null)}
                />
                <View style={styles.sheet}>
                  <View style={styles.sheetHeading}>
                    <Text accessibilityRole="header" style={styles.sheetTitle}>
                      {activeMobileGroup}
                    </Text>
                    <TouchableOpacity
                      accessibilityRole="button"
                      accessibilityLabel="Fechar menu"
                      onPress={() => setGrupoAberto(null)}
                      style={styles.closeButton}
                    >
                      <Ionicons
                        name="close-outline"
                        size={24}
                        color="#52525b"
                      />
                    </TouchableOpacity>
                  </View>
                  <ScrollView
                    bounces={false}
                    keyboardShouldPersistTaps="handled"
                    showsVerticalScrollIndicator={false}
                  >
                    {groups
                      .find((group) => group.title === activeMobileGroup)
                      ?.items.map((item) => {
                        const disponivel = item.route !== undefined;
                        const active = isRouteActive(item.route);
                        return (
                          <TouchableOpacity
                            key={item.name}
                            accessibilityRole="button"
                            accessibilityState={{
                              selected: active,
                              disabled: !disponivel,
                            }}
                            disabled={!disponivel}
                            onPress={() => handleNavigate(item.route)}
                            style={[
                              styles.menuItem,
                              active && styles.menuItemActive,
                              !disponivel && styles.menuItemDisabled,
                            ]}
                          >
                            <Ionicons
                              name={item.icon}
                              size={22}
                              color={disponivel ? "#8c5230" : "#a1a1aa"}
                            />
                            <Text
                              style={[
                                styles.menuText,
                                active && styles.menuTextActive,
                                !disponivel && styles.menuTextDisabled,
                              ]}
                            >
                              {item.name}
                            </Text>
                            <Ionicons
                              name="chevron-forward"
                              size={16}
                              color="#a1a1aa"
                            />
                          </TouchableOpacity>
                        );
                      })}
                  </ScrollView>
                </View>
              </View>
            )}
          </View>

          {isMobile && !keyboardVisible && (
            <View
              style={[
                styles.dockArea,
                { paddingBottom: Math.max(insets.bottom, 12) },
              ]}
            >
              <View style={styles.dock}>
                {groups.map((group) => {
                  const unico = group.items.length === 1 ? group.items[0] : null;
                  const rotaDireta = unico?.route;
                  const active = activeMobileGroup
                    ? activeMobileGroup === group.title
                    : group.items.some(
                        (item) => item.route && isRouteActive(item.route),
                      );
                  return (
                    <TouchableOpacity
                      key={group.title}
                      accessibilityRole="button"
                      accessibilityLabel={group.title}
                      accessibilityHint={
                        rotaDireta
                          ? "Abre esta seção"
                          : "Abre as opções deste grupo"
                      }
                      accessibilityState={{
                        expanded: activeMobileGroup === group.title,
                        selected: active,
                      }}
                      onPress={() => {
                        if (rotaDireta) {
                          setGrupoAberto(null);
                          router.navigate(rotaDireta);
                          return;
                        }
                        setGrupoAberto((current) =>
                          current?.titulo === group.title
                            ? null
                            : {
                                titulo: group.title,
                                rota: pathname,
                                mobile: isMobile,
                              },
                        );
                      }}
                      style={[
                        styles.dockButton,
                        active && styles.dockButtonActive,
                      ]}
                    >
                      <Ionicons
                        name={group.icon}
                        size={27}
                        color={active ? "#ffffff" : "#d6c5ba"}
                      />
                      <View
                        style={[styles.activeDot, { opacity: active ? 1 : 0 }]}
                      />
                    </TouchableOpacity>
                  );
                })}
              </View>
            </View>
          )}
        </View>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  content: { flex: 1, minHeight: 0, minWidth: 0 },
  header: {
    flexDirection: "row",
    justifyContent: "space-between",
    alignItems: "center",
    paddingHorizontal: 20,
    paddingBottom: 10,
    backgroundColor: "#f4f4f5",
  },
  homeButton: {
    width: 48,
    height: 48,
    alignItems: "center",
    justifyContent: "center",
  },
  avatar: {
    width: 42,
    height: 42,
    borderRadius: 21,
    backgroundColor: "#8c5230",
    alignItems: "center",
    justifyContent: "center",
  },
  avatarText: { color: "#fff", fontWeight: "700", fontSize: 14 },
  dockArea: {
    paddingTop: 10,
    paddingHorizontal: 16,
    backgroundColor: "#f4f4f5",
  },
  dock: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-around",
    padding: 8,
    borderRadius: 26,
    backgroundColor: "#2D1A11",
    width: "100%",
    maxWidth: 480,
    alignSelf: "center",
    shadowColor: "#2D1A11",
    shadowOffset: { width: 0, height: 4 },
    shadowOpacity: 0.18,
    shadowRadius: 10,
    elevation: 6,
  },
  dockButton: {
    flex: 1,
    maxWidth: 80,
    minHeight: 54,
    borderRadius: 18,
    alignItems: "center",
    justifyContent: "center",
  },
  dockButtonActive: { backgroundColor: "#8c5230" },
  activeDot: {
    width: 4,
    height: 4,
    borderRadius: 2,
    backgroundColor: "#fff",
    marginTop: 4,
  },
  overlay: {
    ...StyleSheet.absoluteFill,
    justifyContent: "flex-end",
    zIndex: 30,
  },
  backdrop: {
    ...StyleSheet.absoluteFill,
    backgroundColor: "rgba(0,0,0,0.3)",
  },
  sheet: {
    margin: 16,
    marginBottom: 4,
    padding: 12,
    borderRadius: 24,
    backgroundColor: "#fff",
    maxHeight: "95%",
    width: "auto",
  },
  sheetHeading: {
    flexDirection: "row",
    alignItems: "center",
    justifyContent: "space-between",
    paddingLeft: 12,
  },
  sheetTitle: {
    flex: 1,
    color: "#8c5230",
    fontSize: 12,
    fontWeight: "700",
    letterSpacing: 1,
  },
  closeButton: {
    width: 44,
    height: 44,
    alignItems: "center",
    justifyContent: "center",
  },
  menuItem: {
    flexDirection: "row",
    alignItems: "center",
    padding: 14,
    minHeight: 52,
    borderRadius: 14,
    marginTop: 4,
  },
  menuItemActive: { backgroundColor: "#f5ede7" },
  menuItemDisabled: { opacity: 0.45 },
  menuText: { flex: 1, marginHorizontal: 12, color: "#3f3f46", fontSize: 15 },
  menuTextActive: { color: "#8c5230", fontWeight: "700" },
  menuTextDisabled: { color: "#a1a1aa" },
});
