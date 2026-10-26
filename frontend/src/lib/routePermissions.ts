import type { Role } from "./session";

const ROLES_OPERACIONAIS: readonly Role[] = ["ADMIN", "ATENDENTE", "GESTOR_PROJETO"];
const ROLES_CATALOGO: readonly Role[] = ["ADMIN", "FINANCEIRO", "GESTOR_PROJETO"];
const ROLES_GESTAO: readonly Role[] = ["ADMIN", "GESTOR_PROJETO"];

const ROLE_HOME: Record<Role, "/admin" | "/orcamentos" | "/ordens" | "/clientes"> = {
  ADMIN: "/admin",
  ATENDENTE: "/orcamentos",
  GESTOR_PROJETO: "/ordens",
  FINANCEIRO: "/clientes",
};

export function homeRouteForRole(role: Role) {
  return ROLE_HOME[role];
}

function correspondeARota(pathname: string, prefixo: string): boolean {
  return pathname === prefixo || pathname.startsWith(`${prefixo}/`);
}

export function podeAcessarRota(pathname: string, role: Role): boolean {
  if (role === "ADMIN") return true;

  if (correspondeARota(pathname, "/clientes")) return true;
  if (correspondeARota(pathname, "/solicitacoes")) return ROLES_OPERACIONAIS.includes(role);
  if (correspondeARota(pathname, "/orcamentos")) return ROLES_OPERACIONAIS.includes(role);
  if (correspondeARota(pathname, "/ordens")) return ROLES_GESTAO.includes(role);
  if (correspondeARota(pathname, "/recursos")) return ROLES_GESTAO.includes(role);
  if (correspondeARota(pathname, "/admin")) return false;

  if (correspondeARota(pathname, "/configs/usuarios")) return false;
  if (correspondeARota(pathname, "/configs/empresa")) return false;
  if (correspondeARota(pathname, "/configs/smtp")) return false;
  if (correspondeARota(pathname, "/configs/sistema/catalogo")) return ROLES_CATALOGO.includes(role);
  if (pathname === "/configs") return ROLES_CATALOGO.includes(role);
  if (correspondeARota(pathname, "/configs")) return false;

  return false;
}
