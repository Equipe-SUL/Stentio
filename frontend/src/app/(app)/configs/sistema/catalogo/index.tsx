    import { useCallback, useState } from "react";
    import { ScrollView, View, Text } from "react-native";
    import { CrudSection } from "../../../../../components/usuarios/CrudSection";
    import { useBuscaDebounce } from "../../../../../components/usuarios/useBuscaDebounce";
    import { coreApi } from "../../../../../lib/api";
    import type { FieldDef, FiltroLista, FiltroStatus } from "../../../../../components/usuarios/types";
    
    // --- Tipos ------------------------------------------------------------
    
    function ativoDeFiltro(status: FiltroStatus): boolean | undefined {
      if (status === "ativos") return true;
      if (status === "inativos") return false;
      return undefined;
    }
    
    function montarQuery(filtro: FiltroLista): string {
      const params = new URLSearchParams();
      if (filtro.nome?.trim()) params.set("nome", filtro.nome.trim());
      if (filtro.ativo !== undefined) params.set("ativo", String(filtro.ativo));
      // Busca única: sem paginação para listar o catálogo inteiro em uma chamada.
      params.set("size", "100");
      return params.toString();
    }
    
    interface Categoria {
      id: string;
      nome: string;
    }
    
    interface Servico {
      id: string;
      nome: string;
      ativo: boolean;
    }
    
    interface Idioma {
      id: string;
      nome: string;
      codigoIso: string;
      ativo: boolean;
    }
    
    // --- Categorias -------------------------------------------------------
    
    const camposCategoria: FieldDef<Categoria>[] = [{ key: "nome", label: "Nome", type: "text" }];
    
    async function listarCategorias(filtro: FiltroLista = {}): Promise<Categoria[]> {
      const { data } = await coreApi.get<{ content: Categoria[] }>(
        `/api/v1/categorias-projeto?${montarQuery(filtro)}`,
      );
      return data.content ?? data;
    }
    
async function criarCategoria(valores: Omit<Categoria, "id">): Promise<Categoria> {
      const { data } = await coreApi.post<Categoria>("/api/v1/categorias-projeto", { nome: valores.nome });
      return data;
    }
    
    async function atualizarCategoria(id: string, valores: Omit<Categoria, "id">) {
      await coreApi.put(`/api/v1/categorias-projeto/${id}`, { nome: valores.nome });
    }
    
    async function excluirCategoria(id: string) {
      await coreApi.delete(`/api/v1/categorias-projeto/${id}`);
    }
    
    // --- Serviços ---------------------------------------------------------
    
    const camposServico: FieldDef<Servico>[] = [
      { key: "nome", label: "Nome", type: "text" },
      { key: "ativo", label: "Status", type: "boolean" },
    ];
    
    async function listarServicos(filtro: FiltroLista = {}): Promise<Servico[]> {
      const { data } = await coreApi.get<{ content: Servico[] }>(
        `/api/v1/tipos-servico?${montarQuery(filtro)}`,
      );
      return data.content ?? data;
    }
    
async function criarServico(valores: Omit<Servico, "id">): Promise<Servico> {
      const { data } = await coreApi.post<Servico>("/api/v1/tipos-servico", { nome: valores.nome });
      return data;
    }

    async function atualizarServico(id: string, valores: Omit<Servico, "id">) {
      await coreApi.put(`/api/v1/tipos-servico/${id}`, { nome: valores.nome });
    }

    async function atualizarStatusServico(id: string, ativo: boolean) {
      await coreApi.patch(`/api/v1/tipos-servico/${id}/status`, { ativo });
    }

    // Soft delete: sem DELETE físico no Core (associação com recursos); apenas desativa.
    async function excluirServico(id: string) {
      await atualizarStatusServico(id, false);
    }
    
    // --- Idiomas ----------------------------------------------------------
    
    const camposIdioma: FieldDef<Idioma>[] = [
      { key: "nome", label: "Nome", type: "text" },
      { key: "codigoIso", label: "Código (ISO)", type: "iso", placeholder: "ex: pt" },
      { key: "ativo", label: "Status", type: "boolean" },
    ];
    
    async function listarIdiomas(filtro: FiltroLista = {}): Promise<Idioma[]> {
      const { data } = await coreApi.get<{ content: Idioma[] }>(
        `/api/v1/idiomas?${montarQuery(filtro)}`,
      );
      return data.content ?? data;
    }
    
async function criarIdioma(valores: Omit<Idioma, "id">): Promise<Idioma> {
      const { data } = await coreApi.post<Idioma>("/api/v1/idiomas", {
        nome: valores.nome,
        codigoIso: valores.codigoIso,
      });
      return data;
    }

    async function atualizarIdioma(id: string, valores: Omit<Idioma, "id">) {
      await coreApi.put(`/api/v1/idiomas/${id}`, {
        nome: valores.nome,
        codigoIso: valores.codigoIso,
      });
    }

    async function atualizarStatusIdioma(id: string, ativo: boolean) {
      await coreApi.patch(`/api/v1/idiomas/${id}/status`, { ativo });
    }

    // Soft delete: sem DELETE físico no Core (associação com recursos); apenas desativa.
    async function excluirIdioma(id: string) {
      await atualizarStatusIdioma(id, false);
    }
    
    // --- Componente -------------------------------------------------------
    
    export default function CatalogoDeReferenciais() {
      const buscaCategorias = useBuscaDebounce();
      const buscaServicos = useBuscaDebounce();
      const buscaIdiomas = useBuscaDebounce();
      const [statusServicos, setStatusServicos] = useState<FiltroStatus>("todos");
      const [statusIdiomas, setStatusIdiomas] = useState<FiltroStatus>("todos");

      const fetchCategorias = useCallback(
        () => listarCategorias({ nome: buscaCategorias.debounced }),
        [buscaCategorias.debounced],
      );
      const fetchServicos = useCallback(
        () => listarServicos({ nome: buscaServicos.debounced, ativo: ativoDeFiltro(statusServicos) }),
        [buscaServicos.debounced, statusServicos],
      );
      const fetchIdiomas = useCallback(
        () => listarIdiomas({ nome: buscaIdiomas.debounced, ativo: ativoDeFiltro(statusIdiomas) }),
        [buscaIdiomas.debounced, statusIdiomas],
      );

      return (
        <ScrollView
          className="flex-1 bg-neutral-50"
          contentContainerClassName="gap-6 p-6 web:px-24 web:py-12 web:items-center"
          keyboardShouldPersistTaps="handled"
        >
          <View className="w-full web:max-w-6xl gap-6">
            <View>
              <Text className="text-2xl font-bold text-neutral-900">Catálogo de Referenciais</Text>
              <Text className="text-sm text-neutral-500">
                Tipos de serviço, categorias de projeto e idiomas que servem de base no sistema
              </Text>
            </View>
    
            <View className="gap-6 web:md:flex-row">
              <CrudSection
                title="Categorias"
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
                  busca: buscaCategorias.valor,
                  onBusca: buscaCategorias.setValor,
                }}
              />
    
              <CrudSection
                title="Serviços"
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
                  busca: buscaServicos.valor,
                  onBusca: buscaServicos.setValor,
                  comStatus: true,
                  status: statusServicos,
                  onStatus: setStatusServicos,
                }}
              />

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
                  busca: buscaIdiomas.valor,
                  onBusca: buscaIdiomas.setValor,
                  comStatus: true,
                  status: statusIdiomas,
                  onStatus: setStatusIdiomas,
                }}
              />
            </View>
          </View>
        </ScrollView>
      );
    }