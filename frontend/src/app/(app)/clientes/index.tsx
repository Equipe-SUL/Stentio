import { useCallback } from "react";
import { ConsultaCatalogo } from "../../../components/catalogo/ConsultaCatalogo";
import { CrudSection } from "../../../components/usuarios/CrudSection";
import { useBuscaDebounce } from "../../../components/usuarios/useBuscaDebounce";
import {
  atualizarCliente,
  camposCliente,
  criarCliente,
  excluirCliente,
  listarClientes,
} from "../../../lib/clienteService";

export default function ClientesScreen() {
  const busca = useBuscaDebounce();

  const fetchClientes = useCallback(
    () => listarClientes(busca.debounced),
    [busca.debounced],
  );

  return (
    <ConsultaCatalogo
      contexto="Operacional"
      secao="Clientes"
      titulo="Clientes"
      descricao="Empresas e pessoas que solicitam traduções."
      rotaFallback="/admin"
    >
      <CrudSection
        title="Clientes"
        createTitle="Novo cliente"
        editTitle="Editar cliente"
        fields={camposCliente}
        defaultValues={{
          nomeEmpresa: "",
          nomeRepresentante: "",
          emailRepresentante: "",
          telefone: "",
          cpfCnpj: "",
        }}
        fetchList={fetchClientes}
        createItem={criarCliente}
        updateItem={atualizarCliente}
        deleteItem={excluirCliente}
        deleteMessage="Este cliente será excluído permanentemente. A ação não pode ser desfeita."
        emptyMessage="Nenhum cliente cadastrado"
        filtros={{ busca: busca.valor, onBusca: busca.setValor }}
      />
    </ConsultaCatalogo>
  );
}
