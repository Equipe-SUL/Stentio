import { useCallback, useState } from "react";
import { ConsultaCatalogo } from "../../../../../../components/catalogo/ConsultaCatalogo";
import { CrudSection } from "../../../../../../components/usuarios/CrudSection";
import { useBuscaDebounce } from "../../../../../../components/usuarios/useBuscaDebounce";
import type { FiltroStatus } from "../../../../../../components/usuarios/types";
import {
  ativoDeFiltro,
  atualizarIdioma,
  atualizarStatusIdioma,
  camposIdioma,
  criarIdioma,
  excluirIdioma,
  listarIdiomas,
} from "../../../../../../lib/catalogoService";

export default function IdiomasScreen() {
  const busca = useBuscaDebounce();
  const [status, setStatus] = useState<FiltroStatus>("todos");

  const fetchIdiomas = useCallback(
    () => listarIdiomas({ nome: busca.debounced, ativo: ativoDeFiltro(status) }),
    [busca.debounced, status],
  );

  return (
    <ConsultaCatalogo
      contexto="Configurações do Sistema"
      secao="Catálogo"
      titulo="Idiomas"
      descricao="Idiomas disponíveis para tradução e precificação por par."
    >
      <CrudSection
        title="Idiomas"
        createTitle="Novo idioma"
        editTitle="Editar idioma"
        fields={camposIdioma}
        defaultValues={{ nome: "", codigoIso: "", ativo: true }}
        fetchList={fetchIdiomas}
        createItem={criarIdioma}
        updateItem={atualizarIdioma}
        deleteItem={excluirIdioma}
        updateStatus={atualizarStatusIdioma}
        deleteMessage="Este registro será desativado e ficará oculto da listagem. Você pode reativá-lo depois."
        emptyMessage="Nenhum idioma cadastrado"
        filtros={{
          busca: busca.valor,
          onBusca: busca.setValor,
          comStatus: true,
          status,
          onStatus: setStatus,
        }}
      />
    </ConsultaCatalogo>
  );
}
