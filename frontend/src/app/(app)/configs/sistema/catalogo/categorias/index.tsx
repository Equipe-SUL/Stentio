import { useCallback } from "react";
import { ConsultaCatalogo } from "../../../../../../components/catalogo/ConsultaCatalogo";
import { CrudSection } from "../../../../../../components/usuarios/CrudSection";
import { useBuscaDebounce } from "../../../../../../components/usuarios/useBuscaDebounce";
import {
  atualizarCategoria,
  camposCategoria,
  criarCategoria,
  excluirCategoria,
  listarCategorias,
} from "../../../../../../lib/catalogoService";

export default function CategoriasDeProjetoScreen() {
  const busca = useBuscaDebounce();

  const fetchCategorias = useCallback(
    () => listarCategorias({ nome: busca.debounced }),
    [busca.debounced],
  );

  return (
    <ConsultaCatalogo
      contexto="Configurações do Sistema"
      secao="Catálogo"
      titulo="Categorias de Projeto"
      descricao="Agrupamentos usados para classificar e filtrar projetos."
    >
      <CrudSection
        title="Categorias de Projetos"
        createTitle="Nova categoria"
        editTitle="Editar categoria"
        fields={camposCategoria}
        defaultValues={{ nome: "" }}
        fetchList={fetchCategorias}
        createItem={criarCategoria}
        updateItem={atualizarCategoria}
        deleteItem={excluirCategoria}
        emptyMessage="Nenhuma categoria cadastrada"
        filtros={{
          busca: busca.valor,
          onBusca: busca.setValor,
        }}
      />
    </ConsultaCatalogo>
  );
}
