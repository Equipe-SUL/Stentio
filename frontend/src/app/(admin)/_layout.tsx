import React, { useEffect, useState } from "react";
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
import { Slot, useRouter, usePathname, type Href } from "expo-router";
import { useSafeAreaInsets } from "react-native-safe-area-context";
import { Ionicons } from "@expo/vector-icons";
import { useSession } from "../../lib/session";
type IconName = React.ComponentProps<typeof Ionicons>["name"];
interface MenuGroup {
  title: string;
  icon: IconName;
  items: { name: string; icon: IconName; route: string }[];
}

const MENU_GROUPS: MenuGroup[] = [
  {
    title: "GERAL",
    icon: "grid-outline",
    items: [{ name: "Dashboard", icon: "grid-outline", route: "/" }],
  },
  {
    title: "ATENDIMENTO",
    icon: "chatbubbles-outline",
    items: [
      { name: "Clientes", icon: "people-outline", route: "/clientes" },
      {
        name: "Solicitações",
        icon: "document-text-outline",
        route: "/solicitacoes",
      },
      { name: "Orçamentos", icon: "calculator-outline", route: "/orcamentos" },
    ],
  },
  {
    title: "OPERAÇÃO",
    icon: "construct-outline",
    items: [
      { name: "Recursos", icon: "person-outline", route: "/recursos" },
      { name: "Tabela de Preços", icon: "pricetag-outline", route: "/precos" },
    ],
  },
  {
    title: "CADASTROS BASE",
    icon: "create-outline",
    items: [
      {
        name: "Tipos de Serviço",
        icon: "layers-outline",
        route: "/tipos-servico",
      },
      { name: "Categorias", icon: "folder-outline", route: "/categorias" },
      { name: "Idiomas", icon: "globe-outline", route: "/idiomas" },
    ],
  },
  {
    title: "ADMINISTRAÇÃO",
    icon: "shield-checkmark-outline",
    items: [
      {
        name: "Usuários",
        icon: "shield-checkmark-outline",
        route: "/usuarios",
      },
      { name: "Dados da Empresa", icon: "business-outline", route: "/empresa" },
      { name: "Configuração SMTP", icon: "mail-outline", route: "/smtp" },
      {
        name: "Templates de E-mail",
        icon: "mail-open-outline",
        route: "/templates",
      },
    ],
  },
];
// Grupos da barra inferior (Excluindo o Dashboard)
const MOBILE_BOTTOM_GROUPS = MENU_GROUPS.filter((g) => g.title !== "GERAL");
export default function AdminLayout() {
  const router = useRouter();
  const pathname = usePathname();
  const { sair } = useSession();
  const { width } = useWindowDimensions();
  const insets = useSafeAreaInsets();
  const isMobile = width < 768;
  const [keyboardVisible, setKeyboardVisible] = useState(false);
  // usePathname não inclui o grupo (admin).
  const isRouteActive = (route: string) =>
    route === "/"
      ? pathname === "/"
      : pathname === route || pathname.startsWith(`${route}/`);
  const [activeMobileGroup, setActiveMobileGroup] = useState<string | null>(
    null,
  );
  useEffect(() => {
    setActiveMobileGroup(null);
  }, [pathname, isMobile]);
  useEffect(() => {
    const show = Keyboard.addListener(
      Platform.OS === "ios" ? "keyboardWillShow" : "keyboardDidShow",
      () => {
        setKeyboardVisible(true);
        setActiveMobileGroup(null);
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
        setActiveMobileGroup(null);
        return true;
      },
    );
    return () => subscription.remove();
  }, [activeMobileGroup, isMobile]);

  const handleLogout = async () => {
    await sair();
    router.replace("/");
  };
  const handleNavigate = (route: string) => {
    router.navigate((route === "/" ? "/(admin)" : `/(admin)${route}`) as Href);
    setActiveMobileGroup(null);
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
                  onPress={() => router.push("/(admin)")}
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
                {MENU_GROUPS.map((group, index) => (
                  <View key={index} className="mb-6">
                    <Text className="text-xs font-bold text-[#a1a1aa] mb-2 tracking-wider">
                      {group.title}
                    </Text>
                    {group.items.map((item, i) => {
                      const isActive = isRouteActive(item.route);
                      return (
                        <TouchableOpacity
                          key={i}
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
                            className={`text-sm ${isActive ? "text-white font-semibold" : "text-zinc-300"}`}
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
                  <Text className="text-white font-bold">AD</Text>
                </View>
                <View>
                  <Text className="text-white font-bold text-sm">
                    Administrador
                  </Text>
                  <View className="bg-[#8c5230] px-1.5 py-0.5 rounded mt-1 self-start">
                    <Text className="text-[10px] text-white font-bold tracking-wider">
                      ADMIN
                    </Text>
                  </View>
                </View>
              </View>
              <TouchableOpacity onPress={handleLogout} className="p-2">
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
                accessibilityLabel="Voltar ao dashboard central"
                onPress={() => handleNavigate("/")}
                style={styles.homeButton}
              >
                <Ionicons name="grid-outline" size={32} color="#8c5230" />
              </TouchableOpacity>
              <TouchableOpacity
                accessibilityRole="button"
                accessibilityLabel="Sair da conta"
                onPress={handleLogout}
                style={styles.avatar}
              >
                <Text style={styles.avatarText}>AD</Text>
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
              <Slot />
            </View>
            {isMobile && activeMobileGroup && (
              <View style={styles.overlay}>
                <Pressable
                  accessibilityRole="button"
                  accessibilityLabel="Fechar menu"
                  style={styles.backdrop}
                  onPress={() => setActiveMobileGroup(null)}
                />
                <View style={styles.sheet}>
                  <View style={styles.sheetHeading}>
                    <Text accessibilityRole="header" style={styles.sheetTitle}>
                      {activeMobileGroup}
                    </Text>
                    <TouchableOpacity
                      accessibilityRole="button"
                      accessibilityLabel="Fechar menu"
                      onPress={() => setActiveMobileGroup(null)}
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
                    {MOBILE_BOTTOM_GROUPS.find(
                      (group) => group.title === activeMobileGroup,
                    )?.items.map((item) => {
                      const active = isRouteActive(item.route);
                      return (
                        <TouchableOpacity
                          key={item.route}
                          accessibilityRole="button"
                          accessibilityState={{ selected: active }}
                          onPress={() => handleNavigate(item.route)}
                          style={[
                            styles.menuItem,
                            active && styles.menuItemActive,
                          ]}
                        >
                          <Ionicons
                            name={item.icon}
                            size={22}
                            color="#8c5230"
                          />
                          <Text
                            style={[
                              styles.menuText,
                              active && styles.menuTextActive,
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
                {MOBILE_BOTTOM_GROUPS.map((group) => {
                  const active = activeMobileGroup
                    ? activeMobileGroup === group.title
                    : group.items.some((item) => isRouteActive(item.route));
                  return (
                    <TouchableOpacity
                      key={group.title}
                      accessibilityRole="button"
                      accessibilityLabel={group.title}
                      accessibilityHint="Abre as opções deste grupo"
                      accessibilityState={{
                        expanded: activeMobileGroup === group.title,
                        selected: active,
                      }}
                      onPress={() =>
                        setActiveMobileGroup((current) =>
                          current === group.title ? null : group.title,
                        )
                      }
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
    ...StyleSheet.absoluteFillObject,
    justifyContent: "flex-end",
    zIndex: 30,
  },
  backdrop: {
    ...StyleSheet.absoluteFillObject,
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
  menuText: { flex: 1, marginHorizontal: 12, color: "#3f3f46", fontSize: 15 },
  menuTextActive: { color: "#8c5230", fontWeight: "700" },
});
