import React, { useCallback, useEffect, useState } from "react";
import { ActivityIndicator, ScrollView, View, Text, Pressable } from "react-native";
import { Ionicons } from "@expo/vector-icons";
import { DataTable } from "../../components/usuarios/DataTable";
import { RoleBadge, StatusBadge } from "../../components/usuarios/Badge";
import { RowActions } from "../../components/usuarios/RowActions";
import type { ColumnDef } from "../../components/usuarios/types";
import { FormularioUsuario, NovoUsuario } from "../../components/usuarios/CreateUser";
import { FormularioEdicaoUsuario, UsuarioAtualizado } from "../../components/usuarios/EditUser";
import { ConfirmDialog } from "../../components/usuarios/ConfirmDialog";
import { api, getApiErrorMessage } from "../../lib/api";

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

const ROLE_POR_CARGO: Record<Cargo, RoleApi> = {
  Admin: "ADMIN",
  Atendente: "ATENDENTE",
  Gestor_Projeto: "GESTOR_PROJETO",
  Financeiro: "FINANCEIRO",
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

export default function UsuariosAdminScreen() {
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [usuarioEmEdicao, setUsuarioEmEdicao] = useState<Usuario | null>(null);
  const [usuarioParaExcluir, setUsuarioParaExcluir] = useState<Usuario | null>(null);

  const [usuarios, setUsuarios] = useState<Usuario[]>([]);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [erroCriacao, setErroCriacao] = useState("");
  const [erroEdicao, setErroEdicao] = useState("");
  const [erroAcao, setErroAcao] = useState("");

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

  const recarregar = useCallback(() => {
    setCarregando(true);
    setErro("");
    return buscarUsuarios()
      .then(setUsuarios)
      .catch((error) => setErro(getApiErrorMessage(error)))
      .finally(() => setCarregando(false));
  }, []);

  async function handleCreateUsuario(novoUsuario: NovoUsuario): Promise<boolean> {
    setErroCriacao("");
    try {
      const { data } = await api.post<UsuarioApi>("/api/v1/usuarios", {
        nome: novoUsuario.nome,
        email: novoUsuario.email,
        senha: novoUsuario.senha,
        role: ROLE_POR_CARGO[novoUsuario.cargo],
      });

      if (novoUsuario.status === "Inativo") {
        await api.patch(`/api/v1/usuarios/${data.id}/desativar`);
      }

      setMostrarFormulario(false);
      await recarregar();
      return true;
    } catch (error) {
      setErroCriacao(getApiErrorMessage(error));
      return false;
    }
  }

  async function handleUpdateUsuario(dados: UsuarioAtualizado): Promise<boolean> {
    if (!usuarioEmEdicao) return false;

    setErroEdicao("");
    try {
      await api.put<UsuarioApi>(`/api/v1/usuarios/${usuarioEmEdicao.id}`, {
        nome: dados.nome,
        email: dados.email,
        role: ROLE_POR_CARGO[dados.cargo],
        ...(dados.senha.trim() ? { senha: dados.senha } : {}),
      });

      if (dados.status !== usuarioEmEdicao.status) {
        const acao = dados.status === "Ativo" ? "ativar" : "desativar";
        await api.patch(`/api/v1/usuarios/${usuarioEmEdicao.id}/${acao}`);
      }

      setUsuarioEmEdicao(null);
      await recarregar();
      return true;
    } catch (error) {
      setErroEdicao(getApiErrorMessage(error));
      return false;
    }
  }

  async function handleToggleStatus(usuario: Usuario) {
    setErroAcao("");
    const acao = usuario.status === "Ativo" ? "desativar" : "ativar";
    try {
      await api.patch(`/api/v1/usuarios/${usuario.id}/${acao}`);
      await recarregar();
    } catch (error) {
      setErroAcao(getApiErrorMessage(error));
    }
  }

  function handleSolicitarExclusao() {
    if (!usuarioEmEdicao) return;
    setUsuarioParaExcluir(usuarioEmEdicao);
    setUsuarioEmEdicao(null);
  }

  async function handleConfirmarExclusao() {
    if (!usuarioParaExcluir) return;

    setErroAcao("");
    try {
      await api.delete(`/api/v1/usuarios/${usuarioParaExcluir.id}`);
      setUsuarioParaExcluir(null);
      await recarregar();
    } catch (error) {
      setUsuarioParaExcluir(null);
      setErroAcao(getApiErrorMessage(error));
    }
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
          onEdit={() => {
            setErroEdicao("");
            setUsuarioEmEdicao(usuario);
          }}
          onToggle={() => handleToggleStatus(usuario)}
        />
      ),
    },
  ];

  return (
    <ScrollView
      className="flex-1 bg-[#f4f4f5]"
      contentContainerClassName="p-8"
      showsVerticalScrollIndicator={false}
      keyboardShouldPersistTaps="handled"
    >
      <View className="w-full gap-6">
        <View className="flex-row flex-wrap items-center justify-between gap-3 mb-2">
          <View className="shrink">
            <Text className="text-3xl font-bold text-zinc-900 mb-1">Usuários</Text>
            <Text className="text-sm text-zinc-500">
              Gerenciamento de usuários do sistema
            </Text>
          </View>

          <View className="flex-row items-center gap-3">
            <Pressable
              onPress={() => {
                setErroCriacao("");
                setMostrarFormulario(true);
              }}
              className="flex-row items-center gap-2 rounded-lg bg-[#8c5230] px-5 py-3 hover:bg-[#6f4f28] transition-colors shadow-sm"
            >
              <Ionicons name="add" size={18} color="#ffffff" />
              <Text className="font-medium text-white">Novo utilizador</Text>
            </Pressable>
          </View>
        </View>

        {erroAcao ? (
          <View className="rounded-lg border border-red-200 bg-red-50 p-3">
            <Text className="text-sm font-medium text-red-600">{erroAcao}</Text>
          </View>
        ) : null}

        <View className="shadow-sm border border-zinc-100 rounded-2xl overflow-hidden bg-white">
          {carregando ? (
            <View className="items-center justify-center gap-3 bg-white p-10">
              <ActivityIndicator color="#8c5230" />
              <Text className="text-sm text-zinc-500">A carregar utilizadores...</Text>
            </View>
          ) : erro ? (
            <View className="items-center justify-center gap-3 bg-white p-10">
              <Text className="text-center text-sm text-red-600">{erro}</Text>
              <Pressable
                onPress={recarregar}
                className="rounded-lg border border-zinc-300 px-5 py-3 hover:bg-zinc-50"
              >
                <Text className="font-medium text-zinc-700">Tentar novamente</Text>
              </Pressable>
            </View>
          ) : (
            <DataTable
              data={usuarios}
              columns={columns}
              keyExtractor={(usuario) => usuario.id}
              onRowPress={() => {}}
              emptyMessage="Nenhum utilizador registado"
              scrollEnabled={false}
            />
          )}
        </View>
      </View>

      <FormularioUsuario
        visible={mostrarFormulario}
        onSubmit={handleCreateUsuario}
        onCancel={() => setMostrarFormulario(false)}
        erro={erroCriacao}
      />

      <FormularioEdicaoUsuario
        visible={usuarioEmEdicao !== null}
        usuario={usuarioEmEdicao}
        onSubmit={handleUpdateUsuario}
        onCancel={() => setUsuarioEmEdicao(null)}
        onDelete={handleSolicitarExclusao}
        erro={erroEdicao}
      />

      <ConfirmDialog
        visible={usuarioParaExcluir !== null}
        title="Excluir utilizador"
        message={`Tem a certeza que deseja excluir ${usuarioParaExcluir?.nome}? Esta ação não pode ser desfeita.`}
        confirmLabel="Excluir"
        onConfirm={handleConfirmarExclusao}
        onCancel={() => setUsuarioParaExcluir(null)}
      />
    </ScrollView>
  );
}