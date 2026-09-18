import { useState } from "react";
import { ScrollView, View, Text, Pressable } from "react-native";
import { DataTable } from "../../../components/usuarios/DataTable";
import { RoleBadge, StatusBadge } from "../../../components/usuarios/Badge";
import { RowActions } from "../../../components/usuarios/RowActions";
import type { ColumnDef } from "../../../components/usuarios/types";
import { FormularioUsuario, NovoUsuario } from "../../../components/usuarios/CreateUser";
import { FormularioEdicaoUsuario, UsuarioAtualizado } from "../../../components/usuarios/EditUser";
import { ConfirmDialog } from "../../../components/usuarios/ConfirmDialog";
import { useSession } from "../../../lib/session";

type Cargo = "Gestor_Projeto" | "Atendente" | "Admin" | "Financeiro";
type Status = "Ativo" | "Inativo";

interface Usuario {
  id: string;
  nome: string;
  email: string;
  cargo: Cargo;
  status: Status;
  senha: string;
}

export default function TabelaUsuarios() {
  const { sair } = useSession();
  const [mostrarFormulario, setMostrarFormulario] = useState(false);
  const [usuarioEmEdicao, setUsuarioEmEdicao] = useState<Usuario | null>(null);
  const [usuarioParaExcluir, setUsuarioParaExcluir] = useState<Usuario | null>(null);

  const [usuarios, setUsuarios] = useState<Usuario[]>([
    {
      id: "1",
      nome: "João Silva",
      email: "joao@email.com",
      cargo: "Gestor_Projeto",
      status: "Ativo",
      senha: "123456",
    },
    {
      id: "2",
      nome: "Maria Souza",
      email: "maria@email.com",
      cargo: "Financeiro",
      status: "Ativo",
      senha: "123456",
    },
    {
      id: "3",
      nome: "Carlos Lima",
      email: "carlos@email.com",
      cargo: "Atendente",
      status: "Inativo",
      senha: "123456",
    },
  ]);

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