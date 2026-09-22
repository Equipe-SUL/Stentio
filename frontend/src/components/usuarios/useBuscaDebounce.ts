import { useEffect, useState } from "react";

// Mantém o valor imediato do campo e expõe um valor "estabilizado" (debounce)
// usado para montar o fetchList da listagem (busca server-side).
export function useBuscaDebounce(atraso = 400) {
  const [valor, setValor] = useState("");
  const [debounced, setDebounced] = useState("");

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(valor), atraso);
    return () => clearTimeout(timer);
  }, [valor, atraso]);

  return { valor, debounced, setValor };
}