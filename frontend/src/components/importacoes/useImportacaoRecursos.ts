import { useCallback, useEffect, useRef, useState } from "react";
import { Platform } from "react-native";
import * as DocumentPicker from "expo-document-picker";
import {
  carregarConfiguracao,
  extrairFalhaImportacao,
  importarRecursos,
  validarRecursos,
  verificarArquivo,
} from "../../lib/importacaoService";
import type {
  ArquivoCsv,
  ConfiguracaoImportacao,
  EtapaImportacao,
  FalhaImportacao,
  PreviaImportacao,
  RelatorioImportacao,
  Separador,
} from "./types";

export function useImportacaoRecursos() {
  const [configuracao, setConfiguracao] =
    useState<ConfiguracaoImportacao | null>(null);
  const [erroConfiguracao, setErroConfiguracao] = useState("");
  const [arquivo, setArquivo] = useState<ArquivoCsv | null>(null);
  const [separador, setSeparador] = useState<Separador>("auto");
  const [etapa, setEtapa] = useState<EtapaImportacao>("arquivo");
  const [previa, setPrevia] = useState<PreviaImportacao | null>(null);
  const [relatorio, setRelatorio] = useState<RelatorioImportacao | null>(null);
  const [erro, setErro] = useState<FalhaImportacao | null>(null);
  const [progresso, setProgresso] = useState(0);
  const [selecionando, setSelecionando] = useState(false);
  const ocupado = useRef(false);
  const tentativaRealizada = useRef(false);

  const recarregarConfiguracao = useCallback(async () => {
    setErroConfiguracao("");
    try {
      setConfiguracao(await carregarConfiguracao());
    } catch (error) {
      setErroConfiguracao(extrairFalhaImportacao(error).mensagem);
    }
  }, []);

  useEffect(() => {
    let ativo = true;
    carregarConfiguracao()
      .then((config) => {
        if (ativo) setConfiguracao(config);
      })
      .catch((error) => {
        if (ativo) setErroConfiguracao(extrairFalhaImportacao(error).mensagem);
      });
    return () => {
      ativo = false;
    };
  }, []);

  useEffect(
    () => () => {
      if (Platform.OS === "web" && arquivo?.uri.startsWith("blob:"))
        URL.revokeObjectURL(arquivo.uri);
    },
    [arquivo],
  );

  useEffect(() => {
    if (Platform.OS !== "web" || etapa !== "importando") return;
    const avisar = (event: BeforeUnloadEvent) => {
      event.preventDefault();
      event.returnValue = "";
    };
    window.addEventListener("beforeunload", avisar);
    return () => window.removeEventListener("beforeunload", avisar);
  }, [etapa]);

  function limparPrevia() {
    setPrevia(null);
    setErro(null);
    setEtapa("arquivo");
  }

  async function selecionarArquivo() {
    if (ocupado.current || !configuracao || tentativaRealizada.current) return;
    ocupado.current = true;
    setSelecionando(true);
    try {
      const result = await DocumentPicker.getDocumentAsync({
        type: "*/*",
        multiple: false,
        copyToCacheDirectory: true,
        base64: false,
      });
      if (result.canceled) return;
      const selecionado = result.assets[0];
      const aviso = verificarArquivo(
        selecionado,
        configuracao.max_upload_bytes,
      );
      // Uma seleção inválida também invalida a prévia anterior.
      limparPrevia();
      setArquivo(aviso ? null : selecionado);
      if (aviso) {
        if (Platform.OS === "web" && selecionado.uri.startsWith("blob:"))
          URL.revokeObjectURL(selecionado.uri);
        setErro({ mensagem: aviso });
      }
    } catch {
      setErro({
        mensagem:
          "Não foi possível abrir o seletor de arquivos. Tente novamente.",
      });
    } finally {
      ocupado.current = false;
      setSelecionando(false);
    }
  }

  function alterarSeparador(valor: Separador) {
    if (ocupado.current || tentativaRealizada.current) return;
    setSeparador(valor);
    limparPrevia();
  }

  function removerArquivo() {
    if (ocupado.current || tentativaRealizada.current) return;
    setArquivo(null);
    limparPrevia();
  }

  async function validar() {
    if (
      !arquivo ||
      !configuracao ||
      ocupado.current ||
      tentativaRealizada.current
    )
      return;
    ocupado.current = true;
    setEtapa("validando");
    setErro(null);
    setPrevia(null);
    setProgresso(0);
    try {
      setPrevia(await validarRecursos(arquivo, separador, setProgresso));
      setEtapa("revisao");
    } catch (error) {
      setErro(extrairFalhaImportacao(error));
      setEtapa("arquivo");
    } finally {
      ocupado.current = false;
    }
  }

  async function importar() {
    if (!arquivo || !previa || ocupado.current || tentativaRealizada.current)
      return;
    ocupado.current = true;
    tentativaRealizada.current = true;
    setEtapa("importando");
    setErro(null);
    try {
      setRelatorio(await importarRecursos(arquivo, separador));
    } catch (error) {
      const falha = extrairFalhaImportacao(error, true);
      setErro(falha);
      setRelatorio(falha.relatorio ?? null);
    } finally {
      setEtapa("resultado");
      ocupado.current = false;
    }
  }

  function novaImportacao() {
    if (ocupado.current) return;
    tentativaRealizada.current = false;
    setArquivo(null);
    setSeparador("auto");
    setRelatorio(null);
    limparPrevia();
  }

  return {
    configuracao,
    erroConfiguracao,
    recarregarConfiguracao,
    arquivo,
    separador,
    etapa,
    previa,
    relatorio,
    erro,
    progresso,
    selecionando,
    selecionarArquivo,
    alterarSeparador,
    removerArquivo,
    validar,
    importar,
    novaImportacao,
    bloqueado:
      selecionando ||
      etapa === "validando" ||
      etapa === "importando" ||
      etapa === "resultado",
  };
}
