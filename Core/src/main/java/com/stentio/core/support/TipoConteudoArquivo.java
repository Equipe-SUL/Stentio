package com.stentio.core.support;

import java.util.Locale;
import java.util.Map;

/**
 * US-015: o tipo de conteúdo deduzido da extensão do nome original.
 *
 * <p>A US-013 guarda da entrega só o nome e o caminho, não o tipo declarado no upload — e
 * isso é proposital: o tipo que vem do cliente não é confiável, e o download devolve tudo
 * como octet-stream de propósito. Mas o anexo de e-mail precisa de um tipo para o cliente
 * conseguir abrir o arquivo, então ele é deduzido aqui, da extensão que nós mesmos
 * validamos na hora de gravar.
 *
 * <p>A lista cobre as extensões aceitas em {@code app.arquivos.extensoes-permitidas};
 * qualquer outra cai no tipo genérico, que o cliente de e-mail trata como download.
 */
public final class TipoConteudoArquivo {

    public static final String PADRAO = "application/octet-stream";

    private static final Map<String, String> POR_EXTENSAO = Map.ofEntries(
            Map.entry("pdf", "application/pdf"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("rtf", "application/rtf"),
            Map.entry("txt", "text/plain"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("jpg", "image/jpeg"),
            Map.entry("jpeg", "image/jpeg"),
            Map.entry("png", "image/png"));

    private TipoConteudoArquivo() {
    }

    public static String de(String nomeDoArquivo) {
        if (nomeDoArquivo == null) {
            return PADRAO;
        }
        int ponto = nomeDoArquivo.lastIndexOf('.');
        if (ponto < 0 || ponto == nomeDoArquivo.length() - 1) {
            return PADRAO;
        }
        String extensao = nomeDoArquivo.substring(ponto + 1).toLowerCase(Locale.ROOT);
        return POR_EXTENSAO.getOrDefault(extensao, PADRAO);
    }
}
