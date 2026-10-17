// useOpcoesSolicitacao.ts
import { useCallback, useEffect, useState } from "react";
import { getApiErrorMessage } from "../../lib/api";
import { carregarOpcoesSolicitacao } from "./solicitacoesApi";
import type { OpcoesSolicitacao } from "./types";

const OPCOES_VAZIAS: OpcoesSolicitacao = { clientes: [], categorias: [], idiomas: [] };

// Mesmo padrão do CrudSection (estado manual, sem react-query), então não depende
// de um QueryClientProvider existir na raiz do app.
export function useOpcoesSolicitacao() {
  const [opcoes, setOpcoes] = useState<OpcoesSolicitacao>(OPCOES_VAZIAS);
  const [carregando, setCarregando] = useState(true);
  const [erro, setErro] = useState("");
  const [tentativa, setTentativa] = useState(0);

  useEffect(() => {
    let ativo = true;
    setCarregando(true);
    setErro("");

    carregarOpcoesSolicitacao()
      .then((resultado) => {
        if (ativo) setOpcoes(resultado);
      })
      .catch((error) => {
        if (ativo) setErro(getApiErrorMessage(error));
      })
      .finally(() => {
        if (ativo) setCarregando(false);
      });

    return () => {
      ativo = false;
    };
  }, [tentativa]);

  const recarregar = useCallback(() => setTentativa((atual) => atual + 1), []);

  return { opcoes, carregando, erro, recarregar };
}