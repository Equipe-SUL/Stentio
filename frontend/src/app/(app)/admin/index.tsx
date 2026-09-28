import { HubScreen, type HubItem } from "../../../components/navegacao/HubScreen";

const ITENS: HubItem[] = [
  {
    nome: "Usuários",
    descricao: "Cadastro, perfis de acesso e controle de permissões.",
    icon: "people-outline",
    route: "/configs/usuarios",
  },
  {
    nome: "Notificações",
    descricao: "Registros do sistema e pedidos de confirmação por usuário.",
    icon: "notifications-outline",
  },
  {
    nome: "Configurações",
    descricao: "Base cadastral e estrutural do sistema.",
    icon: "settings-outline",
    route: "/configs",
  },
];

export default function SistemaScreen() {
  return (
    <HubScreen
      breadcrumb="Sistema"
      titulo="Sistema"
      subtitulo="Administração do ambiente: quem acessa, o que está acontecendo e a base de configurações do sistema."
      itens={ITENS}
    />
  );
}
