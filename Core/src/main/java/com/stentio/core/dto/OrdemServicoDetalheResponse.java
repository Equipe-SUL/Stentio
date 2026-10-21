package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusOrdemServico;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// CA.3: dados completos da OS, com os vínculos de cliente e solicitação.
public record OrdemServicoDetalheResponse(
        UUID id,
        StatusOrdemServico status,
        @JsonProperty("orcamento_id") UUID orcamentoId,
        @JsonProperty("data_criacao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataCriacao,
        Cliente cliente,
        Solicitacao solicitacao
) {

    public record Cliente(UUID id, String nome) {}

    public record Solicitacao(
            UUID id,
            String descricao,
            String categoria,
            @JsonProperty("idioma_origem") String idiomaOrigem,
            @JsonProperty("idioma_destino") String idiomaDestino,
            List<Arquivo> arquivos
    ) {}

    // "url" aponta para o download autenticado da US-09, nunca para o caminho em disco.
    public record Arquivo(UUID id, String nome, String url) {}
}
