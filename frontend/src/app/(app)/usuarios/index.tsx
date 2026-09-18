import { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, ScrollView, View, Text, Pressable } from "react-native";
import { DataTable } from "../../../components/usuarios/DataTable";
import { RoleBadge, StatusBadge } from "../../../components/usuarios/Badge";
import { RowActions } from "../../../components/usuarios/RowActions";
import type { ColumnDef } from "../../../components/usuarios/types";
import { FormularioUsuario, NovoUsuario } from "../../../components/usuarios/CreateUser";
import { FormularioEdicaoUsuario, UsuarioAtualizado } from "../../../components/usuarios/EditUser";
import { ConfirmDialog } from "../../../components/usuarios/ConfirmDialog";
import { useSession } from "../../../lib/session";
import { api, getApiErrorMessage } from "../../../lib/api";

type Cargo = "Gestor_Projeto" | "Atendente" | "Admin" | "Financeiro";
type Status = "Ativo" | "Inativo";
type RoleApi = "ADMIN" | "ATENDENTE" | "GESTOR_PROJETO" | "FINANCEIRO";

interface Usuario {
  id: string;
  nome: string;
  email: string;
  cargo: Cargo;
  status: Status;
  senha: string;
}

interface UsuarioApi {
  id: string;
  nome: string;
  email: string;
  role: RoleApi;
  ativo: boolean;
}

const CARGO_POR_ROLE: Record<RoleApi, Cargo> = {
  ADMIN: "Admin",
  ATENDENTE: "Atendente",
  GESTOR_PROJETO: "Gestor_Projeto",
  FINANCEIRO: "Financeiro",
};

function paraUsuario({ id, nome, email, role, ativo }: UsuarioApi): Usuario {
  return {
    id,
    nome,
    email,
    cargo: CARGO_POR_ROLE[role],
    status: ativo ? "Ativo" : "Inativo",
    senha: "",
  };
}

async function buscarUsuarios(): Promise<Usuario[]> {
  const { data } = await api.get<UsuarioApi[]>("/api/v1/usuarios");
  return data.map(paraUsuario);
}

export default function TabelaUsuarios() {
  const { sair } = useSession();
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [usuarioEmEdicao, setUsuarioEmEdicao] = useState<Usuario | null>(null);
  const [usuarioParaExcluir, setUsuarioParaExcluir] = useState<Usuario | null>(null);

  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");

  useEffect(() => {
    let ativo = true;
    buscarUsuarios()
      .then((lista) => {
        if (ativo) setUsuarios(lista);
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

  const tentarNovamente = useCallback(() => {
    setCarregando(true);
    setErro("");
    buscarUsuarios()
      .then(setUsuarios)
      .catch((error) => setErro(getApiErrorMessage(error)))
      .finally(() => setCarregando(false));
  }, []);

  function handleCreateUsuario(novoUsuario: NovoUsuario) {
    const usuario: Usuario = {
      id: String(Date.now()),
      ...novoUsuario,
    };

    setUsuarios((usuariosAtuais) => [...usuariosAtuais, usuario]);
    setMostrarFormulario(false);
  }

  function handleUpdateUsuario(dados: UsuarioAtualizado) {
    if (!usuarioEmEdicao) return;

    setUsuarios((usuariosAtuais) =>
      usuariosAtuais.map((u) => (u.id === usuarioEmEdicao.id ? { ...u, ...dados } : u))
    );

    setUsuarioEmEdicao(null);
  }

  function handleToggleStatus(usuario: Usuario) {
    setUsuarios((usuariosAtuais) =>
      usuariosAtuais.map((u) =>
        u.id === usuario.id ? { ...u, status: u.status === "Ativo" ? "Inativo" : "Ativo" } : u
      )
    );
  }

  // Excluir é acionado de dentro do FormularioEdicaoUsuario.
  // Fecha o form de edição e abre a confirmação por cima.
  function handleSolicitarExclusao() {
    if (!usuarioEmEdicao) return;
    setUsuarioParaExcluir(usuarioEmEdicao);
    setUsuarioEmEdicao(null);
  }

  function handleConfirmarExclusao() {
    if (!usuarioParaExcluir) return;

    setUsuarios((usuariosAtuais) => usuariosAtuais.filter((u) => u.id !== usuarioParaExcluir.id));
    setUsuarioParaExcluir(null);
  }

  const columns: ColumnDef<Usuario>[] = [
    { key: "nome", header: "Nome" },
    { key: "email", header: "Email" },
    {
      key: "cargo",
      header: "Cargo",
      render: (usuario) => <RoleBadge role={usuario.cargo} />,
    },
    {
      key: "status",
      header: "Status",
      render: (usuario) => <StatusBadge status={usuario.status} />,
    },
    {
      key: "id",
      header: "",
      width: 180,
      align: "right",
      render: (usuario) => (
        <RowActions
          toggleLabel={usuario.status === "Ativo" ? "Desativar" : "Ativar"}
          onEdit={() => setUsuarioEmEdicao(usuario)}
          onToggle={() => handleToggleStatus(usuario)}
        />
      ),
    },
  ];

return (
  <ScrollView
    className="flex-1 bg-neutral-50"
    contentContainerClassName="gap-6 p-6 web:px-24 web:py-12 web:items-center"
    keyboardShouldPersistTaps="handled"
  >
    <View className="w-full web:max-w-6xl gap-6">
      <View className="flex-row flex-wrap items-center justify-between gap-3">
        <View className="shrink">
          <Text className="text-2xl font-bold text-neutral-900">Usuários</Text>
          <Text className="text-sm text-neutral-500">
            Gerenciamento de usuários do sistema
          </Text>
        </View>

        <View className="flex-row items-center gap-3">
          <Pressable
            onPress={() => sair()}
            className="rounded-lg border border-neutral-300 px-5 py-3"
          >
            <Text className="font-medium text-neutral-700">Sair</Text>
          </Pressable>

          <Pressable
            onPress={() => setMostrarFormulario(true)}
            className="rounded-lg bg-[#6f4f28] px-5 py-3"
          >
            <Text className="font-medium text-white">Novo usuário</Text>
          </Pressable>
        </View>
      </View>

      <View className="shadow-lg">
        {carregando ? (
          <View className="items-center justify-center gap-3 rounded-2xl bg-white p-10">
            <ActivityIndicator color="#6f4f28" />
            <Text className="text-sm text-neutral-500">Carregando usuários...</Text>
          </View>
        ) : erro ? (
          <View className="items-center justify-center gap-3 rounded-2xl bg-white p-10">
            <Text className="text-center text-sm text-red-600">{erro}</Text>
            <Pressable
              onPress={tentarNovamente}
              className="rounded-lg border border-neutral-300 px-5 py-3"
            >
              <Text className="font-medium text-neutral-700">Tentar de novo</Text>
            </Pressable>
          </View>
        ) : (
          <DataTable
            data={usuarios}
            columns={columns}
            keyExtractor={(usuario) => usuario.id}
            onRowPress={(usuario) => {
              console.log("Usuário selecionado:", usuario);
            }}
            emptyMessage="Nenhum usuário cadastrado"
            scrollEnabled={false}
          />
        )}
      </View>
    </View>

    <FormularioUsuario
      visible={mostrarFormulario}
      onSubmit={handleCreateUsuario}
      onCancel={() => setMostrarFormulario(false)}
    />

    <FormularioEdicaoUsuario
      visible={usuarioEmEdicao !== null}
      usuario={usuarioEmEdicao}
      onSubmit={handleUpdateUsuario}
      onCancel={() => setUsuarioEmEdicao(null)}
      onDelete={handleSolicitarExclusao}
    />

    <ConfirmDialog
      visible={usuarioParaExcluir !== null}
      title="Excluir usuário"
      message={`Tem certeza que deseja excluir ${usuarioParaExcluir?.nome}? Essa ação não pode ser desfeita.`}
      confirmLabel="Excluir"
      onConfirm={handleConfirmarExclusao}
      onCancel={() => setUsuarioParaExcluir(null)}
    />
  </ScrollView>
  );
}