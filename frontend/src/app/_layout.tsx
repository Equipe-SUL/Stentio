import './global.css';
import { Stack, usePathname } from 'expo-router';
import Head from 'expo-router/head';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { SessionProvider } from '../lib/session';

const titulosPorRota: Record<string, string> = {
  '/admin': 'Central do sistema',
  '/auditoria': 'Logs de alterações',
  '/cadastro-recurso': 'Cadastro de recurso',
  '/clientes': 'Clientes',
  '/configs': 'Configurações',
  '/configs/empresa': 'Dados da empresa',
  '/configs/sistema/catalogo/categorias': 'Categorias de projeto',
  '/configs/sistema/catalogo/idiomas': 'Idiomas',
  '/configs/sistema/catalogo/tipos': 'Tipos de serviço',
  '/configs/smtp': 'Servidor SMTP',
  '/configs/usuarios': 'Usuários',
  '/dashboard': 'Dashboard',
  '/importacoes': 'Importar recursos',
  '/orcamento': 'Seu orçamento',
  '/orcamentos': 'Orçamentos',
  '/orcamentos/novo': 'Novo orçamento',
  '/ordens': 'Ordens de serviço',
  '/portal': 'Minhas demandas',
  '/portal/certificacoes': 'Minhas certificações',
  '/portal/perfil': 'Meu perfil',
  '/recursos': 'Recursos',
  '/solicitacao': 'Solicitar orçamento',
  '/solicitacao-publica': 'Solicitar orçamento',
  '/solicitacoes': 'Solicitações',
  '/solicitacoes/nova': 'Nova solicitação',
  '/solicitar': 'Solicitar orçamento',
};

function tituloDaPagina(pathname: string): string {
  const rota = pathname.replace(/\/+$/, '') || '/';
  if (rota === '/') return 'Stentio';

  const titulo =
    titulosPorRota[rota] ??
    (/^\/orcamentos\/[^/]+$/.test(rota) ? 'Detalhes do orçamento' : undefined) ??
    (/^\/ordens\/[^/]+\/verificacao$/.test(rota) ? 'Verificação da ordem de serviço' : undefined) ??
    (/^\/ordens\/[^/]+\/workflow$/.test(rota) ? 'Fluxo da ordem de serviço' : undefined) ??
    (/^\/ordens\/[^/]+$/.test(rota) ? 'Detalhes da ordem de serviço' : undefined);

  return `${titulo ?? 'Página não encontrada'} | Stentio`;
}

const queryClient = new QueryClient({
  defaultOptions: { queries: { refetchOnWindowFocus: false } },
});

export default function RootLayout() {
  const pathname = usePathname();

  return (
    <SessionProvider>
      <QueryClientProvider client={queryClient}>
        <Head>
          <title>{tituloDaPagina(pathname)}</title>
          <link rel="icon" type="image/svg+xml" href="/favicon.svg" />
        </Head>
        <Stack screenOptions={{ headerShown: false }} />
      </QueryClientProvider>
    </SessionProvider>
  );
}
