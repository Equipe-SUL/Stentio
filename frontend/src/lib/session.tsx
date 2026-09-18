import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
  type ReactNode,
} from "react";
import { api } from "./api";
import { clearToken, setToken } from "./auth";

export type Role = "ADMIN" | "ATENDENTE" | "GESTOR_PROJETO" | "FINANCEIRO";

export interface UsuarioLogado {
  id: string;
  nome: string;
  email: string;
  role: Role;
  ativo: boolean;
}

interface LoginResponse {
  token: string;
  tipo: string;
  expiresIn: number;
  email: string;
  role: Role;
}

interface SessionContextValue {
  usuario: UsuarioLogado | null;
  carregando: boolean;
  entrar: (email: string, senha: string, persistir: boolean) => Promise<UsuarioLogado>;
  sair: () => Promise<void>;
}

const SessionContext = createContext<SessionContextValue | null>(null);

export function SessionProvider({ children }: { children: ReactNode }) {
  // undefined = ainda validando a sessão; null = sem sessão.
  const [usuario, setUsuario] = useState<UsuarioLogado | null | undefined>(undefined);
  const carregando = usuario === undefined;

  useEffect(() => {
    let ativo = true;
    // Web: valida pelo cookie HttpOnly. Nativo: o interceptor manda o Bearer.
    api
      .get<UsuarioLogado>("/api/v1/eu")
      .then(({ data }) => {
        if (ativo) setUsuario(data);
      })
      .catch(() => {
        if (ativo) setUsuario(null);
      });
    return () => {
      ativo = false;
    };
  }, []);

  const entrar = useCallback(
    async (email: string, senha: string, persistir: boolean) => {
      const { data } = await api.post<LoginResponse>("/api/v1/usuarios/login", {
        email,
        senha,
      });
      await setToken(data.token, persistir);
      const { data: perfil } = await api.get<UsuarioLogado>("/api/v1/eu");
      setUsuario(perfil);
      return perfil;
    },
    []
  );

  const sair = useCallback(async () => {
    try {
      await api.post("/api/v1/usuarios/logout");
    } finally {
      await clearToken();
      setUsuario(null);
    }
  }, []);

  const value = useMemo(
    () => ({ usuario: usuario ?? null, carregando, entrar, sair }),
    [usuario, carregando, entrar, sair]
  );

  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const contexto = useContext(SessionContext);
  if (!contexto) {
    throw new Error("useSession deve ser usado dentro de <SessionProvider>");
  }
  return contexto;
}
