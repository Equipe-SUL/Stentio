    import { ScrollView, View, Text } from "react-native";
    import { CrudSection } from "../../../components/usuarios/CrudSection";
    import { coreApi } from "../../../lib/api";
    import type { FieldDef } from "../../../components/usuarios/types";
    
    // --- Tipos ------------------------------------------------------------
    
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
    
    async function listarCategorias(): Promise<Categoria[]> {
      const { data } = await coreApi.get<{ content: Categoria[] }>("/api/v1/categorias-projeto");
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
    
    async function listarServicos(): Promise<Servico[]> {
      const { data } = await coreApi.get<{ content: Servico[] }>("/api/v1/tipos-servico");
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
    
    async function listarIdiomas(): Promise<Idioma[]> {
      const { data } = await coreApi.get<{ content: Idioma[] }>("/api/v1/idiomas");
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
    
    export default function TiposDeCadastro() {
      return (
        <ScrollView
          className="flex-1 bg-neutral-50"
          contentContainerClassName="gap-6 p-6 web:px-24 web:py-12 web:items-center"
          keyboardShouldPersistTaps="handled"
        >
          <View className="w-full web:max-w-6xl gap-6">
            <View>
              <Text className="text-2xl font-bold text-neutral-900">Cadastros base</Text>
              <Text className="text-sm text-neutral-500">
                Categorias, serviços e idiomas utilizados no sistema
              </Text>
            </View>
    
            <View className="gap-6 web:md:flex-row">
              <CrudSection
                title="Categorias"
                createTitle="Nova categoria"
                editTitle="Editar categoria"
                fields={camposCategoria}
                defaultValues={{ nome: "" }}
                fetchList={listarCategorias}
                createItem={criarCategoria}
                updateItem={atualizarCategoria}
                deleteItem={excluirCategoria}
                emptyMessage="Nenhuma categoria cadastrada"
              />
    
              <CrudSection
                title="Serviços"
                createTitle="Novo serviço"
                editTitle="Editar serviço"
                fields={camposServico}
                defaultValues={{ nome: "", ativo: true }}
                fetchList={listarServicos}
                createItem={criarServico}
                updateItem={atualizarServico}
                deleteItem={excluirServico}
                updateStatus={atualizarStatusServico}
                deleteMessage="Este registro será desativado e ficará oculto da listagem. Você pode reativá-lo depois."
                emptyMessage="Nenhum serviço cadastrado"
              />

              <CrudSection
                title="Idiomas"
                createTitle="Novo idioma"
                editTitle="Editar idioma"
                fields={camposIdioma}
                defaultValues={{ nome: "", codigoIso: "", ativo: true }}
                fetchList={listarIdiomas}
                createItem={criarIdioma}
                updateItem={atualizarIdioma}
                deleteItem={excluirIdioma}
                updateStatus={atualizarStatusIdioma}
                deleteMessage="Este registro será desativado e ficará oculto da listagem. Você pode reativá-lo depois."
                emptyMessage="Nenhum idioma cadastrado"
              />
            </View>
          </View>
        </ScrollView>
      );
    }