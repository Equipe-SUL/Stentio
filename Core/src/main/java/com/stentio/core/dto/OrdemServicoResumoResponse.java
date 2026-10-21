package com.stentio.core.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.stentio.core.model.StatusOrdemServico;

import java.time.Instant;
import java.util.UUID;

// Linha da listagem. Segue o estilo do módulo de orçamento (snake_case), de onde a OS nasce.
public record OrdemServicoResumoResponse(
        UUID id,
        StatusOrdemServico status,
        @JsonProperty("cliente_id") UUID clienteId,
        @JsonProperty("cliente_nome") String clienteNome,
        @JsonProperty("data_criacao") @JsonFormat(shape = JsonFormat.Shape.STRING) Instant dataCriacao
) {
}
