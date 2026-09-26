import type { AxiosInstance } from 'axios';
import { Platform } from 'react-native';

/**
 * Rastreamento de requisições para desenvolvimento.
 *
 * O Expo Go não tem devtools de rede, então o caminho é o terminal do Metro,
 * via console.log/console.warn. Registra na ida e na volta porque "não logou
 * nada" precisa ser distinguível de "a requisição nem saiu".
 *
 * Tudo atrás de __DEV__, então o Metro remove o bloco do bundle de produção.
 */

function habilitado(): boolean {
  return typeof __DEV__ !== 'undefined' && __DEV__;
}

function resumoCorpo(dados: unknown): string {
  if (dados == null) return '';
  if (typeof dados === 'string') return dados.slice(0, 300);

  try {
    return JSON.stringify(dados).slice(0, 300);
  } catch {
    return '[corpo ilegível]';
  }
}

/**
 * Só a presença do header, nunca o valor: o token não pode aparecer em log.
 * Distingue "autenticou por Bearer" de "autenticou por cookie", que é a
 * diferença entre a prévia da logo funcionar ou não no nativo.
 */
function temAuthorization(config: { headers?: unknown }): string {
  const headers = config.headers as Record<string, unknown> | undefined;
  if (!headers) return 'sem auth';

  const chave = Object.keys(headers).find((k) => k.toLowerCase() === 'authorization');
  if (!chave) return 'sem auth';

  return headers[chave] ? 'bearer' : 'sem auth';
}

function prefixo(rotulo: string): string {
  return `[${rotulo}:${Platform.OS}]`;
}

/**
 * Registra uma linha avulsa, para o que não passa pelo axios.
 *
 * Caso do <Image> do expo-image: ele busca a imagem pelo stack nativo, fora do
 * axios, então nenhum interceptor enxerga. Sem isto o carregamento da logo é um
 * evento invisível no log.
 */
export function registrarLog(rotulo: string, mensagem: string, nivel: 'info' | 'erro' = 'info') {
  if (!habilitado()) return;

  const linha = `${prefixo(rotulo)} ${mensagem}`;

  if (nivel === 'erro') {
    console.warn(linha);
  } else {
    console.log(linha);
  }
}

function registrarResposta(
  rotulo: string,
  metodo: string,
  url: string,
  status: number | undefined,
  dados: unknown,
  extra?: string,
) {
  const sufixo = extra ? ` ${extra}` : '';

  if (status === undefined) {
    registrarLog(rotulo, `${metodo} ${url} → FALHOU${sufixo}`, 'erro');
    return;
  }

  if (status >= 400) {
    registrarLog(rotulo, `${metodo} ${url} → ${status}${sufixo} ${resumoCorpo(dados)}`, 'erro');
    return;
  }

  registrarLog(rotulo, `${metodo} ${url} → ${status}${sufixo}`);
}

/**
 * Instala os interceptores de log num client axios. Chame logo após o create().
 *
 * Não substitui nenhum interceptor existente: o de token continua intacto.
 */
export function instrumentarHttp(client: AxiosInstance, rotulo: string): AxiosInstance {
  if (!habilitado()) return client;

  client.interceptors.request.use((config) => {
    registrarLog(
      rotulo,
      `${config.method?.toUpperCase() ?? 'GET'} ${config.url ?? ''} → ENVIADO (${temAuthorization(config as { headers?: unknown })})`,
    );
    return config;
  });

  client.interceptors.response.use(
    (response) => {
      registrarResposta(
        rotulo,
        response.config.method?.toUpperCase() ?? 'GET',
        response.config.url ?? '',
        response.status,
        response.data,
      );
      return response;
    },
    (error) => {
      registrarResposta(
        rotulo,
        error.config?.method?.toUpperCase() ?? 'GET',
        error.config?.url ?? '',
        error.response?.status,
        error.response?.data,
        // Sem resposta significa servidor inalcançável, não erro devolvido por ele.
        error.response ? '' : `(${error.message})`,
      );
      return Promise.reject(error);
    },
  );

  return client;
}
