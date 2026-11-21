package com.stentio.emailservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/** US-015: espelho do payload que o Core publica em email.entrega-final.queue. */
public record EntregaFinalEmailMensagem(
        @JsonProperty("schema_versao") Integer schemaVersao,
        @JsonProperty("idempotency_key") String idempotencyKey,
        String to,
        String subject,
        Cliente cliente,
        @JsonProperty("ordem_servico") OrdemServico ordemServico,
        Anexo anexo,
        String mensagem
) {
    public static final int SCHEMA_SUPORTADO = 1;

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
            @JsonProperty("tamanho_bytes") Long tamanhoBytes) {
    }
}
