// types.ts
export interface OpcaoSelect {
  value: string;
  label: string;
}

// Formato compatível com ClienteResponse do Core.
// O Core devolve `nomeRepresentante` e `emailRepresentante`.
export interface ClienteOpcao {
  id: string;
  nomeEmpresa: string;
  nomeRepresentante?: string;
  emailRepresentante?: string | null;
  email?: string | null;
  telefone?: string | null;
  cpfCnpj?: string | null;
  ativo?: boolean;
}

export interface CategoriaProjetoOpcao {
  id: string;
  nome: string;
  ativo?: boolean;
}

export interface IdiomaOpcao {
  id: string;
  nome: string;
  codigoIso: string;
  ativo?: boolean;
}

export interface OpcoesSolicitacao {
  clientes: ClienteOpcao[];
  categorias: CategoriaProjetoOpcao[];
  idiomas: IdiomaOpcao[];
}

// Arquivo escolhido no File Picker, ainda não enviado.
// `file` só existe na web (objeto File nativo do navegador).
export interface ArquivoSelecionado {
  id: string;
  nome: string;
  uri: string;
  tipo: string;
  tamanho?: number;
  file?: File;
}

// Formato dos erros de validação do backend (já compatível com o setError do RHF).
export interface ErroCampoApi {
  campo: string;
  mensagem: string;
}

// Espelha o SolicitacaoResponse do Core (Core\src\main\java\com\stentio\core\dto\SolicitacaoResponse.java)
export interface SolicitacaoResponse {
  id: string;
  cliente: { id: string; nomeEmpresa: string };
  categoriaProjeto: { id: string; nome: string };
  idiomaOrigem: { id: string; nome: string; codigoIso: string };
  idiomaDestino: { id: string; nome: string; codigoIso: string };
  descricao?: string;
  status: string;
  dataRecebimento: string;
  arquivos?: {
    id: string;
    nome: string;
    url: string;
    tipoConteudo: string;
    tamanhoBytes: number;
  }[];
}