import { HubScreen, type HubItem } from "../../../components/navegacao/HubScreen";

const ITENS: HubItem[] = [
  {
    nome: "Importar recursos",
    descricao: "Importação de profissionais, serviços e preços por arquivo CSV.",
    icon: "cloud-upload-outline",
    route: "/importacoes",
  },
  {
    nome: "Usuários",
    descricao: "Cadastro, perfis de acesso e controle de permissões.",
    icon: "people-outline",
    route: "/configs/usuarios",
  },
  {
    nome: "Logs de alterações",
    descricao: "Histórico de mudanças de status em orçamentos, ordens de serviço e etapas.",
    icon: "time-outline",
    route: "/auditoria",
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
