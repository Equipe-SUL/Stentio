import { useCallback, useState } from "react";
import { ConsultaCatalogo } from "../../../../../../components/catalogo/ConsultaCatalogo";
import { CrudSection } from "../../../../../../components/usuarios/CrudSection";
import { useBuscaDebounce } from "../../../../../../components/usuarios/useBuscaDebounce";
import type { FiltroStatus } from "../../../../../../components/usuarios/types";
import {
  ativoDeFiltro,
  atualizarServico,
  atualizarStatusServico,
  camposServico,
  criarServico,
  excluirServico,
  listarServicos,
} from "../../../../../../lib/catalogoService";

export default function TiposDeServicoScreen() {
  const busca = useBuscaDebounce();
  const [status, setStatus] = useState<FiltroStatus>("todos");

  const fetchServicos = useCallback(
    () => listarServicos({ nome: busca.debounced, ativo: ativoDeFiltro(status) }),
    [busca.debounced, status],
  );

  return (
    <ConsultaCatalogo
      contexto="Configurações do Sistema"
      secao="Catálogo"
      titulo="Tipos de Serviço"
      descricao="Serviços que podem ser orçados e executados nas ordens de serviço."
    >
      <CrudSection
        title="Tipos de Serviços"
        createTitle="Novo serviço"
        editTitle="Editar serviço"
        fields={camposServico}
        defaultValues={{ nome: "", ativo: true }}
        fetchList={fetchServicos}
        createItem={criarServico}
        updateItem={atualizarServico}
        deleteItem={excluirServico}
        updateStatus={atualizarStatusServico}
        deleteMessage="Este registro será desativado e ficará oculto da listagem. Você pode reativá-lo depois."
        emptyMessage="Nenhum serviço cadastrado"
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
