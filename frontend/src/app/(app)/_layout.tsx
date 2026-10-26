import { Redirect, Slot, usePathname, type Href } from "expo-router";
import { ActivityIndicator, View } from "react-native";
import { useSession, type Role } from "../../lib/session";
import { AppShell, type MenuGroup } from "../../components/navegacao/AppShell";
import { homeRouteForRole, podeAcessarRota } from "../../lib/routePermissions";

function menuParaPerfil(role: Role): MenuGroup[] {
  const operacional: MenuGroup["items"] = [
    { name: "Clientes", icon: "people-outline", route: "/clientes" },
  ];

  if (["ADMIN", "ATENDENTE", "GESTOR_PROJETO"].includes(role)) {
    operacional.push(
      { name: "Solicitações", icon: "document-text-outline", route: "/solicitacoes" },
      { name: "Orçamentos", icon: "receipt-outline", route: "/orcamentos" },
    );
  }

  if (["ADMIN", "GESTOR_PROJETO"].includes(role)) {
    operacional.push(
      { name: "Ordens de Serviço", icon: "clipboard-outline", route: "/ordens" },
      { name: "Recursos", icon: "person-outline", route: "/recursos" },
    );
  }

  const grupos: MenuGroup[] = [{ title: "OPERACIONAL", icon: "construct-outline", items: operacional }];

  if (role === "ADMIN") {
    grupos.unshift({
      title: "PAINEL",
      icon: "grid-outline",
      items: [{ name: "Dashboard", icon: "grid-outline" }],
    });
    grupos.push({
      title: "SISTEMA",
      icon: "shield-checkmark-outline",
      items: [{ name: "Central do sistema", icon: "settings-outline", route: "/admin" }],
    });
  } else if (role === "GESTOR_PROJETO" || role === "FINANCEIRO") {
    grupos.push({
      title: "SISTEMA",
      icon: "shield-checkmark-outline",
      items: [{ name: "Catálogo", icon: "settings-outline", route: "/configs" }],
    });
  }

  return grupos;
}

export default function AppLayout() {
  const { usuario, carregando, sair } = useSession();
  const pathname = usePathname();

  if (carregando) {
    return (
      <View className="flex-1 items-center justify-center bg-neutral-50">
        <ActivityIndicator size="large" color="#6f4f28" />
      </View>
    );
  }

  if (!usuario) {
    return <Redirect href="/" />;
  }

  const homeRoute = homeRouteForRole(usuario.role);
  if (!podeAcessarRota(pathname, usuario.role)) {
    return <Redirect href={homeRoute as Href} />;
  }

  return (
    <AppShell
      groups={menuParaPerfil(usuario.role)}
      usuario={usuario}
      homeRoute={homeRoute as Href}
      onSair={sair}
    >
      <Slot />
    </AppShell>
  );
}
