package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * US-015: o que trafega na email.entrega-final.queue.
 *
 * <p>O caminho do anexo vai aqui e em nenhum outro lugar — o email-service lê o arquivo do
 * volume compartilhado montado somente leitura, como já faz com o anexo do orçamento.
 *
 * <p>schema_versao existe para o consumidor poder recusar com clareza uma mensagem de um
 * formato que ele não conhece, em vez de falhar na desserialização.
 */
public record EntregaFinalEmailMensagem(
        @JsonProperty("schema_versao") int schemaVersao,
        @JsonProperty("idempotency_key") String idempotencyKey,
        String to,
        String subject,
        Cliente cliente,
        @JsonProperty("ordem_servico") OrdemServico ordemServico,
        Anexo anexo,
        String mensagem
) {
    public static final int SCHEMA_ATUAL = 1;

    public record Cliente(
            @JsonProperty("nome_representante") String nomeRepresentante,
            @JsonProperty("nome_empresa") String nomeEmpresa) {
    }

    public record OrdemServico(String id, String codigo) {
    }

    public record Anexo(
            @JsonProperty("etapa_id") String etapaId,
            @JsonProperty("nome_arquivo") String nomeArquivo,
            String caminho,
            @JsonProperty("tipo_conteudo") String tipoConteudo,
            @JsonProperty("tamanho_bytes") long tamanhoBytes) {
    }
}
