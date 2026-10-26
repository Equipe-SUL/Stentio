package com.stentio.core.model;

// Dados de um arquivo já gravado no storage, antes de virar entidade.
// Fica no pacote model para que o service de storage não dependa da entidade.
public record ArquivoArmazenado(
        String nomeOriginal,
        String caminho,
        String tipoConteudo,
        long tamanhoBytes
) {
}
