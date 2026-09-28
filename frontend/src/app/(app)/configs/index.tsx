import { HubScreen, type HubItem } from "../../../components/navegacao/HubScreen";

const ITENS: HubItem[] = [
  {
    nome: "Dados da Empresa",
    descricao: "Identificação, contatos e logo usados nos documentos gerados.",
    icon: "business-outline",
    route: "/configs/empresa",
  },
  {
    nome: "Configuração SMTP",
    descricao: "Servidor de envio dos e-mails do sistema.",
    icon: "mail-outline",
    route: "/configs/smtp",
  },
  {
    nome: "Idiomas",
    descricao: "Idiomas disponíveis para tradução e precificação por par.",
    icon: "language-outline",
    route: "/configs/sistema/catalogo/idiomas",
  },
  {
    nome: "Tipos de Serviço",
    descricao: "Serviços que podem ser orçados e executados pelos recursos.",
    icon: "construct-outline",
    route: "/configs/sistema/catalogo/tipos",
  },
  {
    nome: "Categorias de Projeto",
    descricao: "Agrupamentos usados para classificar e filtrar projetos.",
    icon: "folder-outline",
    route: "/configs/sistema/catalogo/categorias",
  },
  {
    nome: "Tabela de Preços",
    descricao: "Valores por tipo de serviço e par de idiomas, para sugerir o orçamento.",
    icon: "pricetag-outline",
  },
  {
    nome: "Templates de E-mail",
    descricao: "Modelos de comunicação com placeholders dinâmicos.",
    icon: "mail-open-outline",
  },
];

export default function ConfiguracoesScreen() {
  return (
    <HubScreen
      breadcrumb="Configurações do Sistema"
      titulo="Configurações"
      subtitulo="Parâmetros que sustentam o sistema: dados da empresa, envio de e-mail e as bases cadastrais usadas em orçamentos e ordens de serviço."
      itens={ITENS}
    />
  );
}
